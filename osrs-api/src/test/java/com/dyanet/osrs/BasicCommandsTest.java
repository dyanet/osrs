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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.dyanet.osrs.transport.StubTransport;
import com.dyanet.osrs.xcp.XcpCodec;
import com.dyanet.osrs.xcp.XcpCodecTest;
import com.dyanet.osrs.xcp.XcpData;

/** The commands kept in osrs-api because they belong to no family (lookup is in OsrsClientTest). */
class BasicCommandsTest {

    private static OsrsClient client(StubTransport stub) {
        return OsrsClient.builder().config(OsrsConfig.test("r", "k")).transport(stub).build();
    }

    @Test
    void balanceFromTheDocumentedReply() {
        StubTransport stub = StubTransport.replying(XcpCodecTest.fixture("get-balance-reply.xml"));
        Balance b = client(stub).balance();
        assertEquals(new BigDecimal("9000.01"), b.balance());
        assertEquals(new BigDecimal("0.00"), b.holdBalance());
        assertEquals(new BigDecimal("9000.01"), b.available());
        XcpData sent = stub.lastRequest().decoded();
        assertEquals("DOMAIN", sent.getString("object").orElseThrow());
        assertEquals("GET_BALANCE", sent.getString("action").orElseThrow());
        assertTrue(sent.getData("attributes").isEmpty());
    }

    @Test
    void balanceWithoutTheFieldIsAnError() {
        StubTransport stub = StubTransport.replying(StubTransport.reply(true, 200, "ok", Map.of("balance", "1")),
            StubTransport.reply(true, 200, "ok", Map.of()));
        assertEquals(BigDecimal.ZERO, client(stub).balance().holdBalance());
        assertThrows(OsrsApiException.class, () -> client(stub).balance());
    }

    @Test
    void belongsToRspFromTheDocumentedReply() {
        StubTransport stub = StubTransport.replying(XcpCodecTest.fixture("belongs-to-rsp-reply.xml"));
        RspOwnership o = client(stub).belongsToRsp("example.com");
        assertTrue(o.belongs());
        assertEquals("example.com", o.domain());
        assertEquals(LocalDateTime.of(2023, 11, 1, 0, 49, 10), o.expiryDate());
        assertEquals("BELONGS_TO_RSP", stub.lastRequest().decoded().getString("action").orElseThrow());
        assertEquals("example.com", stub.lastRequest().decoded().getString("attributes", "domain").orElseThrow());
    }

    @Test
    void unknownDomainsReportFalseEvenOnFailure() {
        StubTransport stub = StubTransport.replying(
            StubTransport.reply(false, 465, "Unknown Domain", Map.of("belongs_to_rsp", "0")),
            StubTransport.reply(true, 200, "ok", Map.of("belongs_to_rsp", "1", "domain_expdate", "garbage")),
            StubTransport.reply(false, 410, "Reseller authentication error", null),
            StubTransport.reply(true, 200, "ok", Map.of()));
        OsrsClient c = client(stub);
        RspOwnership o = c.belongsToRsp("gone.com");
        assertFalse(o.belongs());
        assertNull(o.expiryDate());
        assertNull(c.belongsToRsp("odd.com").expiryDate());
        assertThrows(OsrsAuthenticationException.class, () -> c.belongsToRsp("x.com"));
        assertThrows(OsrsApiException.class, () -> c.belongsToRsp("y.com"));
    }

    @Test
    void nameSuggestFromTheDocumentedReply() {
        StubTransport stub = StubTransport.replying(XcpCodecTest.fixture("name-suggest-reply.xml"));
        NameSuggestResult r = client(stub).suggest(NameSuggestQuery.of("example", ".com", ".net")
            .services(NameSuggestQuery.Service.LOOKUP, NameSuggestQuery.Service.SUGGESTION));
        assertTrue(r.completed());
        assertNull(r.searchKey());
        NameSuggestResult.Section lookup = r.section(NameSuggestQuery.Service.LOOKUP).orElseThrow();
        assertTrue(lookup.success());
        assertEquals(200, lookup.responseCode());
        assertEquals(2, lookup.count());
        assertEquals(NameSuggestResult.Status.TAKEN, lookup.candidates().get(0).status());
        assertFalse(lookup.candidates().get(0).available());
        assertEquals(5, r.section(NameSuggestQuery.Service.SUGGESTION).orElseThrow().count());
        assertTrue(r.section(NameSuggestQuery.Service.PREMIUM).isEmpty());
        assertEquals(List.of("example.net", "myexample.com"),
            r.available().stream().map(NameSuggestResult.Candidate::domain).toList());

        XcpData a = stub.lastRequest().decoded();
        assertEquals("NAME_SUGGEST", a.getString("action").orElseThrow());
        assertEquals("example", a.getString("attributes", "searchstring").orElseThrow());
        assertEquals(List.of(".com", ".net"), a.getList("attributes", "tlds"));
        assertEquals(List.of("lookup", "suggestion"), a.getList("attributes", "services"));
    }

