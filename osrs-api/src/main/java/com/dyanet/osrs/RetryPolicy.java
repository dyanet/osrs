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

import java.time.Duration;

/**
 * Decides whether a request that failed in transport is sent again.
 *
 * <p>Only requests marked {@linkplain com.dyanet.osrs.xcp.XcpRequest#isIdempotent() idempotent}
 * (reads such as lookups) are ever retried, and only after an {@link OsrsTransportException}
 * (no reply, or an HTTP 5xx). A reply from OpenSRS, even a failed one, is never retried here:
 * whether a response code is worth retrying is the caller's decision.
 */
public interface RetryPolicy {

    /**
     * @return the total number of attempts, at least 1
     */
    int maxAttempts();

    /**
     * @param failedAttempt the attempt that just failed, starting at 1
     * @return how long to wait before the next attempt
     */
    Duration delayAfter(int failedAttempt);

    /**
     * @param failure the transport failure
     * @return whether this failure is worth retrying; by default no-reply failures and 5xx
     */
    default boolean isRetryable(OsrsTransportException failure) {
        int s = failure.getHttpStatus();
        return s < 0 || s >= 500;
    }

    /**
     * @return a policy that never retries (the default)
     */
    static RetryPolicy none() {
        return exponential(1, Duration.ZERO, Duration.ZERO);
    }

    /**
     * Exponential backoff: {@code initial}, then doubling up to {@code max}.
     *
     * @param maxAttempts total attempts, at least 1
     * @param initial     delay after the first failure
     * @param max         upper bound for any delay
     * @return the policy
     */
    static RetryPolicy exponential(int maxAttempts, Duration initial, Duration max) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
        return new RetryPolicy() {
            @Override
            public int maxAttempts() {
                return maxAttempts;
            }

            @Override
            public Duration delayAfter(int failedAttempt) {
                Duration d = initial;
                for (int i = 1; i < failedAttempt && d.compareTo(max) < 0; i++) {
                    d = d.multipliedBy(2);
                }
                return d.compareTo(max) > 0 ? max : d;
            }
        };
    }
}
