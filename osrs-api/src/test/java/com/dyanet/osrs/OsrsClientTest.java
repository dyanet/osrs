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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.dyanet.osrs.transport.StubTransport;
import com.dyanet.osrs.transport.Transport;
import com.dyanet.osrs.xcp.OsrsSignature;
import com.dyanet.osrs.xcp.XcpData;
import com.dyanet.osrs.xcp.XcpRequest;
import com.dyanet.osrs.xcp.XcpResponse;

class OsrsClientTest {

    static final OsrsConfig CONFIG = OsrsConfig.test("reseller", "the-private-key");

    private final List<Duration> slept = new ArrayList<>();

    private OsrsClient client(Transport t, RetryPolicy p) {
        return OsrsClient.builder().config(CONFIG).transport(t).retryPolicy(p)
            .sleeper(slept::add).build();
    }

    private static String ok(int code, String text, Map<String, ?> attrs) {
        return StubTransport.reply(true, code, text, attrs);
    }

    private static String fail(int code, String text) {
        return StubTransport.reply(false, code, text, null);
    }

    @Test
    void signsAndIdentifiesEveryRequest() {
        StubTransport stub = StubTransport.replying(ok(200, "Command successful", Map.of("balance", "1.00")));
        XcpResponse r = client(stub, RetryPolicy.none())
            .execute(XcpRequest.builder("DOMAIN", "GET_BALANCE").build());
        assertEquals("1.00", r.getAttributes().getString("balance").orElseThrow());

        StubTransport.Captured c = stub.lastRequest();
        assertEquals(CONFIG.getEndpoint(), c.uri());
        assertEquals("text/xml", c.headers().get("Content-Type"));
        assertEquals("reseller", c.headers().get("X-Username"));
        assertEquals(OsrsSignature.sign(c.body(), "the-private-key"), c.headers().get("X-Signature"));
        assertFalse(c.body().contains("the-private-key"));
        XcpData sent = c.decoded();
        assertEquals("GET_BALANCE", sent.getString("action").orElseThrow());
        assertEquals("DOMAIN", sent.getString("object").orElseThrow());
        assertSame(stub, stub); // keep the stub referenced
        assertEquals(1, stub.requests().size());
    }

    @Test
    void sendReturnsFailedRepliesAndExecuteThrows() {
        StubTransport stub = StubTransport.replying(fail(465, "Invalid domain"), fail(465, "Invalid domain"));
        OsrsClient client = client(stub, RetryPolicy.none());
        XcpRequest req = XcpRequest.builder("DOMAIN", "MODIFY").attribute("domain", "x").build();

        XcpResponse r = client.send(req);
        assertFalse(r.isSuccess());
        assertEquals(465, r.getResponseCode());
        assertSame(req, r.getRequest());

        OsrsApiException e = assertThrows(OsrsApiException.class, () -> client.execute(req));
        assertEquals(465, e.getResponseCode());
        assertEquals("Invalid domain", e.getResponseText());
        assertEquals("OpenSRS DOMAIN MODIFY failed: 465 Invalid domain", e.getMessage());
        assertEquals(465, e.getResponse().getResponseCode());
    }

    @Test
    void authenticationFailuresHaveTheirOwnType() {
        StubTransport stub = StubTransport.replying(fail(410, "Reseller authentication error"),
            fail(401, "Authentication Error"),
            fail(555, "Connection refused: invalid ip address"),
            fail(400, "Access denied: invalid IP address"),
            fail(400, "Internal server error"),
            fail(555, "Domain has already been successfully renewed"));
        OsrsClient client = client(stub, RetryPolicy.none());
        XcpRequest req = XcpRequest.builder("DOMAIN", "GET_BALANCE").build();
        for (int code : new int[] {410, 401, 555, 400}) {
            assertEquals(code, assertThrows(OsrsAuthenticationException.class, () -> client.execute(req))
                .getResponseCode());
        }
        OsrsApiException plain = assertThrows(OsrsApiException.class, () -> client.execute(req));
        assertFalse(plain instanceof OsrsAuthenticationException);
        OsrsApiException renewed = assertThrows(OsrsApiException.class, () -> client.execute(req));
        assertFalse(renewed instanceof OsrsAuthenticationException, "555 renewal is not an auth error");
    }

