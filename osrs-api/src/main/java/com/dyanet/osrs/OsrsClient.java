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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dyanet.osrs.transport.JdkHttpTransport;
import com.dyanet.osrs.transport.Transport;
import com.dyanet.osrs.xcp.OsrsSignature;
import com.dyanet.osrs.xcp.XcpCodec;
import com.dyanet.osrs.xcp.XcpRequest;
import com.dyanet.osrs.xcp.XcpResponse;

/**
 * Client for the OpenSRS reseller XML (XCP) API: it encodes, signs and sends commands and
 * decodes the replies. Command families (domains, transfers, DNS) are separate artifacts built
 * on this class; on its own it can send any {@link XcpRequest} and do a domain
 * {@linkplain #lookup(String) lookup}.
 *
 * <pre>{@code
 * try (OsrsClient client = OsrsClient.builder()
 *         .config(OsrsConfig.test("myreseller", apiKey))
 *         .build()) {
 *     LookupResult r = client.lookup("example.com");
 *     XcpResponse any = client.execute(XcpRequest.builder("DOMAIN", "GET_BALANCE").build());
 * }
 * }</pre>
 *
 * <p>Instances are thread-safe; create one per reseller account and share it. The API key and
 * signature are never logged; at DEBUG level each call logs its object, action, response code
 * and duration.
 */
