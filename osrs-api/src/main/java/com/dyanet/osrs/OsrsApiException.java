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
 * OpenSRS processed the request and reported a failure ({@code is_success=0}).
 *
 * <p>Modules for specific command families subclass this for the response codes they know
 * about; anything not recognised surfaces as this class. The full reply stays available
 * through {@link #getResponse()}.
 */
public class OsrsApiException extends OsrsException {

    private static final long serialVersionUID = 1L;

    private final transient XcpResponse response;

    /**
     * @param response the failed reply
     */
    public OsrsApiException(XcpResponse response) {
        this(describe(response), response);
    }

    /**
     * @param message  what went wrong
     * @param response the failed reply
     */
    public OsrsApiException(String message, XcpResponse response) {
        super(message);
        this.response = response;
    }

    /**
     * @return the OpenSRS {@code response_code}, or {@code -1} if the reply had none
     */
    public int getResponseCode() {
        return response == null ? -1 : response.getResponseCode();
    }

    /**
     * @return the OpenSRS {@code response_text}, possibly empty
     */
    public String getResponseText() {
        return response == null ? "" : response.getResponseText();
    }

    /**
     * @return the reply that reported the failure
     */
    public XcpResponse getResponse() {
        return response;
    }

    static String describe(XcpResponse r) {
        return "OpenSRS " + r.getObject() + " " + r.getRequestAction() + " failed: "
            + r.getResponseCode() + " " + r.getResponseText();
    }
}
