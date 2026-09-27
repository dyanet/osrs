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
 * The server replied, but the reply was not a well-formed XCP {@code OPS_envelope}
 * (for example an HTML error page from a proxy).
 */
public class OsrsProtocolException extends OsrsException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message what went wrong
     * @param cause   the underlying failure, may be {@code null}
     */
    public OsrsProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
}
