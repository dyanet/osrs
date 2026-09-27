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

package com.dyanet.osrs.transport;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.dyanet.osrs.xcp.XcpCodec;
import com.dyanet.osrs.xcp.XcpData;

/**
 * An in-memory {@link Transport} for tests: it records every request and answers from a script,
 * so code built on the client can be tested without a network or an OpenSRS account.
 *
 * <pre>{@code
 * StubTransport stub = StubTransport.replying(
 *     StubTransport.reply(true, 210, "Domain available", Map.of("status", "available")));
 * OsrsClient client = OsrsClient.builder().config(config).transport(stub).build();
 * }</pre>
 */
public final class StubTransport implements Transport {

    /**
     * A request as the stub received it.
     *
     * @param uri     the endpoint
     * @param headers the headers, including {@code X-Username} and {@code X-Signature}
     * @param body    the XML body
     */
    public record Captured(URI uri, Map<String, String> headers, String body) {

        /**
         * @return the decoded request envelope (object, action, attributes...)
         */
        public XcpData decoded() {
            return XcpData.of(XcpCodec.decode(body));
        }
    }

    private final Function<Captured, Reply> responder;
    private final List<Captured> requests = Collections.synchronizedList(new ArrayList<>());

    private StubTransport(Function<Captured, Reply> responder) {
        this.responder = responder;
    }

    /**
     * @param responder computes the reply for each request; it may throw an
     *                  {@link java.io.UncheckedIOException} to simulate a network failure
     * @return the stub
     */
    public static StubTransport responding(Function<Captured, Reply> responder) {
        return new StubTransport(responder);
    }

    /**
     * @param xmlBodies replies with HTTP 200, one per request, in order
     * @return the stub; it fails the request once the script runs out
     */
    public static StubTransport replying(String... xmlBodies) {
        Deque<String> q = new ArrayDeque<>(List.of(xmlBodies));
        return new StubTransport(c -> {
            String next;
            synchronized (q) {
                next = q.poll();
            }
            if (next == null) {
                throw new IllegalStateException("StubTransport has no reply left for " + c.body());
            }
            return new Reply(200, next);
        });
    }

    /**
     * Builds a reply body in the shape OpenSRS sends.
     *
     * @param success      the {@code is_success} flag
     * @param code         the {@code response_code}
     * @param text         the {@code response_text}
     * @param attributes   the {@code attributes} {@code dt_assoc}, or {@code null} for none
     * @return a complete {@code OPS_envelope}
     */
    public static String reply(boolean success, int code, String text, Map<String, ?> attributes) {
        Map<String, Object> top = new LinkedHashMap<>();
        top.put("protocol", XcpCodec.PROTOCOL);
        top.put("action", "REPLY");
        top.put("object", "DOMAIN");
        top.put("is_success", success);
        top.put("response_code", code);
        top.put("response_text", text);
        if (attributes != null) {
            top.put("attributes", attributes);
        }
        return XcpCodec.encodeEnvelope(top);
    }

    @Override
    public Reply post(URI uri, Map<String, String> headers, byte[] body, Duration timeout)
            throws IOException {
        Captured c = new Captured(uri, Map.copyOf(headers), new String(body, StandardCharsets.UTF_8));
        requests.add(c);
        try {
            return responder.apply(c);
        } catch (java.io.UncheckedIOException e) {
            throw e.getCause();
        }
    }

    /**
     * @return every request received so far, oldest first
     */
    public List<Captured> requests() {
        synchronized (requests) {
            return List.copyOf(requests);
        }
    }

    /**
     * @return the most recent request
     * @throws IllegalStateException if none was received
     */
    public Captured lastRequest() {
        synchronized (requests) {
            if (requests.isEmpty()) {
                throw new IllegalStateException("No request received");
            }
            return requests.get(requests.size() - 1);
        }
    }
}
