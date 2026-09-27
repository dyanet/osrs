/*
 * Copyright 2012-2026, Dyanet Inc., Akber A. Choudhry,
 *   and other individual contributors identified by the
 *   @authors tag in each source artefact.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   You may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *   either express or implied.
 *   See the License for the specific language governing permissions
 *   and limitations under the License.
 */

package com.dyanet.osrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.dyanet.osrs.transport.StubTransport;
import com.dyanet.osrs.xcp.OsrsSignature;
import com.dyanet.osrs.xcp.XcpCodec;
import com.dyanet.osrs.xcp.XcpData;
import com.sun.net.httpserver.HttpServer;

/**
 * Drives the real client and the default JDK transport against a local HTTP server that checks
 * the signature the way OpenSRS does.
 */
class HttpLoopbackTest {

    private static final String KEY = "loopback-key";

    private HttpServer server;
    private final AtomicReference<String> contentLength = new AtomicReference<>();

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> {
            byte[] body = ex.getRequestBody().readAllBytes();
            String xml = new String(body, StandardCharsets.UTF_8);
            contentLength.set(ex.getRequestHeaders().getFirst("Content-Length"));
            String reply;
            boolean signed = OsrsSignature.sign(xml, KEY).equals(ex.getRequestHeaders().getFirst("X-Signature"))
                && "tester".equals(ex.getRequestHeaders().getFirst("X-Username"))
                && "POST".equals(ex.getRequestMethod());
            if (!signed) {
                reply = StubTransport.reply(false, 410, "Reseller authentication error", null);
            } else {
                XcpData req = XcpData.of(XcpCodec.decode(xml));
                String domain = req.getString("attributes", "domain").orElse("");
                reply = domain.startsWith("free")
                    ? StubTransport.reply(true, 210, "Domain available", Map.of("status", "available"))
                    : StubTransport.reply(true, 211, "Domain taken", Map.of("status", "taken"));
            }
            byte[] out = reply.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, out.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(out);
            }
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private OsrsClient client(String key) {
        return OsrsClient.builder().config(OsrsConfig.builder()
            .endpoint(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/"))
            .username("tester").apiKey(key).build()).build();
    }

    @Test
    void realRoundTripWithSignatureCheck() {
        try (OsrsClient c = client(KEY)) {
            assertTrue(c.lookup("free-ünïcode.com").available());
            assertEquals(false, c.lookup("taken.com").available());
            assertTrue(Integer.parseInt(contentLength.get()) > 0);
        }
    }

    @Test
    void wrongKeyIsAnAuthenticationError() {
        try (OsrsClient c = client("wrong")) {
            assertThrows(OsrsAuthenticationException.class, () -> c.lookup("free.com"));
        }
    }

    @Test
    void connectionRefusedIsATransportError() throws IOException {
        int port;
        try (ServerSocket s = new ServerSocket(0)) {
            port = s.getLocalPort();
        }
        OsrsClient c = OsrsClient.builder().config(OsrsConfig.builder()
            .endpoint(URI.create("http://127.0.0.1:" + port + "/")).username("u").apiKey("k")
            .connectTimeout(Duration.ofSeconds(2)).build()).build();
        OsrsTransportException e = assertThrows(OsrsTransportException.class, () -> c.lookup("a.com"));
        assertEquals(-1, e.getHttpStatus());
        c.close();
    }
}
