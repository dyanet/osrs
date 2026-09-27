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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.dyanet.osrs.OsrsProtocolException;

public class XcpCodecTest {

    public static String fixture(String name) {
        try (InputStream in = XcpCodecTest.class.getResourceAsStream("/fixtures/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<String, Object> contact(String first) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("first_name", first);
        c.put("country", "CA");
        c.put("fax", null); // left out
        return c;
    }

    @Test
    void nestedAttributesBecomeDtAssocAndDtArray() {
        XcpRequest req = XcpRequest.builder("DOMAIN", "SW_REGISTER")
            .attribute("domain", "example.com")
            .attribute("contact_set", Map.of("owner", contact("Ann")))
            .attribute("nameserver_list", List.of(
                Map.of("name", "ns1.example.net", "sortorder", 1),
                Map.of("name", "ns2.example.net", "sortorder", 2)))
            .build();
        String xml = XcpCodec.encodeRequest(req);

        assertTrue(xml.startsWith("<?xml version='1.0' encoding='UTF-8' standalone='no' ?>\n"
            + "<!DOCTYPE OPS_envelope SYSTEM 'ops.dtd'>\n<OPS_envelope><header><version>0.9</version>"));
        assertTrue(xml.contains("<item key=\"contact_set\"><dt_assoc><item key=\"owner\"><dt_assoc>"
            + "<item key=\"first_name\">Ann</item><item key=\"country\">CA</item></dt_assoc>"));
        assertTrue(xml.contains("<item key=\"nameserver_list\"><dt_array><item key=\"0\"><dt_assoc>"));
        assertFalse(xml.contains("fax"));

        XcpData back = XcpData.of(XcpCodec.decode(xml));
        assertEquals("XCP", back.getString("protocol").orElseThrow());
        assertEquals("SW_REGISTER", back.getString("action").orElseThrow());
        assertEquals("Ann", back.getString("attributes", "contact_set", "owner", "first_name").orElseThrow());
        assertEquals("ns2.example.net", back.getString("attributes", "nameserver_list", "1", "name").orElseThrow());
        assertEquals(2, back.getDataList("attributes", "nameserver_list").size());
        assertEquals(2, back.getInt("attributes", "nameserver_list", "1", "sortorder").orElseThrow());
    }

    @Test
    void scalarTypesAreEncodedTheWayOpenSrsExpects() {
        enum Handle { PROCESS }
        String xml = XcpCodec.encodeRequest(XcpRequest.builder("DOMAIN", "X")
            .attribute("flag_on", true).attribute("flag_off", false)
            .attribute("price", new BigDecimal("1E+1"))
            .attribute("period", 2).attribute("handle", Handle.PROCESS)
            .attribute("date", LocalDate.of(2030, 1, 2)).attribute("arr", new Object[] {"a", 'b'})
            .registrantIp("203.0.113.9").build());
        XcpData d = XcpData.of(XcpCodec.decode(xml));
        assertEquals("1", d.getString("attributes", "flag_on").orElseThrow());
        assertEquals("0", d.getString("attributes", "flag_off").orElseThrow());
        assertEquals("10", d.getString("attributes", "price").orElseThrow());
        assertEquals("2", d.getString("attributes", "period").orElseThrow());
        assertEquals("PROCESS", d.getString("attributes", "handle").orElseThrow());
        assertEquals("2030-01-02", d.getString("attributes", "date").orElseThrow());
        assertEquals(List.of("a", "b"), d.getList("attributes", "arr"));
        assertEquals("203.0.113.9", d.getString("registrant_ip").orElseThrow());
    }

    @Test
    void textIsEscapedAndRoundTrips() {
        String nasty = "A&B <c> \"d\" 'e' é 😀\ttab";
        String xml = XcpCodec.encodeRequest(XcpRequest.builder("DOMAIN", "X")
            .attribute("org_name", nasty).attribute("k&<\"", "v").build());
        assertTrue(xml.contains("A&amp;B &lt;c&gt; &quot;d&quot; &apos;e&apos;"));
        XcpData d = XcpData.of(XcpCodec.decode(xml));
        assertEquals(nasty, d.getString("attributes", "org_name").orElseThrow());
        assertEquals("v", d.getString("attributes", "k&<\"").orElseThrow());
    }

    @Test
    void requestWithoutAttributesOmitsTheAttributesItem() {
        String xml = XcpCodec.encodeRequest(XcpRequest.builder("DOMAIN", "GET_BALANCE").build());
        assertFalse(xml.contains("attributes"));
        assertTrue(xml.contains("<item key=\"protocol\">XCP</item><item key=\"action\">GET_BALANCE</item>"
            + "<item key=\"object\">DOMAIN</item>"));
    }

    @Test
    void invalidValuesAreRejectedWithTheirPath() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> XcpCodec.encodeRequest(
            XcpRequest.builder("D", "A").attribute("x", Map.of("y", new Object())).build()));
        assertTrue(e.getMessage().contains("x.y"), e.getMessage());

        List<String> withNull = new java.util.ArrayList<>();
        withNull.add(null);
        e = assertThrows(IllegalArgumentException.class, () -> XcpCodec.encodeRequest(
            XcpRequest.builder("D", "A").attribute("list", withNull).build()));
        assertTrue(e.getMessage().contains("list[0]"), e.getMessage());

        e = assertThrows(IllegalArgumentException.class, () -> XcpCodec.encodeRequest(
            XcpRequest.builder("D", "A").attribute("bad", "a\u0001b").build()));
        assertTrue(e.getMessage().contains("U+0001"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> XcpCodec.encodeRequest(
            XcpRequest.builder("D", "A").attribute("bad", "\uD800").build()));
    }

    @Test
    void decodesTheDocumentedGetDeletedDomainsReply() {
        XcpResponse r = XcpCodec.decodeResponse(null, fixture("get-deleted-domains-reply.xml"));
        assertTrue(r.isSuccess());
        assertEquals(200, r.getResponseCode());
        assertEquals("Command successful", r.getResponseText());
        assertEquals("DOMAIN", r.getObject());
        assertEquals("REPLY", r.getAction());
        assertEquals("REPLY", r.getRequestAction());
        assertEquals("XCP", r.getProtocol());
        assertEquals(3, r.getAttributes().getInt("total").orElseThrow());
        List<XcpData> rows = r.getAttributes().getDataList("del_domains");
        assertEquals(1, rows.size());
        assertEquals("example1.com", rows.get(0).getString("name").orElseThrow());
        assertEquals(1614618491L, rows.get(0).getLong("delete_date_epoch").orElseThrow());
        assertTrue(r.getRawXml().contains("example1.com"));
        assertTrue(r.toString().contains("code=200"));
    }

    @Test
    void arrayItemsAreOrderedByTheirIndexKeys() {
        String xml = "<OPS_envelope><body><data_block><dt_assoc><item key=\"a\"><dt_array>"
            + "<item key=\"2\">c</item><item key=\"0\">a</item><item key=\"1\">b</item>"
            + "</dt_array></item><item key=\"s\"><dt_scalar>x</dt_scalar></item>"
            + "<item key=\"odd\"><unknown><deep/></unknown>text</item>"
            + "<item key=\"empty\"/></dt_assoc></data_block></body></OPS_envelope>";
        XcpData d = XcpData.of(XcpCodec.decode(xml));
        assertEquals(List.of("a", "b", "c"), d.getList("a"));
        assertEquals("x", d.getString("s").orElseThrow());
        assertEquals("text", d.getString("odd").orElseThrow());
        assertEquals("", d.getString("empty").orElseThrow());
    }

    @Test
    void nonEnvelopeRepliesAreProtocolErrors() {
        assertThrows(OsrsProtocolException.class, () -> XcpCodec.decode(null));
        assertThrows(OsrsProtocolException.class, () -> XcpCodec.decode("  "));
        OsrsProtocolException e = assertThrows(OsrsProtocolException.class,
            () -> XcpCodec.decode("<html><body>502 Bad Gateway</body></html>"));
        assertTrue(e.getMessage().contains("502 Bad Gateway"));
        assertThrows(OsrsProtocolException.class, () -> XcpCodec.decode("not xml at all"));
        assertThrows(OsrsProtocolException.class, () -> XcpCodec.decode("<OPS_envelope><body/></OPS_envelope>"));
        assertThrows(OsrsProtocolException.class,
            () -> XcpCodec.decode("<OPS_envelope><body><data_block>x</data_block></body></OPS_envelope>"));
        assertThrows(OsrsProtocolException.class,
            () -> XcpCodec.decode("<OPS_envelope><body><data_block><dt_assoc><item key=\"a\">"));
    }

    @Test
    void externalEntitiesAreNeverResolved(@TempDir Path dir) throws IOException {
        Path secret = dir.resolve("secret.txt");
        Files.writeString(secret, "TOP-SECRET");
        String xxe = "<?xml version=\"1.0\"?><!DOCTYPE OPS_envelope [<!ENTITY x SYSTEM \""
            + secret.toUri() + "\">]><OPS_envelope><body><data_block><dt_assoc>"
            + "<item key=\"v\">&x;</item></dt_assoc></data_block></body></OPS_envelope>";
        try {
            Map<String, Object> m = XcpCodec.decode(xxe);
            assertFalse(String.valueOf(m).contains("TOP-SECRET"));
        } catch (OsrsProtocolException expected) {
            assertFalse(expected.getMessage().contains("TOP-SECRET"));
        }
    }

    @Test
    void entityExpansionIsNotPerformed() {
        String bomb = "<?xml version=\"1.0\"?><!DOCTYPE OPS_envelope [<!ENTITY a \"aaaaaaaaaa\">"
            + "<!ENTITY b \"&a;&a;&a;&a;&a;&a;&a;&a;&a;&a;\"><!ENTITY c \"&b;&b;&b;&b;&b;&b;&b;&b;&b;&b;\">]>"
            + "<OPS_envelope><body><data_block><dt_assoc><item key=\"v\">&c;</item>"
            + "</dt_assoc></data_block></body></OPS_envelope>";
        try {
            Map<String, Object> m = XcpCodec.decode(bomb);
            assertFalse(String.valueOf(m.get("v")).contains("aaaaaaaaaaaaaaaaaaaa"));
        } catch (OsrsProtocolException expected) {
            assertInstanceOf(OsrsProtocolException.class, expected);
        }
    }
}