public final class OsrsClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(OsrsClient.class);

    /** Built-in mapping of failures common to every command family. */
    static final OsrsErrorMapper BUILT_IN_ERRORS = r -> {
        if (OsrsAuthenticationException.matches(r)) {
            return new OsrsAuthenticationException(r);
        }
        if (OsrsUnavailableException.CODES.contains(r.getResponseCode())) {
            return new OsrsUnavailableException(r);
        }
        return null;
    };

    /** Sleeps between retries; replaced in tests. */
    interface Sleeper {
        void sleep(Duration d) throws InterruptedException;
    }

    private final OsrsConfig config;
    private final Transport transport;
    private final RetryPolicy retryPolicy;
    private final OsrsErrorMapper errorMapper;
    private final Sleeper sleeper;

    private OsrsClient(Builder b) {
        this.config = Objects.requireNonNull(b.config, "config");
        this.transport = b.transport != null ? b.transport
            : new JdkHttpTransport(config.getConnectTimeout());
        this.retryPolicy = b.retryPolicy;
        this.errorMapper = b.errorMapper;
        this.sleeper = b.sleeper;
    }

    /**
     * @return a builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return a client configured by {@link OsrsConfig#load()}
     */
    public static OsrsClient fromDefaultConfig() {
        return builder().config(OsrsConfig.load()).build();
    }

    /**
     * @return the settings this client uses
     */
    public OsrsConfig getConfig() {
        return config;
    }

    /**
     * Sends a command and returns the reply whatever its {@code is_success}.
     *
     * @param request the command
     * @return the decoded reply
     * @throws OsrsTransportException if no usable HTTP reply arrived (after any retries)
     * @throws OsrsProtocolException  if the reply isn't an {@code OPS_envelope}
     */
    public XcpResponse send(XcpRequest request) {
        String xml = XcpCodec.encodeRequest(request);
        long start = System.nanoTime();
        String reply = post(xml, request.isIdempotent() ? retryPolicy : RetryPolicy.none(),
            request.getObject() + " " + request.getAction());
        XcpResponse r = XcpCodec.decodeResponse(request, reply);
        if (log.isDebugEnabled()) {
            log.debug("OpenSRS {} {} -> {} {} ({} ms)", request.getObject(), request.getAction(),
                r.isSuccess() ? "ok" : "failed", r.getResponseCode(),
                (System.nanoTime() - start) / 1_000_000);
        }
        return r;
    }

    /**
     * Sends a command and throws if OpenSRS reports a failure.
     *
     * @param request the command
     * @return the successful reply
     * @throws OsrsApiException       if {@code is_success} is 0 (or a subclass from the client's
     *                                error mappers, e.g. {@link OsrsAuthenticationException}, {@link OsrsUnavailableException})
     * @throws OsrsTransportException if no usable HTTP reply arrived
     * @throws OsrsProtocolException  if the reply isn't an {@code OPS_envelope}
     */
    public XcpResponse execute(XcpRequest request) {
        return execute(request, OsrsErrorMapper.none());
    }

    /**
     * Like {@link #execute(XcpRequest)}, consulting {@code mapper} first. Command-family
     * modules pass their own mapper here.
     *
     * @param request the command
     * @param mapper  maps failures this call understands
     * @return the successful reply
     */
    public XcpResponse execute(XcpRequest request, OsrsErrorMapper mapper) {
        XcpResponse r = send(request);
        if (!r.isSuccess()) {
            throw failure(r, mapper);
        }
        return r;
    }

    /**
     * The exception {@link #execute(XcpRequest, OsrsErrorMapper)} would throw for a failed reply.
     *
     * @param failed a reply with {@code is_success=0}
     * @param mapper consulted first
     * @return the exception, never {@code null}
     */
    public OsrsApiException failure(XcpResponse failed, OsrsErrorMapper mapper) {
        OsrsApiException e = mapper.orElse(errorMapper).orElse(BUILT_IN_ERRORS).map(failed);
        return e != null ? e : new OsrsApiException(failed);
    }

    /**
     * Checks whether a domain can be registered ({@code DOMAIN LOOKUP}).
     *
     * @param domain the domain name, e.g. {@code example.com}
     * @return whether it is available
     * @throws OsrsUnavailableException if the registry can't answer now (e.g. {@code 720})
     * @throws OsrsApiException for other failures
     */
    public LookupResult lookup(String domain) {
        return lookup(domain, false);
    }

    /**
     * @param domain  the domain name
     * @param noCache ask the registry instead of OpenSRS's cache (slower)
     * @return whether it is available
     */
    public LookupResult lookup(String domain, boolean noCache) {
        XcpRequest.Builder b = XcpRequest.builder("DOMAIN", "LOOKUP")
            .attribute("domain", Objects.requireNonNull(domain, "domain"))
            .idempotent(true);
        if (noCache) {
            b.attribute("no_cache", true);
        }
        return LookupResult.from(domain, execute(b.build()));
    }

    /**
     * Signs and posts a raw XML body. Meant for diagnostics; prefer {@link #send(XcpRequest)}.
     *
     * @param xml a complete {@code OPS_envelope}
     * @return the raw reply body
     */
    public String sendRaw(String xml) {
        return post(xml, RetryPolicy.none(), "raw");
    }

    private String post(String xml, RetryPolicy policy, String what) {
        byte[] body = xml.getBytes(StandardCharsets.UTF_8);
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "text/xml");
        headers.put("X-Username", config.getUsername());
        headers.put("X-Signature", OsrsSignature.sign(xml, config.getApiKey()));
        for (int attempt = 1; ; attempt++) {
            try {
                Transport.Reply reply;
                try {
                    reply = transport.post(config.getEndpoint(), headers, body,
                        config.getRequestTimeout());
                } catch (IOException e) {
                    throw new OsrsTransportException("OpenSRS " + what + ": no reply from "
                        + config.getEndpoint() + " (" + e + ")", -1, e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new OsrsException("Interrupted while calling OpenSRS " + what, e);
                }
                int s = reply.status();
                if ((s >= 200 && s < 300) || (reply.body() != null
                        && reply.body().contains("<OPS_envelope"))) {
                    return reply.body();
                }
                throw new OsrsTransportException("OpenSRS " + what + ": HTTP " + s + " from "
                    + config.getEndpoint(), s, null);
            } catch (OsrsTransportException e) {
                if (attempt >= policy.maxAttempts() || !policy.isRetryable(e)) {
                    throw e;
                }
                Duration d = policy.delayAfter(attempt);
                log.debug("OpenSRS {} attempt {} failed, retrying in {}: {}", what, attempt, d,
                    e.getMessage());
                try {
                    sleeper.sleep(d);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }

    /** Closes the transport (and its connections). */
    @Override
    public void close() {
        transport.close();
    }

    @Override
    public String toString() {
        return "OsrsClient[" + config + "]";
    }

    /** Builds an {@link OsrsClient}. */
    public static final class Builder {
        private OsrsConfig config;
        private Transport transport;
        private RetryPolicy retryPolicy = RetryPolicy.none();
        private OsrsErrorMapper errorMapper = OsrsErrorMapper.none();
        private Sleeper sleeper = d -> Thread.sleep(d.toMillis());

        private Builder() {
        }

        /**
         * @param config the account settings (required)
         * @return this builder
         */
        public Builder config(OsrsConfig config) {
            this.config = config;
            return this;
        }

        /**
         * @param transport how requests are sent; default {@link JdkHttpTransport}
         * @return this builder
         */
        public Builder transport(Transport transport) {
            this.transport = transport;
            return this;
        }

        /**
         * @param retryPolicy applied to idempotent requests only; default {@link RetryPolicy#none()}
         * @return this builder
         */
        public Builder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = Objects.requireNonNull(retryPolicy, "retryPolicy");
            return this;
        }

        /**
         * Adds a mapper consulted for every failed reply, after any per-call mapper.
         *
         * @param mapper the mapper
         * @return this builder
         */
        public Builder errorMapper(OsrsErrorMapper mapper) {
            this.errorMapper = this.errorMapper.orElse(Objects.requireNonNull(mapper, "mapper"));
            return this;
        }

        Builder sleeper(Sleeper sleeper) {
            this.sleeper = sleeper;
            return this;
        }

        /**
         * @return the client
         */
        public OsrsClient build() {
            return new OsrsClient(this);
        }
    }
}
