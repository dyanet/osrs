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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * The {@code X-Signature} header: {@code md5(md5(body + key) + key)}, as lowercase hex, computed
 * over the UTF-8 bytes of exactly the body that is sent.
 */
public final class OsrsSignature {

    private OsrsSignature() {
    }

    /**
     * @param body   the exact XML body that will be sent
     * @param apiKey the reseller's private API key
     * @return 32 lowercase hex characters
     */
    public static String sign(String body, String apiKey) {
        return md5Hex(md5Hex(body + apiKey) + apiKey);
    }

    /**
     * @param s input, encoded as UTF-8
     * @return the MD5 digest as 32 lowercase hex characters (leading zeros kept)
     */
    public static String md5Hex(String s) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(md5.digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 is not available in this JVM", e);
        }
    }
}
