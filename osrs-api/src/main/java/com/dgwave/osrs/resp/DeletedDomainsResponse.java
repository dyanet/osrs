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

import static com.dgwave.osrs.OsrsConstants.KEY_DELETE_DATE;
import static com.dgwave.osrs.OsrsConstants.KEY_DELETE_DATE_EPOCH;
import static com.dgwave.osrs.OsrsConstants.KEY_DELETE_DOMAINS;
import static com.dgwave.osrs.OsrsConstants.KEY_EXPIRE_DATE;
import static com.dgwave.osrs.OsrsConstants.KEY_EXPIRE_DATE_EPOCH;
import static com.dgwave.osrs.OsrsConstants.KEY_NAME;
import static com.dgwave.osrs.OsrsConstants.KEY_PAGE_SIZE;
import static com.dgwave.osrs.OsrsConstants.KEY_REASON;
import static com.dgwave.osrs.OsrsConstants.KEY_TOTAL;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.dgwave.osrs.model.DeletedDomain;

/**
 * @author Akber Choudhry
 *
 */
public class DeletedDomainsResponse extends OsrsResponse {


	private List<DeletedDomain> deletedDomains = new ArrayList<DeletedDomain>();
	private Integer pageSize;
	private Integer total;
	
	/* (non-Javadoc)
	 * @see com.dgwave.osrs.resp.OsrsResponse#populate(Map<String, String> attrMap, String level)
	 */
	@Override
	public void populate(Map<String, String> attrMap, String level) {
		try {
			if (KEY_DELETE_DOMAINS.equals(level)) {
				this.deletedDomains.add(createDeletedDomain(attrMap));
			} else {
				for (Iterator<String> it1 = attrMap.keySet().iterator(); it1.hasNext();) {
					String key = it1.next();
					if (KEY_PAGE_SIZE.equals(key)) {
						this.pageSize = Integer.parseInt(attrMap.get(key));
					} else if (KEY_TOTAL.equals(key)) {
						this.total = Integer.parseInt(attrMap.get(key));
					} 
				}
			}
		} catch (NumberFormatException e) {
			logger.warn("Deleted Domains Response not populated", e);
		} catch (ParseException e) {
			logger.warn("Deleted Domains Response not populated", e);
		}
	}


	private DeletedDomain createDeletedDomain(Map<String, String> attrMap2) throws ParseException {
		DeletedDomain delDomain = new DeletedDomain();
		DateFormat dateFormat = new SimpleDateFormat();
		
		for (Iterator<String> it2 = attrMap2.keySet().iterator(); it2.hasNext();) {
			String key = it2.next();
			if (KEY_DELETE_DATE.equals(key)) {
				delDomain.setDeleteDate(dateFormat.parse(attrMap2.get(key)));
			} else if (KEY_DELETE_DATE_EPOCH.equals(key)) {
				delDomain.setDeleteDateEpoch(new Long(attrMap2.get(key)));
			} else if (KEY_EXPIRE_DATE.equals(key)) {
				delDomain.setExpireDate(dateFormat.parse(attrMap2.get(key)));
			} else if (KEY_EXPIRE_DATE_EPOCH.equals(key)) {
				delDomain.setExpireDateEpoch(new Long(attrMap2.get(key)));
			} else if (KEY_NAME.equals(key)) {
				delDomain.setName(attrMap2.get(key));
			} else if (KEY_REASON.equals(key)) {
				delDomain.setReason(attrMap2.get(key));
			}
		}
		return delDomain;
	}


	public List<DeletedDomain> getDeletedDomains() {
		return deletedDomains;
	}

	public void setDeletedDomains(List<DeletedDomain> deletedDomains) {
		this.deletedDomains = deletedDomains;
	}

	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}

	public Integer getTotal() {
		return total;
	}

	public void setTotal(Integer total) {
		this.total = total;
	}

}