    @Test
    void nameSuggestOptionsPremiumPricesAndIncompleteSearches() {
        Map<String, Object> premium = Map.of("count", "1", "is_success", "1", "response_code", "200",
            "items", List.of(Map.of("domain", "gold.com", "status", "available", "price", "2500.00")));
        Map<String, Object> offer = Map.of("is_success", "0", "response_code", "500", "items",
            List.of(Map.of("domain", "x.com", "status", "weird", "third_party_offer_url", "https://offer.example/x")));
        String reply = XcpCodec.encodeEnvelope(Map.of("is_success", 1, "response_code", 200,
            "response_text", "ok", "is_search_completed", 0, "search_key", "abc123",
            "attributes", Map.of("premium", premium, "premium_make_offer", offer)));
        StubTransport stub = StubTransport.replying(reply);
        NameSuggestResult r = client(stub).suggest(NameSuggestQuery.of("gold", ".com")
            .services(NameSuggestQuery.Service.PREMIUM, NameSuggestQuery.Service.PREMIUM_MAKE_OFFER)
            .languages("en", "fr").maxWaitTime(Duration.ofMillis(1500)).searchKey("prev")
            .skipRegistryLookup(true)
            .override(NameSuggestQuery.Service.PREMIUM, Map.of("price_max", 5000))
            .override(NameSuggestQuery.Service.SUGGESTION, Map.of("maximum", 20)));
        assertFalse(r.completed());
        assertEquals("abc123", r.searchKey());
        assertEquals(new BigDecimal("2500.00"),
            r.section(NameSuggestQuery.Service.PREMIUM).orElseThrow().candidates().get(0).price());
        NameSuggestResult.Section o = r.section(NameSuggestQuery.Service.PREMIUM_MAKE_OFFER).orElseThrow();
        assertFalse(o.success());
        assertEquals(1, o.count());
        assertEquals(NameSuggestResult.Status.UNKNOWN, o.candidates().get(0).status());
        assertEquals("https://offer.example/x", o.candidates().get(0).offerUrl());

        XcpData a = stub.lastRequest().decoded().getData("attributes");
        assertEquals(List.of("en", "fr"), a.getList("languages"));
        assertEquals("1.5", a.getString("max_wait_time").orElseThrow());
        assertEquals("prev", a.getString("search_key").orElseThrow());
        assertEquals("1", a.getString("skip_registry_lookup").orElseThrow());
        assertEquals("5000", a.getString("service_override", "premium", "price_max").orElseThrow());
        assertEquals("20", a.getString("service_override", "suggestion", "maximum").orElseThrow());
    }

    @Test
    void nameSuggestQueryValidation() {
        assertThrows(IllegalArgumentException.class, () -> NameSuggestQuery.of(" ", ".com"));
        assertThrows(IllegalArgumentException.class, () -> NameSuggestQuery.of("x"));
        assertThrows(IllegalArgumentException.class, () -> NameSuggestQuery.of("x", ".com").maxWaitTime(Duration.ofMillis(50)));
        XcpData a = XcpData.of(NameSuggestQuery.of("x", ".com").services().attributes());
        assertTrue(a.find("services").isEmpty());
        assertTrue(a.find("languages").isEmpty());
    }

    @Test
    void priceFromTheDocumentedReply() {
        StubTransport stub = StubTransport.replying(XcpCodecTest.fixture("get-price-reply.xml"),
            StubTransport.reply(true, 200, "ok", Map.of("price", "3000", "is_registry_premium", "1",
                "registry_premium_group", "tier3")),
            StubTransport.reply(true, 200, "ok", Map.of()));
        OsrsClient c = client(stub);
        PriceQuote q = c.price("example.com", 1, PriceQuote.PriceType.NEW);
        assertEquals(new BigDecimal("20.2"), q.price());
        assertNull(q.registryPremium());
        XcpData a = stub.lastRequest().decoded();
        assertEquals("GET_PRICE", a.getString("action").orElseThrow());
        assertEquals("1", a.getString("attributes", "period").orElseThrow());
        assertEquals("new", a.getString("attributes", "reg_type").orElseThrow());

        PriceQuote p = c.price("gold.com", 2, PriceQuote.PriceType.RENEWAL);
        assertTrue(p.registryPremium());
        assertEquals("tier3", p.premiumGroup());
        assertEquals("renewal", stub.lastRequest().decoded().getString("attributes", "reg_type").orElseThrow());
        assertThrows(OsrsApiException.class, () -> c.price("x.com", 1, PriceQuote.PriceType.TRANSFER));
        assertThrows(IllegalArgumentException.class, () -> c.price("x.com", 0, PriceQuote.PriceType.TRADE));
    }
}
