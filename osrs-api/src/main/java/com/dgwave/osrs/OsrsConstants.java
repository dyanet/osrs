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

package com.dgwave.osrs;

/**
 * Constants used in XML messages to OpenSRS
 * 
 * @author Akber Choudhry
 */
public interface OsrsConstants {
	
	//Objects
	public static final String OBJECT_DOMAIN = "domain";
	public static final String OBJECT_BALANCE = "balance";
	
	//Actions
	public static final String ACTION_BELONGS_TO_RSP = "belongs_to_rsp";
	public static final String ACTION_GET_BALANCE = "get_balance";
	public static final String ACTION_GET_DELETED_DOMAINS = "get_deleted_domains";
	
	
	//request params
	public static final String PARAM_DOMAIN = "domain";
	public static final String PARAM_REGISTRANT_IP= "registrant_ip";
	public static final String PARAM_OWNER_EMAIL= "owner_email";
	public static final String PARAM_ADMIN_EMAIL= "admin_email";
	public static final String PARAM_BILLING_EMAIL= "billing_email";
	public static final String PARAM_TECH_EMAIL= "tech_email";
	public static final String PARAM_DELETED_FROM= "del_from";
	public static final String PARAM_DELETED_TO= "del_to";
	public static final String PARAM_EXPIRED_FROM= "exp_from";
	public static final String PARAM_EXPIRED_TO= "exp_to";
	public static final String PARAM_PAGE_LIMIT= "limit";
	public static final String PARAM_PAGE_NUM= "page";
	
	//Response Objects
	public static final String MODEL_DOMAIN = "Domain";
	public static final String MODEL_BALANCE = "Balance";
	public static final String MODEL_DELETED_DOMAINS = "DeletedDomains";
	
	// Response keys
	public static final String KEY_BELONGS_TO_RSP = "belongs_to_rsp";
	public static final String KEY_DOMAIN_EXPIRY_DATE = "domain_expdate";
	public static final String KEY_BALANCE = "balance";
	public static final String KEY_HOLD_BALANCE = "hold_balance";
	public static final String KEY_DELETE_DOMAINS = "del_domains";
	public static final String KEY_PAGE_SIZE = "page_size";
	public static final String KEY_TOTAL = "total";
	public static final String KEY_DELETE_DATE = "delete_date";
	public static final String KEY_DELETE_DATE_EPOCH = "delete_date_epoch";
	public static final String KEY_EXPIRE_DATE = "delete_date";
	public static final String KEY_EXPIRE_DATE_EPOCH = "delete_date_epoch";	
	public static final String KEY_NAME= "name";
	public static final String KEY_REASON= "reason";
}
