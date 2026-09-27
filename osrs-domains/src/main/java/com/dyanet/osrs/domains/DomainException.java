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

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import com.dyanet.osrs.OsrsApiException;
import com.dyanet.osrs.OsrsErrorMapper;
import com.dyanet.osrs.xcp.XcpResponse;

/**
 * A domain command failed for a reason OpenSRS reports with a known response code.
 * {@link #getReason()} says which; the raw code and text stay available.
 *
 * <p>Codes and meanings are from the OpenSRS response code table. Authentication and
 * "try again later" failures are reported by {@code osrs-api} as
 * {@link com.dyanet.osrs.OsrsAuthenticationException} and
 * {@link com.dyanet.osrs.OsrsUnavailableException} instead.
 */
public class DomainException extends OsrsApiException {

    private static final long serialVersionUID = 1L;

    /** Why a domain command failed. */
    public enum Reason {
        /** 440: over quota; not enough funds in the reseller account. */
        INSUFFICIENT_FUNDS(440),
        /** 480: the domain isn't owned by this reseller (or the sub-user doesn't exist). */
        NOT_OWNED(480),
        /** 485: the domain is taken, or the nameserver is already mapped to a domain. */
        TAKEN(485),
        /** 437, 486: another request for this domain is already being processed. */
        IN_PROGRESS(437, 486),
        /** 487: the domain can't be transferred. */
        NOT_TRANSFERABLE(487),
        /** 541: the current expiry year given doesn't match the registry's. */
        EXPIRY_YEAR_MISMATCH(541),
        /** 552: the domain is less than 60 days old. */
        TOO_NEW(552),
        /** 555 (without an IP-address message): the domain was already renewed. */
        ALREADY_RENEWED(555),
        /** 701: no TLD could be extracted, or OpenSRS doesn't serve the TLD. */
        TLD_NOT_SUPPORTED(701),
        /** 420, 460, 465, 470, 599: a required field is missing or a value is invalid. */
        INVALID_DATA(420, 460, 465, 470, 599);

        private final Set<Integer> codes;

        Reason(Integer... codes) {
            this.codes = Set.of(codes);
        }

        /**
         * @return the response codes that mean this
         */
        public Set<Integer> codes() {
            return codes;
        }

        static Optional<Reason> of(int code) {
            return Arrays.stream(values()).filter(r -> r.codes.contains(code)).findFirst();
        }
    }

    /**
     * Maps failed replies of domain commands to {@link DomainException}. It runs before the
     * built-in mappers of {@code osrs-api}, so it leaves {@code 555} alone when the text is
     * about an IP address.
     */
    public static final OsrsErrorMapper MAPPER = r -> {
        if (r.getResponseCode() == 555
                && r.getResponseText().toLowerCase(java.util.Locale.ROOT).contains("ip address")) {
            return null;
        }
        return Reason.of(r.getResponseCode()).map(reason -> new DomainException(reason, r)).orElse(null);
    };

    private final Reason reason;

    /**
     * @param reason   why the command failed
     * @param response the failed reply
     */
    public DomainException(Reason reason, XcpResponse response) {
        super(response);
        this.reason = reason;
    }

    /**
     * @return why the command failed
     */
    public Reason getReason() {
        return reason;
    }
}
