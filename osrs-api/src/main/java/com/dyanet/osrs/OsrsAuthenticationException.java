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
 * OpenSRS rejected the reseller credentials or the caller's IP address.
 *
 * <p>Recognised replies: {@code 410} ("Reseller authentication error"), {@code 401}
 * ("Authentication Error": wrong username, API key or signature; the test and live keys
 * differ), and {@code 400} or {@code 555} when the text says the IP address is invalid (the
 * calling address isn't on the reseller's allowlist; changes take up to 15 minutes). Code
 * {@code 555} without that text means something else ("Domain has already been successfully
 * renewed") and is not treated as an authentication error.
 */
public class OsrsAuthenticationException extends OsrsApiException {

    private static final long serialVersionUID = 1L;

    /**
     * @param response the failed reply
     */
    public OsrsAuthenticationException(XcpResponse response) {
        super(response);
    }

    /**
     * @param r a failed reply
     * @return whether it reports an authentication or IP-allowlist failure
     */
    public static boolean matches(XcpResponse r) {
        int code = r.getResponseCode();
        if (code == 410 || code == 401) {
            return true;
        }
        String text = r.getResponseText().toLowerCase(java.util.Locale.ROOT);
        return (code == 400 || code == 555) && text.contains("ip address");
    }
}
