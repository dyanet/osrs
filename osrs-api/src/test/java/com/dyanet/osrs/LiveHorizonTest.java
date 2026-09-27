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

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Live call to the OpenSRS test environment. Runs only with {@code mvn verify -Pintegration}
 * and real credentials in the file named by {@code OSRS_CONFIG} or {@code -Dosrs.config}.
 */
@Tag("integration")
class LiveHorizonTest {

    @Test
    void lookupOfAWellKnownDomain() {
        try (OsrsClient c = OsrsClient.fromDefaultConfig()) {
            assertFalse(c.lookup("google.com").available());
        }
    }
}
