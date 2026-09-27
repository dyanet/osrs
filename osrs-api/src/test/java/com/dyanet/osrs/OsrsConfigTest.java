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

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OsrsConfigTest {

    @AfterEach
    void restore() {
        System.clearProperty(OsrsConfig.CONFIG_PROPERTY);
        System.clearProperty(OsrsConfig.ENV_PROPERTY);
        OsrsConfig.envLookup = System::getenv;
    }

    @Test
    void testAndLiveFactories() {
        OsrsConfig t = OsrsConfig.test("me", "k");
        assertEquals(URI.create("https://horizon.opensrs.net:55443/"), t.getEndpoint());
        assertEquals("me", t.getUsername());
        assertEquals("k", t.getApiKey());
        assertEquals(Duration.ofSeconds(30), t.getConnectTimeout());
        assertEquals(Duration.ofSeconds(120), t.getRequestTimeout());
        assertEquals(URI.create("https://rr-n1-tor.opensrs.net:55443/"), OsrsConfig.live("me", "k").getEndpoint());
    }

    @Test
    void builderValidatesAndNeverPrintsTheKey() {
        assertThrows(IllegalArgumentException.class, () -> OsrsConfig.builder().username("u").apiKey("k").build());
        assertThrows(IllegalArgumentException.class, () -> OsrsConfig.builder().host("h").apiKey("k").build());
        assertThrows(IllegalArgumentException.class, () -> OsrsConfig.builder().host("h").username("u").apiKey(" ").build());
        assertThrows(IllegalArgumentException.class, () -> OsrsConfig.builder().port(0));
        assertThrows(IllegalArgumentException.class, () -> OsrsConfig.builder().port(70000));
        OsrsConfig c = OsrsConfig.builder().endpoint(URI.create("http://127.0.0.1:1/"))
            .username("u").apiKey("sup3rsecret").connectTimeout(Duration.ofSeconds(1))
            .requestTimeout(Duration.ofSeconds(2)).build();
        assertEquals(URI.create("http://127.0.0.1:1/"), c.getEndpoint());
        assertFalse(c.toString().contains("sup3rsecret"));
        assertTrue(c.toString().contains("****"));
    }

    @Test
    void readsPropertiesWrittenForEarlierVersions() {
        Properties p = new Properties();
        p.setProperty("osrs.environment", "test");
        p.setProperty("osrs.host", "horizon.opensrs.net");
        p.setProperty("osrs.port", "80");
        p.setProperty("osrs.sslPort", "55443");
        p.setProperty("osrs.userName", "me");
        p.setProperty("osrs.key", "k");
        p.setProperty("osrs.connectTimeoutMs", "1500");
        p.setProperty("osrs.requestTimeoutMs", "2500");
        OsrsConfig c = OsrsConfig.fromProperties(p);
        assertEquals(URI.create("https://horizon.opensrs.net:55443/"), c.getEndpoint());
        assertEquals(Duration.ofMillis(1500), c.getConnectTimeout());
        assertEquals(Duration.ofMillis(2500), c.getRequestTimeout());

        p.setProperty("osrs.sslPort", "notaport");
        assertThrows(OsrsException.class, () -> OsrsConfig.fromProperties(p));
        p.setProperty("osrs.sslPort", "55443");
        p.remove("osrs.key");
        OsrsException e = assertThrows(OsrsException.class, () -> OsrsConfig.fromProperties(p));
        assertTrue(e.getMessage().contains("apiKey"), e.getMessage());
    }

    @Test
    void loadsTheClasspathDefault() {
        OsrsConfig.envLookup = k -> null;
        OsrsConfig c = OsrsConfig.load();
        assertEquals("horizon.opensrs.net", c.getEndpoint().getHost());
    }

    @Test
    void systemPropertyThenEnvironmentVariable(@TempDir Path dir) throws Exception {
        Path a = dir.resolve("a.properties");
        Files.writeString(a, "osrs.host=a.example\nosrs.userName=u\nosrs.key=k\n");
        Path b = dir.resolve("b.properties");
        Files.writeString(b, "osrs.host=b.example\nosrs.userName=u\nosrs.key=k\n");
        OsrsConfig.envLookup = k -> OsrsConfig.CONFIG_ENV_VAR.equals(k) ? b.toString() : null;
        assertEquals("b.example", OsrsConfig.load().getEndpoint().getHost());
        System.setProperty(OsrsConfig.CONFIG_PROPERTY, a.toString());
        assertEquals("a.example", OsrsConfig.load().getEndpoint().getHost());
    }

    @Test
    void missingOrInvalidFilesAreReported(@TempDir Path dir) throws Exception {
        OsrsConfig.envLookup = k -> null;
        System.setProperty(OsrsConfig.ENV_PROPERTY, "nowhere");
        OsrsException e = assertThrows(OsrsException.class, OsrsConfig::load);
        assertTrue(e.getMessage().contains("osrs-nowhere.properties"), e.getMessage());

        Path bad = dir.resolve("bad.properties");
        Files.writeString(bad, "osrs.host=h\n");
        System.setProperty(OsrsConfig.CONFIG_PROPERTY, bad.toString());
        e = assertThrows(OsrsException.class, OsrsConfig::load);
        assertTrue(e.getMessage().contains(bad.toString()), e.getMessage());
    }
}
