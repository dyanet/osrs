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

package com.dyanet.osrs.domains;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.dyanet.osrs.OsrsApiException;
import com.dyanet.osrs.OsrsAuthenticationException;
import com.dyanet.osrs.OsrsClient;
import com.dyanet.osrs.OsrsConfig;
import com.dyanet.osrs.OsrsUnavailableException;
import com.dyanet.osrs.transport.StubTransport;
import com.dyanet.osrs.xcp.XcpData;

class DomainsTest {

    static String fixture(String name) {
        try (InputStream in = DomainsTest.class.getResourceAsStream("/fixtures/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Domains domains(StubTransport stub) {
        return Domains.on(OsrsClient.builder().config(OsrsConfig.test("r", "k")).transport(stub).build());
    }

    private static String fail(int code, String text) {
        return StubTransport.reply(false, code, text, null);
    }

    @Test
    void deletedDomainsFromTheDocumentedReply() {
        StubTransport stub = StubTransport.replying(fixture("get-deleted-domains-reply.xml"));
        DeletedDomainsPage p = domains(stub).deletedDomains(DeletedDomainsQuery.builder()
            .deletedBetween(LocalDate.of(2021, 1, 1), LocalDate.of(2099, 1, 1))
            .expiredBetween(LocalDate.of(2020, 1, 1), null)
            .ownerEmail("*@example.com").adminEmail("a@x").billingEmail(" ").techEmail(null)
            .domain("example1.com").limit(10).page(0).build());
        assertEquals(3, p.total());
        assertEquals(40, p.pageSize());
        assertEquals(1, p.page());
        DeletedDomain d = p.domains().get(0);
        assertEquals("example1.com", d.name());
        assertEquals(Instant.ofEpochSecond(1614618491L), d.deletedAt());
        assertEquals(Instant.ofEpochSecond(1646172437L), d.expiredAt());
        assertEquals("By-Request", d.reason());

        XcpData a = stub.lastRequest().decoded().getData("attributes");
        assertEquals("2021-01-01", a.getString("del_from").orElseThrow());
        assertEquals("2030-12-31", a.getString("del_to").orElseThrow(), "clamped to OpenSRS's last year");
        assertEquals("2020-01-01", a.getString("exp_from").orElseThrow());
        assertTrue(a.getString("exp_to").isEmpty());
        assertEquals("*@example.com", a.getString("owner_email").orElseThrow());
        assertEquals("a@x", a.getString("admin_email").orElseThrow());
        assertTrue(a.getString("billing_email").isEmpty());
        assertEquals("10", a.getString("limit").orElseThrow());
        assertEquals("0", a.getString("page").orElseThrow());
    }

    /** Regression: 0.9.x sent the deletion dates as exp_from/exp_to too. */
    @Test
    void deletionAndExpiryFiltersAreIndependent() {
        StubTransport stub = StubTransport.replying(StubTransport.reply(true, 200, "ok", Map.of()));
        DeletedDomainsPage p = domains(stub).deletedDomains(DeletedDomainsQuery.builder()
            .deletedBetween(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1)).build());
        XcpData a = stub.lastRequest().decoded().getData("attributes");
        assertTrue(a.getString("exp_from").isEmpty());
        assertTrue(a.getString("exp_to").isEmpty());
        assertEquals(0, p.total());
        assertTrue(p.domains().isEmpty());
    }

    @Test
    void emptyQuerySendsNoAttributes() {
        StubTransport stub = StubTransport.replying(StubTransport.reply(true, 200, "ok", Map.of("total", "0")));
        domains(stub).deletedDomains(DeletedDomainsQuery.all());
        assertTrue(stub.lastRequest().decoded().getData("attributes").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> DeletedDomainsQuery.builder().limit(0));
        assertThrows(IllegalArgumentException.class, () -> DeletedDomainsQuery.builder().page(-1));
    }

    @Test
    void knownDomainCodesBecomeDomainExceptions() {
        StubTransport stub = StubTransport.replying(fail(440, "Registration Failed: over quota"),
            fail(480, "Domain not owned by user"), fail(555, "Domain has already been successfully renewed"),
            fail(555, "Connection refused: invalid ip address"), fail(720, "Supplier Unavailable"),
            fail(999, "Something new"));
        Domains d = domains(stub);
        DeletedDomainsQuery q = DeletedDomainsQuery.all();
        assertEquals(DomainException.Reason.INSUFFICIENT_FUNDS,
            assertThrows(DomainException.class, () -> d.deletedDomains(q)).getReason());
        assertEquals(DomainException.Reason.NOT_OWNED,
            assertThrows(DomainException.class, () -> d.deletedDomains(q)).getReason());
        assertEquals(DomainException.Reason.ALREADY_RENEWED,
            assertThrows(DomainException.class, () -> d.deletedDomains(q)).getReason());
        assertInstanceOf(OsrsAuthenticationException.class, assertThrows(OsrsApiException.class, () -> d.deletedDomains(q)));
        assertInstanceOf(OsrsUnavailableException.class, assertThrows(OsrsApiException.class, () -> d.deletedDomains(q)));
        OsrsApiException other = assertThrows(OsrsApiException.class, () -> d.deletedDomains(q));
        assertFalse(other instanceof DomainException);
        assertEquals(999, other.getResponseCode());
    }

    @Test
    void everyReasonHasCodesAndCodesAreUnique() {
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (DomainException.Reason r : DomainException.Reason.values()) {
            assertFalse(r.codes().isEmpty());
            for (int c : r.codes()) {
                assertTrue(seen.add(c), "duplicate code " + c);
            }
        }
    }
}
