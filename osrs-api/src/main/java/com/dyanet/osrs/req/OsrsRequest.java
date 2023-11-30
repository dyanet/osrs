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

package com.dyanet.osrs.req;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract Request
 * 
 * @author Akber Choudhry
 */
public abstract class OsrsRequest {
  
  private String action;
  private String object;
  private String model;
  
  private Map<String, Object> simpleParams = new HashMap<String, Object>();
  private Map<String, Map<String, Object>> complexParams = new HashMap<String, Map<String, Object>>();
  
  private Date lastDate = new Date(1924991999000L);
  
  public void addSimpleParam(String name, String value) {
    if (name != null && !"".equals(name) && value != null && !"".equals(value)) {
      this.simpleParams.put(name, value);
    }
  }

  public void addComplexParam(String name, Map<String, Object> map) {
    if (name != null && !"".equals(name) && map != null && !map.isEmpty()) {
      this.complexParams.put(name, map);
    }
  }

  public String getAction() {
    return action;
  }

  protected void setAction(String action) {
    this.action = action;
  }

  public String getObject() {
    return object;
  }

  protected void setObject(String object) {
    this.object = object;
  }
  
  public abstract Map<String, Object> getAttributes();

  protected Map<String, Object> getSimpleParams() {
    return simpleParams;
  }

  protected Map<String, Map<String, Object>> getComplexParams() {
    return complexParams;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  protected String cleanDate(Date date) {
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    if (date == null) {
      return null;
    } else {
      if (date.compareTo(lastDate)>0) {
        return format.format(lastDate);
      } else {
        return format.format(date);
      }
    }
  }

  
}