    @Test
    void tryAgainLaterFailuresHaveTheirOwnType() {
        StubTransport stub = StubTransport.replying(fail(720, "Supplier Unavailable"),
            fail(310, "Exceeded max simultaneous connections"));
        OsrsClient client = client(stub, RetryPolicy.none());
        XcpRequest req = XcpRequest.builder("DOMAIN", "GET").build();
        assertEquals(720, assertThrows(OsrsUnavailableException.class, () -> client.execute(req)).getResponseCode());
        assertEquals(310, assertThrows(OsrsUnavailableException.class, () -> client.execute(req)).getResponseCode());
    }

    static class DomainGone extends OsrsApiException {
        private static final long serialVersionUID = 1L;

        DomainGone(XcpResponse r) {
            super(r);
        }
    }

    @Test
    void perCallMappersComeBeforeClientMappersBeforeBuiltIns() {
        StubTransport stub = StubTransport.replying(fail(410, "x"), fail(480, "gone"), fail(480, "gone"));
        OsrsErrorMapper clientWide = r -> r.getResponseCode() == 480 ? new DomainGone(r) : null;
        OsrsClient client = OsrsClient.builder().config(CONFIG).transport(stub).errorMapper(clientWide).build();
        XcpRequest req = XcpRequest.builder("DOMAIN", "GET").build();

        OsrsErrorMapper perCall = r -> r.getResponseCode() == 410 ? new DomainGone(r) : null;
        assertInstanceOf(DomainGone.class, assertThrows(OsrsApiException.class, () -> client.execute(req, perCall)));
        assertInstanceOf(DomainGone.class, assertThrows(OsrsApiException.class, () -> client.execute(req)));
        assertInstanceOf(DomainGone.class, client.failure(client.send(req), OsrsErrorMapper.none()));
        assertNull(OsrsErrorMapper.none().map(null));
    }

    @Test
    void httpErrorsWithoutAnEnvelopeAreTransportErrors() {
        StubTransport stub = StubTransport.responding(c -> new Transport.Reply(502, "<html>Bad Gateway</html>"));
        OsrsTransportException e = assertThrows(OsrsTransportException.class,
            () -> client(stub, RetryPolicy.none()).send(XcpRequest.builder("DOMAIN", "X").build()));
        assertEquals(502, e.getHttpStatus());
    }

    @Test
    void httpErrorsCarryingAnEnvelopeAreDecoded() {
        StubTransport stub = StubTransport.responding(c -> new Transport.Reply(401, fail(410, "Reseller authentication error")));
        assertThrows(OsrsAuthenticationException.class,
            () -> client(stub, RetryPolicy.none()).execute(XcpRequest.builder("DOMAIN", "X").build()));
    }

    @Test
    void idempotentRequestsAreRetriedWithBackoff() {
        AtomicInteger calls = new AtomicInteger();
        StubTransport stub = StubTransport.responding(c -> {
            if (calls.incrementAndGet() < 3) {
                throw new UncheckedIOException(new IOException("connection reset"));
            }
            return new Transport.Reply(200, ok(200, "ok", null));
        });
        RetryPolicy p = RetryPolicy.exponential(3, Duration.ofMillis(10), Duration.ofSeconds(1));
        XcpResponse r = client(stub, p).send(XcpRequest.builder("DOMAIN", "GET").idempotent(true).build());
        assertTrue(r.isSuccess());
        assertEquals(3, calls.get());
        assertEquals(List.of(Duration.ofMillis(10), Duration.ofMillis(20)), slept);
        // every attempt carries the same, valid signature
        assertEquals(1, stub.requests().stream().map(c -> c.headers().get("X-Signature")).distinct().count());
    }

    @Test
    void chargeableRequestsAreNeverRetried() {
        AtomicInteger calls = new AtomicInteger();
        StubTransport stub = StubTransport.responding(c -> {
            calls.incrementAndGet();
            throw new UncheckedIOException(new IOException("timeout"));
        });
        RetryPolicy p = RetryPolicy.exponential(5, Duration.ofMillis(1), Duration.ofMillis(1));
        OsrsTransportException e = assertThrows(OsrsTransportException.class,
            () -> client(stub, p).send(XcpRequest.builder("DOMAIN", "SW_REGISTER").build()));
        assertEquals(-1, e.getHttpStatus());
        assertEquals(1, calls.get());
        assertTrue(slept.isEmpty());
    }

