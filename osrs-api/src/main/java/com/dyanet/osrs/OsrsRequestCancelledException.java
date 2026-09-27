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

/**
 * Thrown (through its future) for a request that was cancelled before being sent, because an
 * earlier request in the same {@link OsrsSession} failed. Nothing was sent to OpenSRS for it,
 * so it is safe to send again once the earlier problem is fixed.
 */
public class OsrsRequestCancelledException extends OsrsException {

    private static final long serialVersionUID = 1L;

    private final transient QueueFlush flush;

    OsrsRequestCancelledException(String request, QueueFlush flush) {
        super("\"" + request + "\" was not sent: an earlier request, \"" + flush.failedRequest()
            + "\", failed, so every request queued after it was cancelled to keep your account"
            + " consistent. Nothing was sent to OpenSRS for this one; send it again once the"
            + " earlier problem is fixed.", flush.failure());
        this.flush = flush;
    }

    OsrsRequestCancelledException(String request, String session) {
        super("\"" + request + "\" was not sent because the session \"" + session
            + "\" was closed first. Nothing was sent to OpenSRS for it.");
        this.flush = null;
    }

    /**
     * @return the failure that caused the cancellation, or {@code null} if the session was closed
     */
    public QueueFlush getFlush() {
        return flush;
    }
}
