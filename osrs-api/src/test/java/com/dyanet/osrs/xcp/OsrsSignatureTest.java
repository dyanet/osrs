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

package com.dyanet.osrs.xcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.junit.jupiter.api.Test;

class OsrsSignatureTest {

    @Test
    void matchesTheTestVectorFromTheOpenSrsTroubleshootingGuide() {
        assertEquals("e787cc1d1951dfec4827cede7b1a0933", OsrsSignature.md5Hex("ConnecttoOpenSRSviaSSL"));
    }

    @Test
    void isDoubleMd5OfBodyAndKey() {
        String body = "<OPS_envelope/>";
        String key = "k3y";
        assertEquals(OsrsSignature.md5Hex(OsrsSignature.md5Hex(body + key) + key),
            OsrsSignature.sign(body, key));
    }

    @Test
    void hashesUtf8Bytes() throws Exception {
        String body = "Zoë Müller 😀";
        byte[] d = MessageDigest.getInstance("MD5").digest(body.getBytes(StandardCharsets.UTF_8));
        assertEquals(HexFormat.of().formatHex(d), OsrsSignature.md5Hex(body));
    }

    /**
     * Regression: 0.9.x formatted digests with BigInteger.toString(16), which drops leading
     * zero bytes, so about 1 request in 256 carried a 30-character (invalid) signature.
     */
    @Test
    void keepsLeadingZerosThatTheOldFormatterDropped() throws Exception {
        String input = null;
        for (int i = 0; input == null; i++) {
            String candidate = "body-" + i;
            if (OsrsSignature.md5Hex(candidate).startsWith("00")) {
                input = candidate;
            }
        }
        String hex = OsrsSignature.md5Hex(input);
        assertEquals(32, hex.length());
        assertNotEquals(hex, oldBytesToHex(MessageDigest.getInstance("MD5")
            .digest(input.getBytes(StandardCharsets.UTF_8))));
    }

    /** The 0.9.x implementation, kept here to show the bug. */
    private static String oldBytesToHex(byte[] bytes) {
        String s = new BigInteger(1, bytes).toString(16);
        return s.length() % 2 != 0 ? "0" + s : s;
    }
}
