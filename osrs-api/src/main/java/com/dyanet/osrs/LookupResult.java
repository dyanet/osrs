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

import com.dyanet.osrs.xcp.XcpResponse;

/**
 * The answer to a {@code DOMAIN LOOKUP}.
 *
 * @param domain    the domain that was checked
 * @param available {@code true} for response code 210, {@code false} for 211
 * @param premium   whether OpenSRS flagged it as a premium name ({@code reason=Premium Name});
 *                  premium names cost more than the regular price
 * @param hasClaim  for new TLDs in their claims period, whether a trademark (TMCH) claim exists;
 *                  {@code null} when not reported
 * @param response  the full reply
 */
public record LookupResult(String domain, boolean available, boolean premium, Boolean hasClaim,
        XcpResponse response) {

    /** Response code: the domain is available. */
    public static final int CODE_AVAILABLE = 210;
    /** Response code: the domain is taken (still {@code is_success=1}). */
    public static final int CODE_TAKEN = 211;

    static LookupResult from(String domain, XcpResponse r) {
        boolean available;
        int code = r.getResponseCode();
        if (code == CODE_AVAILABLE) {
            available = true;
        } else if (code == CODE_TAKEN) {
            available = false;
        } else {
            String status = r.getAttributes().getString("status").orElse("");
            if ("available".equalsIgnoreCase(status)) {
                available = true;
            } else if ("taken".equalsIgnoreCase(status)) {
                available = false;
            } else {
                throw new OsrsApiException("OpenSRS DOMAIN LOOKUP for " + domain
                    + " gave no availability: " + code + " " + r.getResponseText(), r);
            }
        }
        boolean premium = r.getAttributes().getString("reason")
            .map(s -> s.toLowerCase().contains("premium")).orElse(false);
        Boolean claim = r.getAttributes().getFlag("has_claim").orElse(null);
        return new LookupResult(domain, available, premium, claim, r);
    }
}
