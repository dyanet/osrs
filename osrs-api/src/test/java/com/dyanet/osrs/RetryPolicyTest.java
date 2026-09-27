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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class RetryPolicyTest {

    @Test
    void exponentialDoublesUpToTheCap() {
        RetryPolicy p = RetryPolicy.exponential(5, Duration.ofMillis(100), Duration.ofMillis(350));
        assertEquals(5, p.maxAttempts());
        assertEquals(Duration.ofMillis(100), p.delayAfter(1));
        assertEquals(Duration.ofMillis(200), p.delayAfter(2));
        assertEquals(Duration.ofMillis(350), p.delayAfter(3));
        assertEquals(Duration.ofMillis(350), p.delayAfter(10));
        assertEquals(1, RetryPolicy.none().maxAttempts());
        assertThrows(IllegalArgumentException.class, () -> RetryPolicy.exponential(0, Duration.ZERO, Duration.ZERO));
    }

    @Test
    void retriesNoReplyAnd5xxOnly() {
        RetryPolicy p = RetryPolicy.none();
        assertTrue(p.isRetryable(new OsrsTransportException("x", -1, null)));
        assertTrue(p.isRetryable(new OsrsTransportException("x", 503, null)));
        assertFalse(p.isRetryable(new OsrsTransportException("x", 404, null)));
    }
}
