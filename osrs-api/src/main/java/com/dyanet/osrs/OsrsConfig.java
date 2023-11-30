/*
 * Copyright 2012-2023, Dyanet Inc., Akber A. Choudhry,
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

import static com.dyanet.osrs.Config.ACTIVECONFIG;
import static com.dyanet.osrs.Config.OSRSCONFIG;
import static com.dyanet.osrs.Config.OSRSDIR;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.fluent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Active configuration read in from XML and exposed by static methods
 * 
 * @author Akber Choudhry
 */
public class OsrsConfig {
    private static XMLConfiguration config;
    private static Logger logger = LoggerFactory.getLogger(OsrsConfig.class);

    /**
     * Internal private constructor
     */
    private OsrsConfig() {
        loadConfig();
    }

    /**
     * Looks up active file, loads active file and validates it
     */
    private static void loadConfig() {
        Configurations configs = new Configurations();
        
        try {
            XMLConfiguration actConfig = configs.fileBasedBuilder(XMLConfiguration.class, 
                    OSRSDIR + OSRSCONFIG + ACTIVECONFIG)
                    .getConfiguration();

            String actFile = actConfig.getString("activeConfig");
            actConfig = null; // just a reminder
            
            config = configs.fileBasedBuilder(XMLConfiguration.class,
                OSRSDIR + OSRSCONFIG + actFile)
                .getConfiguration();

            validateConfiguration();
            
            logger.info("OSRS configuration loaded successfully");
        } catch (Exception ce) {
            logger.error("Initial Configuration could not be loaded - OSRS will not run");
            throw new OsrsException("Configuration Problem", ce);
        }
    }

    /**
     * Use for plain key->value lookups.  Nested elements in the
     * configuration can be accessed by '.', such as <code>osrs.host</code>
     * @param String key
     * @return String value
     */
    public static String getValue(String key) {
        if (config == null) {
            loadConfig();
        }

            return config.getString(key);
    }

    /**
     * Exposes the methods of the config.
     * Use for any complicated retrievals from the config
     * 
     * @return XMLConfiguration
     */
    public static XMLConfiguration get() {
        if (config == null) {
            loadConfig();
        }
        return config;
    }

    /**
     * Perform minimal validation on the configuration
     */
    private static void validateConfiguration() throws OsrsException {
            String osrsUsername = config.getString(
                    "osrs.userName", "");
            String osrsPassword = config.getString(
                    "osrs.password", "");
            String osrsKey = config.getString(
                    "osrs.key", "");
            String osrsEnvironment = config.getString(
                    "osrs.environment", "");
            String osrsProtocol = config.getString(
                    "osrs.protocol", "");
            String osrsHost = config.getString(
                    "osrs.host", "");
            String osrsPort = config.getString(
                    "osrs.port", "");
            String osrsSslPort = config.getString(
                    "osrs.sslPort", "");
            String osrsBaseClassVersion = config.getString(
                    "osrs.baseClassVersion", "");
            String osrsVersion = config.getString(
                    "osrs.version", "");

            final String configError = "OSRS Error - Incomplete config file - Missing osrs_";
            if ("".equals(osrsUsername)) {
                throw new OsrsException(configError + "userName");
            }
            if ("".equals(osrsPassword)) {
                throw new OsrsException(configError + "password");
            }
            if ("".equals(osrsKey)) {
                throw new OsrsException(configError + "key");
            }
            if ("".equals(osrsEnvironment)) {
                throw new OsrsException(configError + "environment");
            }
            if ("".equals(osrsProtocol)) {
                throw new OsrsException(configError + "protocol");
            }

            if ("".equals(osrsHost)) {
                throw new OsrsException(configError + "host");
            }
            if ("".equals(osrsPort)) {
                throw new OsrsException(configError + "port");
            }
            if ("".equals(osrsSslPort)) {
                throw new OsrsException(configError + "sslPort");
            }
            if ("".equals(osrsBaseClassVersion)) {
                throw new OsrsException(configError + "baseClassVersion");
            }
            if ("".equals(osrsVersion)) {
                throw new OsrsException(configError + "version");
            }
    }
}