    @Test
    void retriesStopAtMaxAttemptsAndSkipClientErrors() {
        AtomicInteger calls = new AtomicInteger();
        StubTransport s503 = StubTransport.responding(c -> {
            calls.incrementAndGet();
            return new Transport.Reply(503, "busy");
        });
        RetryPolicy p = RetryPolicy.exponential(3, Duration.ofMillis(1), Duration.ofMillis(1));
        XcpRequest read = XcpRequest.builder("DOMAIN", "GET").idempotent(true).build();
        assertThrows(OsrsTransportException.class, () -> client(s503, p).send(read));
        assertEquals(3, calls.get());

        calls.set(0);
        StubTransport s404 = StubTransport.responding(c -> {
            calls.incrementAndGet();
            return new Transport.Reply(404, "no");
        });
        assertThrows(OsrsTransportException.class, () -> client(s404, p).send(read));
        assertEquals(1, calls.get());
    }

    @Test
    void interruptionStopsTheCallAndKeepsTheFlag() {
        Transport t = (u, h, b, to) -> {
            throw new InterruptedException();
        };
        OsrsException e = assertThrows(OsrsException.class,
            () -> client(t, RetryPolicy.none()).send(XcpRequest.builder("DOMAIN", "GET").build()));
        assertTrue(Thread.interrupted());
        assertTrue(e.getMessage().contains("Interrupted"));
    }

    @Test
    void lookupReadsTheResponseCode() {
        StubTransport stub = StubTransport.replying(
            ok(210, "Domain available", Map.of("status", "available")),
            ok(211, "Domain taken", Map.of("status", "taken")),
            ok(200, "Command successful", Map.of("status", "available", "reason", "Premium Name", "has_claim", "1")),
            ok(200, "Command successful", Map.of("status", "taken")),
            ok(200, "Command successful", Map.of()),
            fail(720, "Supplier unavailable"));
        OsrsClient client = client(stub, RetryPolicy.none());

        LookupResult a = client.lookup("free.com");
        assertTrue(a.available());
        assertFalse(a.premium());
        assertNull(a.hasClaim());
        assertEquals("free.com", a.domain());
        assertEquals("LOOKUP", stub.lastRequest().decoded().getString("action").orElseThrow());
        assertEquals("free.com", stub.lastRequest().decoded().getString("attributes", "domain").orElseThrow());
        assertTrue(stub.lastRequest().decoded().getString("attributes", "no_cache").isEmpty());

        LookupResult t = client.lookup("google.com", true);
        assertFalse(t.available());
        assertEquals(211, t.response().getResponseCode());
        assertEquals("1", stub.lastRequest().decoded().getString("attributes", "no_cache").orElseThrow());

        LookupResult p = client.lookup("gold.com");
        assertTrue(p.available());
        assertTrue(p.premium());
        assertTrue(p.hasClaim());

        assertFalse(client.lookup("x.com").available());
        assertThrows(OsrsApiException.class, () -> client.lookup("y.com"));
        assertEquals(720, assertThrows(OsrsUnavailableException.class, () -> client.lookup("z.com")).getResponseCode());
    }

    @Test
    void sendRawPostsASignedBody() {
        StubTransport stub = StubTransport.replying("<raw/>");
        assertEquals("<raw/>", client(stub, RetryPolicy.none()).sendRaw("<x/>"));
        assertEquals(OsrsSignature.sign("<x/>", "the-private-key"), stub.lastRequest().headers().get("X-Signature"));
    }

    @Test
    void stubWithoutRepliesLeftFailsLoudly() {
        StubTransport stub = StubTransport.replying();
        assertThrows(IllegalStateException.class, stub::lastRequest);
        assertThrows(IllegalStateException.class,
            () -> client(stub, RetryPolicy.none()).send(XcpRequest.builder("DOMAIN", "X").build()));
    }

    @Test
    void toStringAndCloseNeverExposeTheKey() {
        OsrsClient c = client(StubTransport.replying(), RetryPolicy.none());
        assertFalse(c.toString().contains("the-private-key"));
        assertSame(CONFIG, c.getConfig());
        c.close();
    }
}
