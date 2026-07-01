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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Active configuration, loaded once from a simple {@code .properties} file.
 *
 * <p>The file is resolved in this order:
 * <ol>
 *   <li>the system property {@code osrs.config} &mdash; either a filesystem path
 *       or a classpath resource name, letting you point at an external file;</li>
 *   <li>otherwise the {@code OSRS_CONFIG} environment variable, same semantics
 *       &mdash; convenient for container/{@code .env} deployments that export
 *       environment variables rather than pass JVM {@code -D} flags;</li>
 *   <li>otherwise {@code osrs-<env>.properties} on the classpath, where
 *       {@code <env>} is the system property {@code osrs.env} (default
 *       {@code test}).</li>
 * </ol>
 *
 * <p>Expected keys: {@code osrs.host}, {@code osrs.port}, {@code osrs.sslPort},
 * {@code osrs.protocol}, {@code osrs.environment}, {@code osrs.userName},
 * {@code osrs.password}, {@code osrs.key}, {@code osrs.version},
 * {@code osrs.baseClassVersion}.
 *
 * @author Akber Choudhry
 */
public final class OsrsConfig {

    /** System property naming an explicit config file (path or classpath resource). */
    public static final String CONFIG_PROPERTY = "osrs.config";
    /** Environment variable naming an explicit config file, checked if {@link #CONFIG_PROPERTY} is unset. */
    public static final String CONFIG_ENV_VAR = "OSRS_CONFIG";
    /** System property selecting the environment when neither config override is set. */
    public static final String ENV_PROPERTY = "osrs.env";
    /** Environment used when {@link #ENV_PROPERTY} is unset. */
    public static final String DEFAULT_ENV = "test";

    /** Keys that must be present and non-blank for the client to run. */
    private static final String[] REQUIRED_KEYS = {
        "osrs.userName", "osrs.password", "osrs.key", "osrs.environment",
        "osrs.protocol", "osrs.host", "osrs.port", "osrs.sslPort",
        "osrs.baseClassVersion", "osrs.version"
    };

    private static final Logger logger = LoggerFactory.getLogger(OsrsConfig.class);

    private static volatile Properties config;

    /**
     * Environment variable lookup, defaulting to the real process environment.
     * Package-private so tests can substitute a fake lookup instead of
     * mutating actual process environment variables (which the JDK does not
     * support doing safely at runtime).
     */
    static java.util.function.Function<String, String> envLookup = System::getenv;

    private OsrsConfig() {
    }

    private static synchronized void loadConfig() {
        if (config != null) {
            return;
        }
        String resource = resolveResource();
        Properties loaded = new Properties();
        try (InputStream in = openStream(resource)) {
            if (in == null) {
                throw new OsrsException("OSRS configuration not found: " + resource);
            }
            loaded.load(in);
        } catch (IOException e) {
            throw new OsrsException("Could not read OSRS configuration: " + resource, e);
        }
        validate(loaded, resource);
        config = loaded;
        logger.info("OSRS configuration loaded from {}", resource);
    }

    private static String resolveResource() {
        String override = System.getProperty(CONFIG_PROPERTY);
        if (override != null && !override.isBlank()) {
            return override;
        }
        String envVarOverride = envLookup.apply(CONFIG_ENV_VAR);
        if (envVarOverride != null && !envVarOverride.isBlank()) {
            return envVarOverride;
        }
        String env = System.getProperty(ENV_PROPERTY, DEFAULT_ENV);
        return "osrs-" + env + ".properties";
    }

    /** Try the resource as a filesystem path first, then fall back to the classpath. */
    private static InputStream openStream(String resource) throws IOException {
        File file = new File(resource);
        if (file.isFile()) {
            return new FileInputStream(file);
        }
        return OsrsConfig.class.getClassLoader().getResourceAsStream(resource);
    }

    private static void validate(Properties p, String resource) {
        for (String key : REQUIRED_KEYS) {
            String value = p.getProperty(key);
            if (value == null || value.isBlank()) {
                throw new OsrsException(
                    "OSRS configuration " + resource + " is missing required key: " + key);
            }
        }
    }

    /**
     * Look up a configuration value by key, for example {@code osrs.host}.
     *
     * @param key the property key
     * @return the value, or {@code null} if absent
     */
    public static String getValue(String key) {
        if (config == null) {
            loadConfig();
        }
        return config.getProperty(key);
    }

    /**
     * @return the raw {@link Properties} backing the configuration
     */
    public static Properties get() {
        if (config == null) {
            loadConfig();
        }
        return config;
    }

    /**
     * Drop the cached configuration so the next access reloads it. Intended for
     * tests that switch {@code osrs.env} / {@code osrs.config} / {@code OSRS_CONFIG}
     * at runtime.
     */
    public static void reset() {
        config = null;
    }
}
