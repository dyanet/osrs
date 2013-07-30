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

import java.util.Map;

import com.dgwave.osrs.model.Domain;

import static com.dgwave.osrs.OsrsConstants.*;

/**
 * @author Akber Choudhry
 *
 */
public class BelongsToRsp extends OsrsRequest {

	private Domain domain;
	
	/**
	 * Default Constructor
	 */
	public BelongsToRsp() {
		super();
		setObject(OBJECT_DOMAIN);
		setAction(ACTION_BELONGS_TO_RSP);
		setModel(MODEL_DOMAIN);
	}
	@Override
	public Map<String, Object> getAttributes() {
		addSimpleParam(PARAM_DOMAIN, getDomain().getName());
		return getSimpleParams();
	}

	public Domain getDomain() {
		return domain;
	}

	public void setDomain(Domain domain) {
		this.domain = domain;
	}
}
