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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A domain search ({@code DOMAIN NAME_SUGGEST}): availability of a name across TLDs, plus
 * suggestions and premium names.
 *
 * <pre>{@code
 * NameSuggestQuery q = NameSuggestQuery.of("example", ".com", ".net")
 *     .services(Service.LOOKUP, Service.SUGGESTION)
 *     .maxWaitTime(Duration.ofSeconds(3));
 * }</pre>
 */
public final class NameSuggestQuery {

    /** The kinds of result OpenSRS can search for; sent in {@code services}. */
    public enum Service {
        /** Exact-match availability in each TLD. */
        LOOKUP,
        /** Suggested alternative names. */
        SUGGESTION,
        /** Premium names for sale. */
        PREMIUM,
        /** Premium names available through a brokered transfer. */
        PREMIUM_BROKERED_TRANSFER,
        /** Premium names whose owner accepts offers. */
        PREMIUM_MAKE_OFFER,
        /** Personal names (.NAME-style) results. */
        PERSONAL_NAMES;

        /**
         * @return the attribute key OpenSRS uses for this service
         */
        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private final String searchString;
    private final List<String> tlds;
    private Set<Service> services = EnumSet.noneOf(Service.class);
    private final List<String> languages = new ArrayList<>();
    private Duration maxWaitTime;
    private String searchKey;
    private boolean skipRegistryLookup;
    private final Map<String, Map<String, ?>> overrides = new LinkedHashMap<>();

    private NameSuggestQuery(String searchString, List<String> tlds) {
        if (Objects.requireNonNull(searchString, "searchString").isBlank()) {
            throw new IllegalArgumentException("searchString must not be blank");
        }
        if (tlds.isEmpty()) {
            throw new IllegalArgumentException("at least one TLD is required");
        }
        this.searchString = searchString;
        this.tlds = List.copyOf(tlds);
    }

    /**
     * @param searchString the name, word or phrase to search for
     * @param tlds         TLDs to check, e.g. {@code ".com"} (at least one)
     * @return the query, with OpenSRS's default services
     */
    public static NameSuggestQuery of(String searchString, String... tlds) {
        return new NameSuggestQuery(searchString, List.of(tlds));
    }

    /**
     * @param services the kinds of result wanted; none means OpenSRS's default
     * @return this query
     */
    public NameSuggestQuery services(Service... services) {
        this.services = services.length == 0 ? EnumSet.noneOf(Service.class) : EnumSet.of(services[0], services);
        return this;
    }

    /**
     * @param codes suggestion languages ({@code en}, {@code fr}, {@code de}, {@code it}, {@code es})
     * @return this query
     */
    public NameSuggestQuery languages(String... codes) {
        languages.clear();
        languages.addAll(List.of(codes));
        return this;
    }

    /**
     * @param wait how long OpenSRS may spend (at least 0.1 s); partial results come back with a
     *             {@linkplain NameSuggestResult#searchKey() search key} to continue
     * @return this query
     */
    public NameSuggestQuery maxWaitTime(Duration wait) {
        if (wait.toMillis() < 100) {
            throw new IllegalArgumentException("maxWaitTime must be at least 0.1 s");
        }
        this.maxWaitTime = wait;
        return this;
    }

    /**
     * @param key the search key of an incomplete earlier result, to continue that search
     * @return this query
     */
    public NameSuggestQuery searchKey(String key) {
        this.searchKey = key;
        return this;
    }

    /**
     * @param skip {@code true} to skip the registry check (faster, less accurate)
     * @return this query
     */
    public NameSuggestQuery skipRegistryLookup(boolean skip) {
        this.skipRegistryLookup = skip;
        return this;
    }

    /**
     * Overrides the defaults of one service, e.g. {@code override(SUGGESTION, Map.of("maximum", 20))}
     * or {@code override(PREMIUM, Map.of("price_max", 500))}. Keys are those of
     * {@code service_override} in the OpenSRS documentation.
     *
     * @param service  the service
     * @param settings its settings
     * @return this query
     */
    public NameSuggestQuery override(Service service, Map<String, ?> settings) {
        overrides.put(service.key(), Map.copyOf(settings));
        return this;
    }

    Map<String, Object> attributes() {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("searchstring", searchString);
        a.put("tlds", tlds);
        if (!services.isEmpty()) {
            a.put("services", services.stream().map(Service::key).toList());
        }
        if (!languages.isEmpty()) {
            a.put("languages", List.copyOf(languages));
        }
        if (maxWaitTime != null) {
            a.put("max_wait_time", java.math.BigDecimal.valueOf(maxWaitTime.toMillis(), 3).stripTrailingZeros());
        }
        if (searchKey != null && !searchKey.isBlank()) {
            a.put("search_key", searchKey);
        }
        if (skipRegistryLookup) {
            a.put("skip_registry_lookup", 1);
        }
        if (!overrides.isEmpty()) {
            a.put("service_override", Collections.unmodifiableMap(new LinkedHashMap<>(overrides)));
        }
        return a;
    }
}
