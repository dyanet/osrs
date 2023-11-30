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

/**
 * OSRS System Configuration
 * 
 * @author Akber Choudhry
 */
public interface Config {

  /**
   * Setting default path to the base directory of OSRS
   * <code>osrs.dir</code> is a property that can be set with
   * the <code>-Dosrs.dir</code> option in the JVM
   * Do not include a trailing separator.  For example:
   * <code>/opt/osrs</code>
   */
  public static final String OSRSDIR = System.getProperty("osrs.dir","src/main/resources");

  /**
   * Configurations folder under the.  It can contain multiple
   * configuration files, most likely a test and a live 
   */
  public static final String OSRSCONFIG = "/config/";
  
  
  /**
   * Active Config file is file that points to the OpenSRS file holding connection information
   * It will be loaded from  OSRSDIR + OSRSCONFIG + "activeConfig.xml";
   * At the minimum, it should contain <code>&lt;activeConfig&gt;something.xml&lt;/activeConfig&gt;</code>
   * where 'something.xml' is a valid configuration in the OSRSCONFIG folder
   * 
   * Any change to this file should reload the system
   * 
   * As features are added, this will contain the global configuration 
   * such as proxies, etc.
   */
  public static final String ACTIVECONFIG = "activeConfig.xml";
}
