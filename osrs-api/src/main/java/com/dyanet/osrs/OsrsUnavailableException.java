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

import java.util.Set;

import com.dyanet.osrs.xcp.XcpResponse;

/**
 * OpenSRS or the registry behind it could not handle the command right now; sending it again
 * later may succeed. Recognised codes: {@code 310} (too many simultaneous connections),
 * {@code 350} (too many commands on one connection), and the registry communication errors
 * {@code 702}, {@code 703}, {@code 704}, {@code 705} and {@code 720} ("Supplier Unavailable").
 *
 * <p>The client never retries these by itself: only the caller knows whether repeating a
 * command (a registration, say) is safe.
 */
public class OsrsUnavailableException extends OsrsApiException {

    private static final long serialVersionUID = 1L;

    /** The response codes this exception covers. */
    public static final Set<Integer> CODES = Set.of(310, 350, 702, 703, 704, 705, 720);

    /**
     * @param response the failed reply
     */
    public OsrsUnavailableException(XcpResponse response) {
        super(response);
    }
}
