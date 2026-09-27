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
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.dyanet.osrs.transport.StubTransport;
import com.dyanet.osrs.xcp.XcpCodecTest;
import com.dyanet.osrs.xcp.XcpData;

/** The commands kept in osrs-api because they belong to no family: balance and belongs_to_rsp. */
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
}
