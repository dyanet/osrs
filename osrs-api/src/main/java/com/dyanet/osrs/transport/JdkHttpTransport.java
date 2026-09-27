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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * {@link Transport} on the JDK's {@link HttpClient}: HTTP/1.1, TLS, connection pooling, no
 * third-party dependencies.
 */
public final class JdkHttpTransport implements Transport {

    private final HttpClient http;

    /**
     * @param connectTimeout TCP/TLS connect timeout
     */
    public JdkHttpTransport(Duration connectTimeout) {
        this(HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(connectTimeout)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build());
    }

    /**
     * @param http a preconfigured client (proxy, SSL context, executor...)
     */
    public JdkHttpTransport(HttpClient http) {
        this.http = http;
    }

    @Override
    public Reply post(URI uri, Map<String, String> headers, byte[] body, Duration timeout)
            throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder(uri)
            .timeout(timeout)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body));
        headers.forEach(b::header);
        HttpResponse<String> resp = http.send(b.build(),
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new Reply(resp.statusCode(), resp.body());
    }

    @Override
    public void close() {
        http.close();
    }
}
