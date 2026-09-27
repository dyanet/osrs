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

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Filters for {@code GET_DELETED_DOMAINS}. Every filter is optional; email filters accept
 * wildcards. OpenSRS only accepts years up to 2030, so later dates are clamped to 2030-12-31.
 *
 * <pre>{@code
 * DeletedDomainsQuery q = DeletedDomainsQuery.builder()
 *     .deletedBetween(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 9, 30))
 *     .limit(100).page(1).build();
 * }</pre>
 */
public final class DeletedDomainsQuery {

    /** The last date OpenSRS accepts in a date filter. */
    public static final LocalDate LAST_DATE = LocalDate.of(2030, 12, 31);

    private final Map<String, Object> attributes;

    private DeletedDomainsQuery(Map<String, Object> attributes) {
        this.attributes = Map.copyOf(attributes);
    }

    /**
     * @return a builder with no filters
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return a query with no filters (first page, OpenSRS's default page size)
     */
    public static DeletedDomainsQuery all() {
        return builder().build();
    }

    Map<String, Object> attributes() {
        return attributes;
    }

    /** Builds a {@link DeletedDomainsQuery}. */
    public static final class Builder {
        private final Map<String, Object> a = new LinkedHashMap<>();

        private Builder() {
        }

        private Builder put(String k, Object v) {
            if (v == null || (v instanceof String s && s.isBlank())) {
                a.remove(k);
            } else {
                a.put(k, v);
            }
            return this;
        }

        private static LocalDate clamp(LocalDate d) {
            return d == null || !d.isAfter(LAST_DATE) ? d : LAST_DATE;
        }

        /**
         * @param domain only this domain
         * @return this builder
         */
        public Builder domain(String domain) {
            return put("domain", domain);
        }

        /**
         * @param email owner contact email (wildcards allowed)
         * @return this builder
         */
        public Builder ownerEmail(String email) {
            return put("owner_email", email);
        }

        /**
         * @param email admin contact email (wildcards allowed)
         * @return this builder
         */
        public Builder adminEmail(String email) {
            return put("admin_email", email);
        }

        /**
         * @param email billing contact email (wildcards allowed)
         * @return this builder
         */
        public Builder billingEmail(String email) {
            return put("billing_email", email);
        }

        /**
         * @param email tech contact email (wildcards allowed)
         * @return this builder
         */
        public Builder techEmail(String email) {
            return put("tech_email", email);
        }

        /**
         * @param from first deletion date, inclusive ({@code del_from})
         * @param to   last deletion date ({@code del_to})
         * @return this builder
         */
        public Builder deletedBetween(LocalDate from, LocalDate to) {
            return put("del_from", clamp(from)).put("del_to", clamp(to));
        }

        /**
         * @param from first expiry date ({@code exp_from})
         * @param to   last expiry date ({@code exp_to})
         * @return this builder
         */
        public Builder expiredBetween(LocalDate from, LocalDate to) {
            return put("exp_from", clamp(from)).put("exp_to", clamp(to));
        }

        /**
         * @param limit results per page (OpenSRS default 40)
         * @return this builder
         */
        public Builder limit(int limit) {
            if (limit < 1) {
                throw new IllegalArgumentException("limit must be positive");
            }
            return put("limit", limit);
        }

        /**
         * @param page the page to fetch, as OpenSRS numbers pages
         * @return this builder
         */
        public Builder page(int page) {
            if (page < 0) {
                throw new IllegalArgumentException("page must not be negative");
            }
            return put("page", page);
        }

        /**
         * @return the query
         */
        public DeletedDomainsQuery build() {
            return new DeletedDomainsQuery(a);
        }
    }
}
