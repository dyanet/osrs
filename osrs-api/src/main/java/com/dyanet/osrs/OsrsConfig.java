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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;

/**
 * Connection settings for one OpenSRS reseller account. Immutable and thread-safe.
 *
 * <pre>{@code
 * OsrsConfig test = OsrsConfig.test("myreseller", apiKey);          // horizon.opensrs.net
 * OsrsConfig live = OsrsConfig.live("myreseller", apiKey);          // rr-n1-tor.opensrs.net
 * OsrsConfig file = OsrsConfig.load();                              // from a properties file
 * }</pre>
 *
 * <p>The API key is never included in {@link #toString()}.
 */
public final class OsrsConfig {

    /** The OpenSRS test ("horizon") environment. No IP allowlist. */
    public static final String TEST_HOST = "horizon.opensrs.net";
    /** The OpenSRS live environment. Requires the caller's IP on the reseller allowlist. */
    public static final String LIVE_HOST = "rr-n1-tor.opensrs.net";
    /** The HTTPS port of the XML API. */
    public static final int DEFAULT_PORT = 55443;

    /** System property naming a config file (path or classpath resource). */
    public static final String CONFIG_PROPERTY = "osrs.config";
    /** Environment variable naming a config file, used when {@link #CONFIG_PROPERTY} is unset. */
    public static final String CONFIG_ENV_VAR = "OSRS_CONFIG";
    /** System property selecting {@code osrs-<env>.properties} on the classpath. */
    public static final String ENV_PROPERTY = "osrs.env";
    /** Environment used when {@link #ENV_PROPERTY} is unset. */
    public static final String DEFAULT_ENV = "test";

    /** Environment lookup; replaced in tests. */
    static Function<String, String> envLookup = System::getenv;

    private final URI endpoint;
    private final String username;
    private final String apiKey;
    private final Duration connectTimeout;
    private final Duration requestTimeout;

    private OsrsConfig(Builder b) {
        this.endpoint = b.endpoint != null ? b.endpoint
            : URI.create("https://" + requireText(b.host, "host") + ":" + b.port + "/");
        this.username = requireText(b.username, "username");
        this.apiKey = requireText(b.apiKey, "apiKey");
        this.connectTimeout = Objects.requireNonNull(b.connectTimeout, "connectTimeout");
        this.requestTimeout = Objects.requireNonNull(b.requestTimeout, "requestTimeout");
    }

    /**
     * @return a builder with the default port and timeouts
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @param username the reseller username
     * @param apiKey   the test environment's API key
     * @return settings for {@link #TEST_HOST}
     */
    public static OsrsConfig test(String username, String apiKey) {
        return builder().host(TEST_HOST).username(username).apiKey(apiKey).build();
    }

    /**
     * @param username the reseller username
     * @param apiKey   the live environment's API key (different from the test key)
     * @return settings for {@link #LIVE_HOST}
     */
    public static OsrsConfig live(String username, String apiKey) {
        return builder().host(LIVE_HOST).username(username).apiKey(apiKey).build();
    }

    /**
     * Reads settings from properties:
     * <ul>
     *   <li>{@code osrs.host} (required), {@code osrs.sslPort} (default {@value #DEFAULT_PORT});</li>
     *   <li>{@code osrs.userName} and {@code osrs.key} (required);</li>
     *   <li>{@code osrs.connectTimeoutMs}, {@code osrs.requestTimeoutMs} (optional).</li>
     * </ul>
     * Other keys are ignored, so files written for earlier versions still load.
     *
     * @param p the properties
     * @return the settings
     * @throws OsrsException if a required key is missing or a number is invalid
     */
    public static OsrsConfig fromProperties(Properties p) {
        try {
            Builder b = builder()
                .host(p.getProperty("osrs.host"))
                .username(p.getProperty("osrs.userName"))
                .apiKey(p.getProperty("osrs.key"));
            String port = p.getProperty("osrs.sslPort");
            if (port != null && !port.isBlank()) {
                b.port(Integer.parseInt(port.trim()));
            }
            String ct = p.getProperty("osrs.connectTimeoutMs");
            if (ct != null && !ct.isBlank()) {
                b.connectTimeout(Duration.ofMillis(Long.parseLong(ct.trim())));
            }
            String rt = p.getProperty("osrs.requestTimeoutMs");
            if (rt != null && !rt.isBlank()) {
                b.requestTimeout(Duration.ofMillis(Long.parseLong(rt.trim())));
            }
            return b.build();
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new OsrsException("Invalid OpenSRS configuration: " + e.getMessage(), e);
        }
    }

