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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.dyanet.osrs.NameSuggestQuery.Service;
import com.dyanet.osrs.xcp.XcpData;
import com.dyanet.osrs.xcp.XcpResponse;

/**
 * The results of a {@code NAME_SUGGEST} search, one {@link Section} per service.
 *
 * @param completed whether OpenSRS finished the search; if not, send the query again with
 *                  {@link #searchKey()} to get the rest
 * @param searchKey the key to continue an incomplete search, or {@code null}
 * @param sections  the results per service that OpenSRS returned
 * @param response  the full reply
 */
public record NameSuggestResult(boolean completed, String searchKey, Map<Service, Section> sections,
        XcpResponse response) {

    /** Availability of one candidate name. */
    public enum Status {
        /** Can be registered. */
        AVAILABLE,
        /** Already registered. */
        TAKEN,
        /** Recently sold. */
        JUSTSOLD,
        /** OpenSRS couldn't tell. */
        UNDETERMINED,
        /** Still being checked. */
        IN_PROGRESS,
        /** A value this client doesn't know. */
        UNKNOWN;

        static Status of(String s) {
            try {
                return valueOf(s.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return UNKNOWN;
            }
        }
    }

    /**
     * One candidate name.
     *
     * @param domain   the name
     * @param status   its availability
     * @param price    the price (premium services only), or {@code null}
     * @param offerUrl where to make an offer ({@code premium_make_offer} only), or {@code null}
     */
    public record Candidate(String domain, Status status, BigDecimal price, String offerUrl) {

        /**
         * @return whether {@link #status()} is {@link Status#AVAILABLE}
         */
        public boolean available() {
            return status == Status.AVAILABLE;
        }
    }

    /**
     * The results of one service.
     *
     * @param success      whether this service succeeded
     * @param responseCode its response code
     * @param count        how many results OpenSRS reported
     * @param candidates   the results
     */
    public record Section(boolean success, int responseCode, int count, List<Candidate> candidates) {
    }

    /**
     * @param service a service
     * @return its results, if OpenSRS returned that section
     */
    public Optional<Section> section(Service service) {
        return Optional.ofNullable(sections.get(service));
    }

    /**
     * @return every available candidate across all sections, in section order
     */
    public List<Candidate> available() {
        List<Candidate> out = new ArrayList<>();
        sections.values().forEach(s -> s.candidates().stream().filter(Candidate::available).forEach(out::add));
        return Collections.unmodifiableList(out);
    }

    static NameSuggestResult from(XcpResponse r) {
        XcpData top = r.getEnvelope();
        XcpData a = r.getAttributes();
        Map<Service, Section> sections = new EnumMap<>(Service.class);
        for (Service s : Service.values()) {
            XcpData sec = a.getData(s.key());
            if (sec.isEmpty()) {
                continue;
            }
            List<Candidate> items = new ArrayList<>();
            for (XcpData i : sec.getDataList("items")) {
                items.add(new Candidate(i.getString("domain").orElse(""),
                    Status.of(i.getString("status").orElse("")),
                    i.getDecimal("price").orElse(null),
                    i.getString("third_party_offer_url").orElse(null)));
            }
            sections.put(s, new Section(sec.getFlag("is_success").orElse(false),
                sec.getInt("response_code").orElse(-1),
                sec.getInt("count").orElse(items.size()), List.copyOf(items)));
        }
        boolean completed = top.getFlag("is_search_completed")
            .or(() -> a.getFlag("is_search_completed")).orElse(true);
        String key = top.getString("search_key").or(() -> a.getString("search_key")).orElse(null);
        return new NameSuggestResult(completed, key, Collections.unmodifiableMap(sections), r);
    }
}
