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

import java.util.List;

/**
 * What happened when a request in an {@link OsrsSession} failed and the requests queued after
 * it were cancelled.
 *
 * @param session        the session's name
 * @param failedRequest  a short description of the request that failed
 * @param failure        why it failed
 * @param cancelled      descriptions of the requests that were removed, in queue order; none
 *                       of them was sent to OpenSRS
 */
public record QueueFlush(String session, String failedRequest, Throwable failure, List<String> cancelled) {

    /**
     * A plain-language explanation, suitable for showing to an end user.
     *
     * @return the message
     */
    public String message() {
        StringBuilder sb = new StringBuilder()
            .append("The request \"").append(failedRequest).append("\" did not go through")
            .append(reason(failure)).append('.');
        if (cancelled.isEmpty()) {
            sb.append(" No other requests were waiting, so nothing else was affected.");
        } else {
            int n = cancelled.size();
            sb.append(" To keep your account consistent, ")
                .append(n == 1 ? "the 1 request" : "all " + n + " requests")
                .append(" waiting after it ").append(n == 1 ? "was" : "were")
                .append(" cancelled and not sent: ")
                .append(String.join("; ", cancelled))
                .append(". Fix the problem, then send ").append(n == 1 ? "it" : "them").append(" again.");
        }
        return sb.toString();
    }

    static String reason(Throwable t) {
        if (t instanceof OsrsApiException api && api.getResponse() != null) {
            String text = api.getResponseText();
            return " (OpenSRS said: " + (text.isBlank() ? "error " + api.getResponseCode()
                : text + ", code " + api.getResponseCode()) + ")";
        }
        if (t instanceof OsrsTransportException) {
            return " (OpenSRS could not be reached or did not answer)";
        }
        if (t instanceof OsrsProtocolException) {
            return " (OpenSRS sent a reply that could not be read)";
        }
        String m = t.getMessage();
        return m == null || m.isBlank() ? "" : " (" + m + ")";
    }
}