    /**
     * Loads settings from the first of:
     * <ol>
     *   <li>the system property {@value #CONFIG_PROPERTY} (a file path or classpath resource);</li>
     *   <li>the environment variable {@value #CONFIG_ENV_VAR} (same);</li>
     *   <li>{@code osrs-<env>.properties} on the classpath, {@code <env>} being the system
     *       property {@value #ENV_PROPERTY} (default {@value #DEFAULT_ENV}).</li>
     * </ol>
     * Templates are in the project's {@code config/} directory.
     *
     * @return the settings
     * @throws OsrsException if no file is found or it is invalid
     */
    public static OsrsConfig load() {
        String resource = resolveResource();
        Properties p = new Properties();
        try (InputStream in = open(resource)) {
            if (in == null) {
                throw new OsrsException("OpenSRS configuration not found: " + resource
                    + " (set -D" + CONFIG_PROPERTY + " or " + CONFIG_ENV_VAR + ")");
            }
            p.load(in);
        } catch (IOException e) {
            throw new OsrsException("Could not read OpenSRS configuration " + resource, e);
        }
        try {
            return fromProperties(p);
        } catch (OsrsException e) {
            throw new OsrsException(e.getMessage() + " in " + resource, e);
        }
    }

    static String resolveResource() {
        String sys = System.getProperty(CONFIG_PROPERTY);
        if (sys != null && !sys.isBlank()) {
            return sys;
        }
        String env = envLookup.apply(CONFIG_ENV_VAR);
        if (env != null && !env.isBlank()) {
            return env;
        }
        return "osrs-" + System.getProperty(ENV_PROPERTY, DEFAULT_ENV) + ".properties";
    }

    private static InputStream open(String resource) throws IOException {
        Path file = Path.of(resource);
        if (Files.isRegularFile(file)) {
            return Files.newInputStream(file);
        }
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        InputStream in = cl == null ? null : cl.getResourceAsStream(resource);
        return in != null ? in : OsrsConfig.class.getClassLoader().getResourceAsStream(resource);
    }

    private static String requireText(String s, String what) {
        if (s == null || s.isBlank()) {
            throw new IllegalArgumentException(what + " is required");
        }
        return s.trim();
    }

    /**
     * @return the XML API endpoint
     */
    public URI getEndpoint() {
        return endpoint;
    }

    /**
     * @return the reseller username, sent as {@code X-Username}
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return the private API key used to sign requests
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * @return the connect timeout
     */
    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * @return the timeout for a whole request
     */
    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    @Override
    public String toString() {
        return "OsrsConfig[" + endpoint + " user=" + username + " key=****]";
    }

    /** Builds an {@link OsrsConfig}. */
    public static final class Builder {
        private String host;
        private int port = DEFAULT_PORT;
        private URI endpoint;
        private String username;
        private String apiKey;
        private Duration connectTimeout = Duration.ofSeconds(30);
        private Duration requestTimeout = Duration.ofSeconds(120);

        private Builder() {
        }

        /**
         * @param host e.g. {@link #TEST_HOST} or {@link #LIVE_HOST}
         * @return this builder
         */
        public Builder host(String host) {
            this.host = host;
            return this;
        }

        /**
         * @param port the HTTPS port, default {@value #DEFAULT_PORT}
         * @return this builder
         */
        public Builder port(int port) {
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("port out of range: " + port);
            }
            this.port = port;
            return this;
        }

        /**
         * Overrides host and port with a full URI, e.g. for a proxy or a local test server.
         *
         * @param endpoint the endpoint
         * @return this builder
         */
        public Builder endpoint(URI endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        /**
         * @param username the reseller username
         * @return this builder
         */
        public Builder username(String username) {
            this.username = username;
            return this;
        }

        /**
         * @param apiKey the private API key (test and live keys differ)
         * @return this builder
         */
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /**
         * @param timeout the connect timeout, default 30 s
         * @return this builder
         */
        public Builder connectTimeout(Duration timeout) {
            this.connectTimeout = timeout;
            return this;
        }

        /**
         * @param timeout the timeout for a whole request, default 120 s
         * @return this builder
         */
        public Builder requestTimeout(Duration timeout) {
            this.requestTimeout = timeout;
            return this;
        }

        /**
         * @return the settings
         * @throws IllegalArgumentException if host, username or API key is missing
         */
        public OsrsConfig build() {
            return new OsrsConfig(this);
        }
    }
}
