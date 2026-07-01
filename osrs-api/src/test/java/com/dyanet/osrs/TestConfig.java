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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Offline tests for the simplified {@link OsrsConfig} loader.
 */
public class TestConfig {

    @AfterEach
    public void restoreDefaults() {
        // Ensure other test classes see the default (classpath) test config.
        System.clearProperty(OsrsConfig.CONFIG_PROPERTY);
        System.clearProperty(OsrsConfig.ENV_PROPERTY);
        OsrsConfig.envLookup = System::getenv;
        OsrsConfig.reset();
    }

    @Test
    public void loadsDefaultTestConfigFromClasspath() {
        OsrsConfig.reset();
        assertEquals("horizon.opensrs.net", OsrsConfig.getValue("osrs.host"));
        assertEquals("55443", OsrsConfig.getValue("osrs.sslPort"));
        assertEquals("dyanet", OsrsConfig.getValue("osrs.userName"));
        assertEquals("test", OsrsConfig.getValue("osrs.environment"));
    }

    @Test
    public void unknownKeyReturnsNull() {
        OsrsConfig.reset();
        assertNull(OsrsConfig.getValue("osrs.doesNotExist"));
    }

    @Test
    public void getExposesRawProperties() {
        OsrsConfig.reset();
        assertNotNull(OsrsConfig.get());
        assertEquals("https", OsrsConfig.get().getProperty("osrs.protocol"));
    }

    @Test
    public void configPropertyOverridesWithFilesystemPath(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("custom.properties");
        Files.writeString(file, """
                osrs.environment=test
                osrs.protocol=https
                osrs.host=example.test
                osrs.port=80
                osrs.sslPort=1234
                osrs.userName=alice
                osrs.password=none
                osrs.key=deadbeef
                osrs.version=0.9
                osrs.baseClassVersion=0.9.1
                """);

        System.setProperty(OsrsConfig.CONFIG_PROPERTY, file.toString());
        OsrsConfig.reset();

        assertEquals("example.test", OsrsConfig.getValue("osrs.host"));
        assertEquals("1234", OsrsConfig.getValue("osrs.sslPort"));
        assertEquals("alice", OsrsConfig.getValue("osrs.userName"));
    }

    @Test
    public void osrsConfigEnvVarOverridesWithFilesystemPath(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("from-env.properties");
        Files.writeString(file, """
                osrs.environment=test
                osrs.protocol=https
                osrs.host=env-var.example.test
                osrs.port=80
                osrs.sslPort=4321
                osrs.userName=bob
                osrs.password=none
                osrs.key=cafebabe
                osrs.version=0.9
                osrs.baseClassVersion=0.9.1
                """);

        // Simulate OSRS_CONFIG=<file> without mutating the real process environment.
        String path = file.toString();
        OsrsConfig.envLookup = key -> OsrsConfig.CONFIG_ENV_VAR.equals(key) ? path : null;
        OsrsConfig.reset();

        assertEquals("env-var.example.test", OsrsConfig.getValue("osrs.host"));
        assertEquals("bob", OsrsConfig.getValue("osrs.userName"));
    }

    @Test
    public void systemPropertyTakesPrecedenceOverConfigEnvVar(@TempDir Path dir) throws Exception {
        Path fromEnv = dir.resolve("from-env.properties");
        Files.writeString(fromEnv, """
                osrs.environment=test
                osrs.protocol=https
                osrs.host=env-var.example.test
                osrs.port=80
                osrs.sslPort=4321
                osrs.userName=bob
                osrs.password=none
                osrs.key=cafebabe
                osrs.version=0.9
                osrs.baseClassVersion=0.9.1
                """);
        Path fromProperty = dir.resolve("from-property.properties");
        Files.writeString(fromProperty, """
                osrs.environment=test
                osrs.protocol=https
                osrs.host=system-property.example.test
                osrs.port=80
                osrs.sslPort=1111
                osrs.userName=alice
                osrs.password=none
                osrs.key=deadbeef
                osrs.version=0.9
                osrs.baseClassVersion=0.9.1
                """);

        String envPath = fromEnv.toString();
        OsrsConfig.envLookup = key -> OsrsConfig.CONFIG_ENV_VAR.equals(key) ? envPath : null;
        System.setProperty(OsrsConfig.CONFIG_PROPERTY, fromProperty.toString());
        OsrsConfig.reset();

        assertEquals("system-property.example.test", OsrsConfig.getValue("osrs.host"),
                "-Dosrs.config must win over OSRS_CONFIG");
    }

    @Test
    public void missingRequiredKeyIsReported(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("incomplete.properties");
        // Missing osrs.key on purpose.
        Files.writeString(file, """
                osrs.environment=test
                osrs.protocol=https
                osrs.host=example.test
                osrs.port=80
                osrs.sslPort=1234
                osrs.userName=alice
                osrs.password=none
                osrs.version=0.9
                osrs.baseClassVersion=0.9.1
                """);

        System.setProperty(OsrsConfig.CONFIG_PROPERTY, file.toString());
        OsrsConfig.reset();

        OsrsException ex = assertThrows(OsrsException.class, () -> OsrsConfig.getValue("osrs.host"));
        assertTrue(ex.getMessage().contains("osrs.key"), "message should name the missing key");
    }

    @Test
    public void unknownConfigResourceThrows() {
        System.setProperty(OsrsConfig.CONFIG_PROPERTY, "no-such-config.properties");
        OsrsConfig.reset();
        assertThrows(OsrsException.class, () -> OsrsConfig.getValue("osrs.host"));
    }
}
