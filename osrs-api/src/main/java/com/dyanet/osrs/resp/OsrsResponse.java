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

package com.dyanet.osrs.resp;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Abstract Response
 * 
 * @author Akber Choudhry
 */
public abstract class OsrsResponse {

  private String action;
  private String object;
  private String protocol;
  private boolean errorFlag = false;
  private int errorCode;
  private String errorMessage;

  protected static Logger logger = LoggerFactory.getLogger(OsrsResponse.class);
  
  public abstract void populate(Map<String,String> attrMap, String level);

  public String getAction() {
    return action;
  }
  public void setAction(String action) {
    this.action = action;
  }
  public String getObject() {
    return object;
  }
  public void setObject(String object) {
    this.object = object;
  }

  public boolean isError() {
    return errorFlag;
  }

  public void setError(boolean errorFlag) {
    this.errorFlag = errorFlag;
  }

  public int getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(int errorCode) {
    this.errorCode = errorCode;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public String getProtocol() {
    return protocol;
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  public String toString() {
    return "Response action: " + action + 
    "; object: " + object +
    "; error code: " + errorCode + 
    "; error message: " + errorMessage;
  }
}
