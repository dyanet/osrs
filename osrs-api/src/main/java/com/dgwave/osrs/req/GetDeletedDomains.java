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

import static com.dgwave.osrs.OsrsConstants.ACTION_GET_DELETED_DOMAINS;
import static com.dgwave.osrs.OsrsConstants.MODEL_DELETED_DOMAINS;
import static com.dgwave.osrs.OsrsConstants.OBJECT_DOMAIN;
import static com.dgwave.osrs.OsrsConstants.PARAM_ADMIN_EMAIL;
import static com.dgwave.osrs.OsrsConstants.PARAM_BILLING_EMAIL;
import static com.dgwave.osrs.OsrsConstants.PARAM_DELETED_FROM;
import static com.dgwave.osrs.OsrsConstants.PARAM_DELETED_TO;
import static com.dgwave.osrs.OsrsConstants.PARAM_DOMAIN;
import static com.dgwave.osrs.OsrsConstants.PARAM_EXPIRED_FROM;
import static com.dgwave.osrs.OsrsConstants.PARAM_EXPIRED_TO;
import static com.dgwave.osrs.OsrsConstants.PARAM_OWNER_EMAIL;
import static com.dgwave.osrs.OsrsConstants.PARAM_PAGE_LIMIT;
import static com.dgwave.osrs.OsrsConstants.PARAM_PAGE_NUM;
import static com.dgwave.osrs.OsrsConstants.PARAM_TECH_EMAIL;

import java.util.Date;
import java.util.Map;

/**
 * @author Akber A. Choudhry
 */
public class GetDeletedDomains extends OsrsRequest {

	private String adminEmail;
	private String billingEmail;
	private String ownerEmail;
	private String techEmail;
	private Date deletedFrom;
	private Date deletedTo;
	private String domain;
	private Date expiredFrom;
	private Date expiredTo;
	private Integer pageLimit;
	private Integer pageNumber;
	
	/**
	 * Default Constructor
	 */
	public GetDeletedDomains() {
		super();
		setObject(OBJECT_DOMAIN);
		setAction(ACTION_GET_DELETED_DOMAINS);
		setModel(MODEL_DELETED_DOMAINS);
	}
	
	@Override
	public Map<String, Object> getAttributes() {
		addSimpleParam(PARAM_ADMIN_EMAIL, getAdminEmail());
		addSimpleParam(PARAM_BILLING_EMAIL, getBillingEmail());
		addSimpleParam(PARAM_OWNER_EMAIL, getOwnerEmail());
		addSimpleParam(PARAM_TECH_EMAIL, getTechEmail());
		addSimpleParam(PARAM_DELETED_FROM, cleanDate(getDeletedFrom()));
		addSimpleParam(PARAM_DELETED_TO, cleanDate(getDeletedTo()));
		addSimpleParam(PARAM_DOMAIN, getDomain());
		addSimpleParam(PARAM_EXPIRED_FROM, cleanDate(getDeletedFrom()));
		addSimpleParam(PARAM_EXPIRED_TO, cleanDate(getDeletedTo()));
		if (this.pageLimit != null && this.pageLimit != 0) {
			addSimpleParam(PARAM_PAGE_LIMIT, getPageLimit().toString());
			addSimpleParam(PARAM_PAGE_NUM, getPageNumber().toString());
		}
		return getSimpleParams();
	}

	public String getAdminEmail() {
		return adminEmail;
	}

	public void setAdminEmail(String adminEmail) {
		this.adminEmail = adminEmail;
	}

	public String getBillingEmail() {
		return billingEmail;
	}

	public void setBillingEmail(String billingEmail) {
		this.billingEmail = billingEmail;
	}

	public String getOwnerEmail() {
		return ownerEmail;
	}

	public void setOwnerEmail(String ownerEmail) {
		this.ownerEmail = ownerEmail;
	}

	public String getTechEmail() {
		return techEmail;
	}

	public void setTechEmail(String techEmail) {
		this.techEmail = techEmail;
	}

	public Date getDeletedFrom() {
		return deletedFrom;
	}

	public void setDeletedFrom(Date deletedFrom) {
		this.deletedFrom = deletedFrom;
	}

	public Date getDeletedTo() {
		return deletedTo;
	}

	public void setDeletedTo(Date deletedTo) {
		this.deletedTo = deletedTo;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public Date getExpiredFrom() {
		return expiredFrom;
	}

	public void setExpiredFrom(Date expiredFrom) {
		this.expiredFrom = expiredFrom;
	}

	public Date getExpiredTo() {
		return expiredTo;
	}

	public void setExpiredTo(Date expiredTo) {
		this.expiredTo = expiredTo;
	}

	public Integer getPageLimit() {
		return pageLimit;
	}

	public void setPageLimit(Integer pageLimit) {
		this.pageLimit = pageLimit;
	}

	public Integer getPageNumber() {
		return pageNumber;
	}

	public void setPageNumber(Integer pageNumber) {
		this.pageNumber = pageNumber;
	}

}
