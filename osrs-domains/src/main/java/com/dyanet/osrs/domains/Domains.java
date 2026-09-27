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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.dyanet.osrs.OsrsApiException;
import com.dyanet.osrs.OsrsClient;
import com.dyanet.osrs.xcp.XcpData;
import com.dyanet.osrs.xcp.XcpRequest;
import com.dyanet.osrs.xcp.XcpResponse;

/**
 * OpenSRS domain commands, on top of an {@link OsrsClient}.
 *
 * <pre>{@code
 * Domains domains = Domains.on(client);
 * Balance b = domains.balance();
 * RspOwnership o = domains.belongsToRsp("example.com");
 * DeletedDomainsPage p = domains.deletedDomains(DeletedDomainsQuery.all());
 * }</pre>
 *
 * <p>Availability checks ({@code LOOKUP}) are in {@code osrs-api}: {@link OsrsClient#lookup(String)}.
 * Failures with a known domain response code throw {@link DomainException}.
 */
public final class Domains {

    /** The XCP object of every command here. */
    public static final String OBJECT = "DOMAIN";

    private static final DateTimeFormatter EXPDATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OsrsClient client;

    private Domains(OsrsClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    /**
     * @param client the connection to use
     * @return the domain commands
     */
    public static Domains on(OsrsClient client) {
        return new Domains(client);
    }

    private XcpResponse call(XcpRequest request) {
        return client.execute(request, DomainException.MAPPER);
    }

    /**
     * The reseller account balance ({@code GET_BALANCE}).
     *
     * @return the balance
     */
    public Balance balance() {
        XcpData a = call(XcpRequest.builder(OBJECT, "GET_BALANCE").idempotent(true).build())
            .getAttributes();
        return new Balance(
            a.getDecimal("balance").orElseThrow(() -> missing("GET_BALANCE", "balance")),
            a.getDecimal("hold_balance").orElse(java.math.BigDecimal.ZERO));
    }

    /**
     * Whether a domain is managed by this reseller ({@code BELONGS_TO_RSP}). Domains OpenSRS
     * doesn't manage, and expired domains past their grace period, report {@code false}, even
     * when OpenSRS answers with an "Unknown Domain" error.
     *
     * @param domain the domain name
     * @return the answer
     */
    public RspOwnership belongsToRsp(String domain) {
        XcpRequest req = XcpRequest.builder(OBJECT, "BELONGS_TO_RSP")
            .attribute("domain", Objects.requireNonNull(domain, "domain")).idempotent(true).build();
        XcpResponse r = client.send(req);
        XcpData a = r.getAttributes();
        Boolean belongs = a.getFlag("belongs_to_rsp").orElse(null);
        if (belongs == null) {
            if (!r.isSuccess()) {
                throw client.failure(r, DomainException.MAPPER);
            }
            throw missing("BELONGS_TO_RSP", "belongs_to_rsp");
        }
        LocalDateTime expiry = belongs
            ? a.getString("domain_expdate").map(Domains::parseExpdate).orElse(null) : null;
        return new RspOwnership(domain, belongs, expiry);
    }

    /**
     * Domains deleted from this reseller's account ({@code GET_DELETED_DOMAINS}).
     *
     * @param query the filters and page
     * @return one page of results
     */
    public DeletedDomainsPage deletedDomains(DeletedDomainsQuery query) {
        XcpData a = call(XcpRequest.builder(OBJECT, "GET_DELETED_DOMAINS")
            .attributes(query.attributes()).idempotent(true).build()).getAttributes();
        List<DeletedDomain> rows = new ArrayList<>();
        for (XcpData d : a.getDataList("del_domains")) {
            rows.add(new DeletedDomain(
                d.getString("name").orElse(""),
                d.getLong("delete_date_epoch").map(Instant::ofEpochSecond).orElse(null),
                d.getLong("expiredate_epoch").map(Instant::ofEpochSecond).orElse(null),
                d.getString("reason").orElse("")));
        }
        return new DeletedDomainsPage(
            a.getInt("total").orElse(rows.size()),
            a.getInt("page_size").orElse(rows.size()),
            a.getInt("page").orElse(0),
            List.copyOf(rows));
    }

    static LocalDateTime parseExpdate(String s) {
        try {
            return LocalDateTime.parse(s.trim(), EXPDATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static OsrsApiException missing(String action, String attribute) {
        return new OsrsApiException("OpenSRS " + OBJECT + " " + action + " reply has no " + attribute, null);
    }
}
