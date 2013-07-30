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

package com.dgwave.osrs.resp;

import static com.dgwave.osrs.OsrsConstants.KEY_BELONGS_TO_RSP;
import static com.dgwave.osrs.OsrsConstants.KEY_DOMAIN_EXPIRY_DATE;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Map;

import com.dgwave.osrs.model.Domain;

/**
 * @author Akber Choudhry
 */
public class DomainResponse extends OsrsResponse {


	private Domain domain;
	
	/* (non-Javadoc)
	 * @see com.dgwave.osrs.resp.OsrsResponse#populate(Map<String, String> attrMap)
	 */
	@Override
	public void populate(Map<String, String> attrMap, String level) {
		DateFormat dateFormat = new SimpleDateFormat();
		try {
			for (Iterator<String> it1 = attrMap.keySet().iterator(); it1.hasNext();) {
				String key = (String) it1.next();
				if (KEY_BELONGS_TO_RSP.equals(key)) {
					int tf = Integer.parseInt(attrMap.get(key));
					this.domain.setBelongsToRsp(tf==1?true:false);
				} else if (KEY_DOMAIN_EXPIRY_DATE.equals(key)) {
					this.domain.setExpireDate(dateFormat.parse(attrMap.get(key)));
				} 
			}
		} catch (NumberFormatException e) {
			logger.warn("Deleted Domains Response not populated", e);
		} catch (ParseException e) {
			logger.warn("Deleted Domains Response not populated", e);
		}
	}

	public Domain getDomain() {
		return domain;
	}
	public void setDomain(Domain domain) {
		
	}
}
