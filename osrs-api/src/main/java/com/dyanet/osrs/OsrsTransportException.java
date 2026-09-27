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
 * The request could not be delivered, or the server answered with an HTTP error that did not
 * carry an {@code OPS_envelope}: connection refused, TLS or DNS failure, timeout, or an HTTP
 * status other than 2xx.
 *
 * <p>These failures are the ones a {@link RetryPolicy} may retry.
 */
public class OsrsTransportException extends OsrsException {

    private static final long serialVersionUID = 1L;

    private final int httpStatus;

    /**
     * @param message    what went wrong
     * @param httpStatus the HTTP status, or {@code -1} if no HTTP reply was received
     * @param cause      the underlying failure, may be {@code null}
     */
    public OsrsTransportException(String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    /**
     * @return the HTTP status, or {@code -1} if no HTTP reply was received
     */
    public int getHttpStatus() {
        return httpStatus;
    }
}
