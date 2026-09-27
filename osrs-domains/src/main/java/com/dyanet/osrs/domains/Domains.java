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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.dyanet.osrs.OsrsClient;
import com.dyanet.osrs.xcp.XcpData;
import com.dyanet.osrs.xcp.XcpRequest;
import com.dyanet.osrs.xcp.XcpResponse;

/**
 * OpenSRS domain commands, on top of an {@link OsrsClient}.
 *
 * <pre>{@code
 * Domains domains = Domains.on(client);
 * DeletedDomainsPage p = domains.deletedDomains(DeletedDomainsQuery.all());
 * }</pre>
 *
 * <p>The basic commands that belong to no family are in {@code osrs-api}:
 * {@link OsrsClient#lookup(String)}, {@link OsrsClient#balance()} and
 * {@link OsrsClient#belongsToRsp(String)}.
 * Failures with a known domain response code throw {@link DomainException}.
 */
public final class Domains {

    /** The XCP object of every command here. */
    public static final String OBJECT = "DOMAIN";

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

}
