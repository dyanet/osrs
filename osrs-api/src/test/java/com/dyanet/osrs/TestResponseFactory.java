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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.dyanet.osrs.jackson.OPSEnvelope;
import com.dyanet.osrs.model.DeletedDomain;
import com.dyanet.osrs.req.BelongsToRsp;
import com.dyanet.osrs.req.GetBalance;
import com.dyanet.osrs.req.GetDeletedDomains;
import com.dyanet.osrs.resp.BalanceResponse;
import com.dyanet.osrs.resp.DeletedDomainsResponse;
import com.dyanet.osrs.resp.DomainResponse;
import com.dyanet.osrs.resp.OsrsResponse;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * Offline coverage for {@link OsrsResponseFactory}: parse a canned OPS envelope
 * (no network) and assert it is mapped onto the right typed response, including
 * the {@code dt_array} path used by deleted-domain listings.
 */
public class TestResponseFactory {

    private final XmlMapper mapper = new XmlMapper();
    private final OsrsResponseFactory factory = new OsrsResponseFactory();

    private OPSEnvelope parse(String xml) throws Exception {
        return mapper.readValue(xml, OPSEnvelope.class);
    }

    private static String envelope(String attributesXml, String... topItems) {
        StringBuilder sb = new StringBuilder();
        sb.append("<OPS_envelope><header><version>0.9</version></header><body><data_block><dt_assoc>");
        for (String item : topItems) {
            sb.append(item);
        }
        sb.append("<item key=\"attributes\"><dt_assoc>").append(attributesXml).append("</dt_assoc></item>");
        sb.append("</dt_assoc></data_block></body></OPS_envelope>");
        return sb.toString();
    }

    @Test
    public void mapsBalanceResponse() throws Exception {
        String xml = envelope(
                "<item key=\"balance\">10000</item><item key=\"hold_balance\">0.00</item>",
                "<item key=\"protocol\">XCP</item>",
                "<item key=\"object\">BALANCE</item>",
                "<item key=\"action\">REPLY</item>",
                "<item key=\"response_text\">Command successful</item>",
                "<item key=\"is_success\">1</item>",
                "<item key=\"response_code\">200</item>");

        OsrsResponse response = factory.createResponse(new GetBalance(), parse(xml));
        assertFalse(response.isError());
        assertEquals(200, response.getErrorCode());
        assertEquals("REPLY", response.getAction());
        assertEquals("BALANCE", response.getObject());
        assertEquals("XCP", response.getProtocol());
        assertEquals("Command successful", response.getErrorMessage());

        BalanceResponse balance = (BalanceResponse) response;
        assertEquals(new BigDecimal("10000"), balance.getBalance().getBalance());
        assertEquals(new BigDecimal("0.00"), balance.getBalance().getHoldBalance());
    }

    @Test
    public void mapsDomainBelongsToResponse() throws Exception {
        String xml = envelope(
                "<item key=\"belongs_to_rsp\">1</item>",
                "<item key=\"object\">DOMAIN</item>",
                "<item key=\"is_success\">1</item>",
                "<item key=\"response_code\">200</item>");

        OsrsResponse response = factory.createResponse(new BelongsToRsp(), parse(xml));
        DomainResponse domain = (DomainResponse) response;
        assertTrue(domain.getDomain().isBelongsToRsp());
    }

    @Test
    public void mapsEmptyDeletedDomainsArray() throws Exception {
        String xml = envelope(
                "<item key=\"total\">0</item>"
                + "<item key=\"page_size\">40</item>"
                + "<item key=\"del_domains\"><dt_array></dt_array></item>"
                + "<item key=\"page\">1</item>",
                "<item key=\"object\">DOMAIN</item>",
                "<item key=\"action\">REPLY</item>",
                "<item key=\"is_success\">1</item>",
                "<item key=\"response_code\">200</item>");

        DeletedDomainsResponse response =
                (DeletedDomainsResponse) factory.createResponse(new GetDeletedDomains(), parse(xml));
        assertTrue(response.getDeletedDomains().isEmpty(), "empty array must yield no records");
        assertEquals(0, response.getTotal());
        assertEquals(40, response.getPageSize());
    }

    @Test
    public void mapsPopulatedDeletedDomainsArray() throws Exception {
        String xml = envelope(
                "<item key=\"total\">2</item>"
                + "<item key=\"page_size\">40</item>"
                + "<item key=\"del_domains\"><dt_array>"
                + "  <item key=\"0\"><dt_assoc>"
                + "    <item key=\"name\">one.com</item><item key=\"reason\">expired</item>"
                + "  </dt_assoc></item>"
                + "  <item key=\"1\"><dt_assoc>"
                + "    <item key=\"name\">two.net</item><item key=\"reason\">expired</item>"
                + "  </dt_assoc></item>"
                + "</dt_array></item>"
                + "<item key=\"page\">1</item>",
                "<item key=\"object\">DOMAIN</item>",
                "<item key=\"action\">REPLY</item>",
                "<item key=\"is_success\">1</item>",
                "<item key=\"response_code\">200</item>");

        DeletedDomainsResponse response =
                (DeletedDomainsResponse) factory.createResponse(new GetDeletedDomains(), parse(xml));

        List<DeletedDomain> domains = response.getDeletedDomains();
        assertEquals(2, domains.size());
        assertEquals("one.com", domains.get(0).getName());
        assertEquals("expired", domains.get(0).getReason());
        assertEquals("two.net", domains.get(1).getName());
        assertEquals(2, response.getTotal());
        assertEquals(40, response.getPageSize());
    }

    @Test
    public void nullEnvelopeMarksResponseAsError() {
        OsrsResponse response = factory.createResponse(new GetBalance(), null);
        assertTrue(response.isError());
    }

    @Test
    public void nonNumericResponseCodeFallsBackTo999() throws Exception {
        String xml = envelope(
                "<item key=\"balance\">1</item><item key=\"hold_balance\">0</item>",
                "<item key=\"is_success\">1</item>",
                "<item key=\"response_code\">not-a-number</item>");

        OsrsResponse response = factory.createResponse(new GetBalance(), parse(xml));
        assertEquals(999, response.getErrorCode());
    }

    @Test
    public void isSuccessZeroSetsErrorFlag() throws Exception {
        String xml = envelope(
                "<item key=\"balance\">1</item><item key=\"hold_balance\">0</item>",
                "<item key=\"is_success\">0</item>",
                "<item key=\"response_code\">400</item>");

        OsrsResponse response = factory.createResponse(new GetBalance(), parse(xml));
        assertTrue(response.isError());
        assertEquals(400, response.getErrorCode());
    }
}
