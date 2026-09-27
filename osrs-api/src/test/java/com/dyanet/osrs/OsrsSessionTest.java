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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.dyanet.osrs.transport.StubTransport;
import com.dyanet.osrs.transport.Transport;
import com.dyanet.osrs.xcp.XcpCodec;
import com.dyanet.osrs.xcp.XcpRequest;
import com.dyanet.osrs.xcp.XcpResponse;

class OsrsSessionTest {

    private static final String OK = StubTransport.reply(true, 200, "Command successful", Map.of());

    private static OsrsClient client(Transport t) {
        return OsrsClient.builder().config(OsrsConfig.test("r", "k")).transport(t).build();
    }

    private static XcpRequest req(String action, String domain) {
        return XcpRequest.builder("DOMAIN", action).attribute("domain", domain).build();
    }

    private static Throwable cause(CompletableFuture<?> f) {
        ExecutionException e = assertThrows(ExecutionException.class, () -> f.get(5, TimeUnit.SECONDS));
        return e.getCause();
    }

    @Test
    void commandsRunOneAtATimeInSubmissionOrder() throws Exception {
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger maxInFlight = new AtomicInteger();
        List<String> order = Collections.synchronizedList(new ArrayList<>());
        StubTransport slow = StubTransport.responding(c -> {
            maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), Math::max);
            order.add(c.decoded().getString("attributes", "domain").orElseThrow());
            try {
                Thread.sleep(20); // a lagging OpenSRS
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            inFlight.decrementAndGet();
            return new Transport.Reply(200, OK);
        });
        try (OsrsSession s = client(slow).openSession("order 1042")) {
            List<CompletableFuture<XcpResponse>> fs = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                fs.add(s.submit(req("MODIFY", "d" + i + ".com")));
            }
            s.drain();
            for (CompletableFuture<XcpResponse> f : fs) {
                assertTrue(f.get().isSuccess());
            }
            assertEquals(1, maxInFlight.get(), "never more than one request in flight");
            assertEquals(List.of("d0.com", "d1.com", "d2.com", "d3.com", "d4.com", "d5.com", "d6.com", "d7.com"), order);
            assertNull(s.getLastFlush());
            assertEquals(0, s.pending());
        }
    }

    @Test
    void aFailureCancelsEverythingQueuedAfterItAndExplainsWhy() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        StubTransport stub = StubTransport.responding(c -> {
            String d = c.decoded().getString("attributes", "domain").orElse("");
            if (d.equals("first.com")) {
                try {
                    release.await(5, TimeUnit.SECONDS); // hold the queue so the rest line up
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return new Transport.Reply(200, OK);
            }
            if (d.equals("broke.com")) {
                return new Transport.Reply(200, StubTransport.reply(false, 440,
                    "Registration Failed: over quota", null));
            }
            return new Transport.Reply(200, OK);
        });
        AtomicReference<QueueFlush> heard = new AtomicReference<>();
        OsrsSession s = client(stub).openSession("order 1042").onFlush(heard::set);

        CompletableFuture<XcpResponse> first = s.submit(req("MODIFY", "first.com"));
        CompletableFuture<XcpResponse> broke = s.submit(req("SW_REGISTER", "broke.com"));
        CompletableFuture<XcpResponse> after1 = s.submit(req("RENEW", "after1.com"));
        CompletableFuture<String> after2 = s.submit("set DNS for after2.com", c -> "never");
        release.countDown();
        s.drain();

        assertTrue(first.get().isSuccess());
        OsrsApiException failed = assertInstanceOf(OsrsApiException.class, cause(broke));
        assertEquals(440, failed.getResponseCode());

        OsrsRequestCancelledException c1 = assertInstanceOf(OsrsRequestCancelledException.class, cause(after1));
        assertInstanceOf(OsrsRequestCancelledException.class, cause(after2));
        assertTrue(c1.getMessage().contains("\"DOMAIN RENEW after1.com\" was not sent"), c1.getMessage());
        assertTrue(c1.getMessage().contains("\"DOMAIN SW_REGISTER broke.com\", failed"), c1.getMessage());
        assertTrue(c1.getMessage().contains("send it again"), c1.getMessage());
        assertSame(failed, c1.getCause());

        // nothing after the failure reached OpenSRS
        assertEquals(2, stub.requests().size());

        QueueFlush f = heard.get();
        assertSame(f, s.getLastFlush());
        assertSame(f, c1.getFlush());
        assertEquals("order 1042", f.session());
        assertEquals("DOMAIN SW_REGISTER broke.com", f.failedRequest());
        assertEquals(List.of("DOMAIN RENEW after1.com", "set DNS for after2.com"), f.cancelled());
        assertEquals("The request \"DOMAIN SW_REGISTER broke.com\" did not go through (OpenSRS said:"
            + " Registration Failed: over quota, code 440). To keep your account consistent, all 2"
            + " requests waiting after it were cancelled and not sent: DOMAIN RENEW after1.com;"
            + " set DNS for after2.com. Fix the problem, then send them again.", f.message());

        // the session keeps working for requests submitted afterwards
        assertTrue(s.call(req("MODIFY", "later.com")).isSuccess());
        s.close();
    }

    @Test
    void messagesForOtherKindsOfFailure() {
        QueueFlush one = new QueueFlush("s", "renew a.com", new OsrsTransportException("x", -1, null), List.of("b"));
        assertEquals("The request \"renew a.com\" did not go through (OpenSRS could not be reached or did not"
            + " answer). To keep your account consistent, the 1 request waiting after it was cancelled"
            + " and not sent: b. Fix the problem, then send it again.", one.message());
        QueueFlush none = new QueueFlush("s", "x", new OsrsProtocolException("bad", null), List.of());
        assertEquals("The request \"x\" did not go through (OpenSRS sent a reply that could not be read)."
            + " No other requests were waiting, so nothing else was affected.", none.message());
        assertTrue(new QueueFlush("s", "x", new IllegalStateException("boom"), List.of()).message().contains("(boom)"));
        assertFalse(new QueueFlush("s", "x", new IllegalStateException(), List.of()).message().contains("()"));
        XcpResponse noText = XcpCodec.decodeResponse(null, StubTransport.reply(false, 465, "", null));
        assertTrue(new QueueFlush("s", "x", new OsrsApiException(noText), List.of()).message()
            .contains("(OpenSRS said: error 465)"));
    }

    @Test
    void failingFunctionsAndListenersAreHandled() throws Exception {
        OsrsSession s = client(StubTransport.replying(OK)).openSession(" ");
        assertEquals("session", s.getName());
        s.onFlush(f -> {
            throw new IllegalStateException("listener bug");
        });
        CompletableFuture<Object> bad = s.submit(null, c -> {
            throw new IllegalArgumentException("bad input");
        });
        s.drain();
        assertInstanceOf(IllegalArgumentException.class, cause(bad));
        assertEquals("request", s.getLastFlush().failedRequest());
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
            () -> s.call("again", c -> { throw new IllegalArgumentException("x"); }));
        assertEquals("x", thrown.getMessage());
        assertEquals("ok", s.call("fine", c -> "ok"));
        assertTrue(s.toString().contains("session"));
    }

    @Test
    void closingCancelsWhatHasNotStarted() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        OsrsSession s = client(StubTransport.replying(OK, OK)).openSession("closing");
        CompletableFuture<String> running = s.submit("running", c -> {
            started.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "done";
        });
        started.await(5, TimeUnit.SECONDS);
        CompletableFuture<String> waiting = s.submit("waiting", c -> "no");
        assertEquals(1, s.pending());
        s.close();
        OsrsRequestCancelledException e = assertInstanceOf(OsrsRequestCancelledException.class, cause(waiting));
        assertTrue(e.getMessage().contains("session \"closing\" was closed"), e.getMessage());
        assertNull(e.getFlush());
        assertThrows(IllegalStateException.class, () -> s.submit("late", c -> "x"));
        release.countDown();
        assertEquals("done", running.get(5, TimeUnit.SECONDS));
    }

    @Test
    void describeUsesObjectActionAndDomain() {
        assertEquals("DOMAIN GET_BALANCE", OsrsSession.describe(XcpRequest.builder("DOMAIN", "GET_BALANCE").build()));
        assertEquals("DOMAIN RENEW x.com", OsrsSession.describe(req("RENEW", "x.com")));
    }
}
