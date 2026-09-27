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

import java.util.Map;

/**
 * A decoded XCP reply.
 *
 * <p>{@code is_success} only says whether OpenSRS could process the command. Some commands
 * report their answer in the {@code response_code} of a successful reply (a domain lookup
 * returns {@code 210} for available and {@code 211} for taken, both with {@code is_success=1}),
 * so check the code as well.
 */
public final class XcpResponse {

    private final XcpRequest request;
    private final XcpData envelope;
    private final String rawXml;

    XcpResponse(XcpRequest request, Map<String, Object> envelope, String rawXml) {
        this.request = request;
        this.envelope = XcpData.of(envelope);
        this.rawXml = rawXml;
    }

    /**
     * @return the request this answers, or {@code null} if the reply was decoded on its own
     */
    public XcpRequest getRequest() {
        return request;
    }

    /**
     * @return {@code true} if {@code is_success} is {@code 1}
     */
    public boolean isSuccess() {
        return envelope.getFlag("is_success").orElse(false);
    }

    /**
     * @return the {@code response_code}, or {@code -1} if absent or not numeric
     */
    public int getResponseCode() {
        return envelope.getInt("response_code").orElse(-1);
    }

    /**
     * @return the {@code response_text}, or an empty string
     */
    public String getResponseText() {
        return envelope.getString("response_text").orElse("");
    }

    /**
     * @return the reply's {@code object}, falling back to the request's
     */
    public String getObject() {
        return envelope.getString("object")
            .orElse(request == null ? "" : request.getObject());
    }

    /**
     * @return the reply's {@code action} (normally {@code REPLY})
     */
    public String getAction() {
        return envelope.getString("action").orElse("");
    }

    /**
     * @return the action of the request this answers, or the reply's own action
     */
    public String getRequestAction() {
        return request != null ? request.getAction() : getAction();
    }

    /**
     * @return the {@code protocol} (normally {@code XCP})
     */
    public String getProtocol() {
        return envelope.getString("protocol").orElse("");
    }

    /**
     * @return the {@code attributes} {@code dt_assoc}; empty if the reply had none
     */
    public XcpData getAttributes() {
        return envelope.getData("attributes");
    }

    /**
     * @return every top-level item of the reply, including {@code attributes}
     */
    public XcpData getEnvelope() {
        return envelope;
    }

    /**
     * The reply exactly as received. It can contain personal data and, for some commands,
     * domain authorisation codes, so treat it like a secret.
     *
     * @return the raw XML
     */
    public String getRawXml() {
        return rawXml;
    }

    @Override
    public String toString() {
        return "XcpResponse[" + getObject() + " " + getRequestAction() + " success=" + isSuccess()
            + " code=" + getResponseCode() + " text=" + getResponseText() + "]";
    }
}
