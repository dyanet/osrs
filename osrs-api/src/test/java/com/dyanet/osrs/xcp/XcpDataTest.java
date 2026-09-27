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

package com.dyanet.osrs.xcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class XcpDataTest {

    private final XcpData d = XcpData.of(Map.of(
        "n", " 42 ", "big", "9000.01", "long", "1614618491", "bad", "x",
        "yes", "Y", "no", "0", "maybe", "?",
        "list", List.of("a", Map.of("k", "v")),
        "assoc", Map.of("inner", "i")));

    @Test
    void typedAccessors() {
        assertEquals(42, d.getInt("n").orElseThrow());
        assertEquals(new BigDecimal("9000.01"), d.getDecimal("big").orElseThrow());
        assertEquals(1614618491L, d.getLong("long").orElseThrow());
        assertTrue(d.getInt("bad").isEmpty());
        assertTrue(d.getLong("bad").isEmpty());
        assertTrue(d.getDecimal("bad").isEmpty());
        assertTrue(d.getFlag("yes").orElseThrow());
        assertFalse(d.getFlag("no").orElseThrow());
        assertTrue(d.getFlag("maybe").isEmpty());
        assertEquals("i", d.getData("assoc").getString("inner").orElseThrow());
        assertEquals("v", d.getString("list", "1", "k").orElseThrow());
        assertEquals(1, d.getDataList("list").size());
    }

    @Test
    void missingPathsAndMismatchesAreEmpty() {
        assertTrue(d.find("nope").isEmpty());
        assertTrue(d.find("n", "deeper").isEmpty());
        assertTrue(d.find("list", "x").isEmpty());
        assertTrue(d.find("list", "9").isEmpty());
        assertTrue(d.getString("assoc").isEmpty());
        assertTrue(d.getData("n").isEmpty());
        assertTrue(d.getList("assoc").isEmpty());
        assertSame(XcpData.empty(), XcpData.of(null));
        assertSame(XcpData.empty(), XcpData.of(Map.of()));
    }

    @Test
    void toStringShowsKeysNotValues() {
        String s = XcpData.of(Map.of("auth_info", "s3cret")).toString();
        assertTrue(s.contains("auth_info"));
        assertFalse(s.contains("s3cret"));
        assertEquals(Map.of("auth_info", "s3cret"), XcpData.of(Map.of("auth_info", "s3cret")).asMap());
    }

    @Test
    void requestToStringHidesValues() {
        XcpRequest r = XcpRequest.builder("DOMAIN", "SW_REGISTER")
            .attribute("reg_password", "hunter2").attribute("drop", "x").attribute("drop", null)
            .attributes(Map.of("domain", "example.com")).registrantIp(" ").build();
        assertFalse(r.toString().contains("hunter2"));
        assertTrue(r.toString().contains("reg_password"));
        assertFalse(r.getAttributes().containsKey("drop"));
        assertEquals(null, r.getRegistrantIp());
        assertFalse(r.isIdempotent());
    }
}
