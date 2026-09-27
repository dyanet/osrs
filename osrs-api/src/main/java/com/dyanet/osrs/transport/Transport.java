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

package com.dyanet.osrs.transport;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Map;

/**
 * Sends one signed HTTP POST and returns the reply. The default is {@link JdkHttpTransport};
 * tests use {@link StubTransport}. Implementations must be thread-safe.
 */
public interface Transport extends AutoCloseable {

    /**
     * @param uri     the endpoint
     * @param headers request headers (including {@code X-Signature}); never log them
     * @param body    the UTF-8 request body
     * @param timeout how long to wait for the whole reply
     * @return the HTTP status and body
     * @throws IOException          if no reply was received
     * @throws InterruptedException if the calling thread was interrupted
     */
    Reply post(URI uri, Map<String, String> headers, byte[] body, Duration timeout)
        throws IOException, InterruptedException;

    /** Releases connections. The default does nothing. */
    @Override
    default void close() {
    }

    /**
     * An HTTP reply.
     *
     * @param status the HTTP status code
     * @param body   the reply body decoded as UTF-8
     */
    record Reply(int status, String body) {
    }
}
