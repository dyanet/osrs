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

package com.dgwave.osrs.model;

import java.util.Date;

/**
 * @author Akber A. Choudhry
 */
public class DeletedDomain {
	
	private Date deleteDate;
	private Long deleteDateEpoch;
	private Date expireDate;
	private Long expireDateEpoch;
	private String name;
	private String reason;
	
	
	public DeletedDomain() {
		super();
	}


	public Date getDeleteDate() {
		return deleteDate;
	}


	public void setDeleteDate(Date deleteDate) {
		this.deleteDate = deleteDate;
	}


	public Long getDeleteDateEpoch() {
		return deleteDateEpoch;
	}


	public void setDeleteDateEpoch(Long deleteDateEpoch) {
		this.deleteDateEpoch = deleteDateEpoch;
	}


	public Date getExpireDate() {
		return expireDate;
	}


	public void setExpireDate(Date expireDate) {
		this.expireDate = expireDate;
	}


	public Long getExpireDateEpoch() {
		return expireDateEpoch;
	}


	public void setExpireDateEpoch(Long expireDateEpoch) {
		this.expireDateEpoch = expireDateEpoch;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public String getReason() {
		return reason;
	}


	public void setReason(String reason) {
		this.reason = reason;
	}
		
}
