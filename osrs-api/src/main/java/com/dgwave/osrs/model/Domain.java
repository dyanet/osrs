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
import java.util.List;

/**
 * @author Akber Choudhry
 */
public class Domain {
	String name;
	Registry registry;
	boolean belongsToRsp;
	boolean autoRenew;
	Date registryCreateDate;
	Date registryExpireDate;
	Date registryUpdateDate;
	String sponsoringRsp;
	Date expireDate;
	boolean letExpire;
	List<Contact> contactSet;
	List<NameServer> nameServerSet;
	List<ExtraInfo> extraInfo;
	
	public Domain() {
		super();
	}

	public Domain(String name) {
		this();
		setName(name);
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Registry getRegistry() {
		return registry;
	}
	public void setRegistry(Registry registry) {
		this.registry = registry;
	}
	public boolean isAutoRenew() {
		return autoRenew;
	}
	public void setAutoRenew(boolean autoRenew) {
		this.autoRenew = autoRenew;
	}
	public Date getRegistryCreateDate() {
		return registryCreateDate;
	}
	public void setRegistryCreateDate(Date registryCreateDate) {
		this.registryCreateDate = registryCreateDate;
	}
	public Date getRegistryExpireDate() {
		return registryExpireDate;
	}
	public void setRegistryExpireDate(Date registryExpireDate) {
		this.registryExpireDate = registryExpireDate;
	}
	public Date getRegistryUpdateDate() {
		return registryUpdateDate;
	}
	public void setRegistryUpdateDate(Date registryUpdateDate) {
		this.registryUpdateDate = registryUpdateDate;
	}
	public String getSponsoringRsp() {
		return sponsoringRsp;
	}
	public void setSponsoringRsp(String sponsoringRsp) {
		this.sponsoringRsp = sponsoringRsp;
	}
	public Date getExpireDate() {
		return expireDate;
	}
	public void setExpireDate(Date expireDate) {
		this.expireDate = expireDate;
	}
	public boolean isLetExpire() {
		return letExpire;
	}
	public void setLetExpire(boolean letExpire) {
		this.letExpire = letExpire;
	}
	public List<Contact> getContactSet() {
		return contactSet;
	}
	public void setContactSet(List<Contact> contactSet) {
		this.contactSet = contactSet;
	}
	public List<NameServer> getNameServerSet() {
		return nameServerSet;
	}
	public void setNameServerSet(List<NameServer> nameServerSet) {
		this.nameServerSet = nameServerSet;
	}
	public List<ExtraInfo> getExtraInfo() {
		return extraInfo;
	}
	public void setExtraInfo(List<ExtraInfo> extraInfo) {
		this.extraInfo = extraInfo;
	}

	public boolean isBelongsToRsp() {
		return belongsToRsp;
	}

	public void setBelongsToRsp(boolean belongsToRsp) {
		this.belongsToRsp = belongsToRsp;
	}
	
	
}
