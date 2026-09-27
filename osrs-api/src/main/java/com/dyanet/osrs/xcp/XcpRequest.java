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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * One XCP command: an {@code object}, an {@code action}, and its {@code attributes}.
 *
 * <p>Attribute values may be nested: a {@code Map<String, ?>} is sent as a {@code dt_assoc}, a
 * {@code List}/{@code Collection}/array as a {@code dt_array}; see {@link XcpCodec} for all the
 * accepted types. {@code null} values are left out.
 *
 * <pre>{@code
 * XcpRequest req = XcpRequest.builder("DOMAIN", "SW_REGISTER")
 *     .attribute("domain", "example.com")
 *     .attribute("contact_set", Map.of("owner", owner, "admin", owner))
 *     .attribute("nameserver_list", List.of(Map.of("name", "ns1.example.net", "sortorder", 1)))
 *     .build();
 * }</pre>
 *
 * <p>{@link #toString()} shows the attribute names only, never their values.
 */
public final class XcpRequest {

    private final String object;
    private final String action;
    private final Map<String, Object> attributes;
    private final String registrantIp;
    private final boolean idempotent;

    private XcpRequest(Builder b) {
        this.object = b.object;
        this.action = b.action;
        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<>(b.attributes));
        this.registrantIp = b.registrantIp;
        this.idempotent = b.idempotent;
    }

    /**
     * @param object the XCP object, e.g. {@code DOMAIN}
     * @param action the XCP action, e.g. {@code LOOKUP}
     * @return a builder
     */
    public static Builder builder(String object, String action) {
        return new Builder(object, action);
    }

    /**
     * @return the XCP object
     */
    public String getObject() {
        return object;
    }

    /**
     * @return the XCP action
     */
    public String getAction() {
        return action;
    }

    /**
     * @return the attributes, in insertion order
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * @return the optional top-level {@code registrant_ip}, or {@code null}
     */
    public String getRegistrantIp() {
        return registrantIp;
    }

    /**
     * @return whether sending this request twice is harmless (reads); only these are retried
     */
    public boolean isIdempotent() {
        return idempotent;
    }

    @Override
    public String toString() {
        return "XcpRequest[" + object + " " + action + " " + attributes.keySet() + "]";
    }

    /** Builds an {@link XcpRequest}. */
    public static final class Builder {
        private final String object;
        private final String action;
        private final Map<String, Object> attributes = new LinkedHashMap<>();
        private String registrantIp;
        private boolean idempotent;

        private Builder(String object, String action) {
            this.object = requireText(object, "object");
            this.action = requireText(action, "action");
        }

        /**
         * @param name  attribute name
         * @param value attribute value; {@code null} removes it
         * @return this builder
         */
        public Builder attribute(String name, Object value) {
            requireText(name, "attribute name");
            if (value == null) {
                attributes.remove(name);
            } else {
                attributes.put(name, value);
            }
            return this;
        }

        /**
         * @param values attributes to add; {@code null} values are skipped
         * @return this builder
         */
        public Builder attributes(Map<String, ?> values) {
            values.forEach(this::attribute);
            return this;
        }

        /**
         * @param ip the end customer's IP address, sent as the top-level {@code registrant_ip}
         * @return this builder
         */
        public Builder registrantIp(String ip) {
            this.registrantIp = ip == null || ip.isBlank() ? null : ip;
            return this;
        }

        /**
         * Marks the request as safe to send twice, so a {@link com.dyanet.osrs.RetryPolicy}
         * may retry it. Never set this on commands that register, renew, transfer or charge.
         *
         * @param idempotent whether the request is a read
         * @return this builder
         */
        public Builder idempotent(boolean idempotent) {
            this.idempotent = idempotent;
            return this;
        }

        /**
         * @return the request
         */
        public XcpRequest build() {
            return new XcpRequest(this);
        }

        private static String requireText(String s, String what) {
            if (Objects.requireNonNull(s, what).isBlank()) {
                throw new IllegalArgumentException(what + " must not be blank");
            }
            return s;
        }
    }
}
