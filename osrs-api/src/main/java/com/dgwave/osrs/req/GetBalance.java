/*
 * Copyright 2012, Digiwave Systems Ltd., Akber A. Choudhry,
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

package com.dgwave.osrs.req;

import static com.dgwave.osrs.OsrsConstants.ACTION_GET_BALANCE;
import static com.dgwave.osrs.OsrsConstants.MODEL_BALANCE;
import static com.dgwave.osrs.OsrsConstants.OBJECT_BALANCE;
import static com.dgwave.osrs.OsrsConstants.PARAM_REGISTRANT_IP;

import java.net.InetAddress;
import java.util.Map;

import com.dgwave.osrs.OsrsException;

/**
 * @author Akber Choudhry
 */
public class GetBalance extends OsrsRequest {

	private String registrantIp;
	
	/**
	 * Default Constructor
	 */
	public GetBalance() {
		super();
		setObject(OBJECT_BALANCE);
		setAction(ACTION_GET_BALANCE);
		setModel(MODEL_BALANCE);
	}
	
	@Override
	public Map<String, Object> getAttributes() {
		addSimpleParam(PARAM_REGISTRANT_IP, getRegistrantIp());
		return getSimpleParams();
	}
	public String getRegistrantIp() {
		return registrantIp;
	}
	public void setRegistrantIp(String registrantIp) throws OsrsException {
		if (registrantIp != null && !"".equals(registrantIp)) {
			this.registrantIp = registrantIp;
		}  else {
		   try{
		      InetAddress ownIP=InetAddress.getLocalHost();
		      this.registrantIp = ownIP.getHostAddress();
		    }catch (Exception e){
		      throw new OsrsException("Error getting localhost IP",e);
		    }
		}
	}
}
