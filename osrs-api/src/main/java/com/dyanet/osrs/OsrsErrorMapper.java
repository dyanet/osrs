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
 * Turns a failed reply into a specific exception. Command-family modules supply mappers for
 * the response codes they understand; {@link OsrsClient#execute(com.dyanet.osrs.xcp.XcpRequest,
 * OsrsErrorMapper)} consults them before falling back to {@link OsrsApiException}.
 */
@FunctionalInterface
public interface OsrsErrorMapper {

    /**
     * @param failed a reply with {@code is_success=0}
     * @return the exception to throw, or {@code null} if this mapper doesn't recognise it
     */
    OsrsApiException map(XcpResponse failed);

    /**
     * @return a mapper that recognises nothing
     */
    static OsrsErrorMapper none() {
        return r -> null;
    }

    /**
     * @param next the mapper to consult if this one returns {@code null}
     * @return a mapper trying this one first, then {@code next}
     */
    default OsrsErrorMapper orElse(OsrsErrorMapper next) {
        return r -> {
            OsrsApiException e = map(r);
            return e != null ? e : next.map(r);
        };
    }
}
