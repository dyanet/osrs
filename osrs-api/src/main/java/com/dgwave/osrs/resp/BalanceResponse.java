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

import static com.dgwave.osrs.OsrsConstants.*;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;

import com.dgwave.osrs.model.Balance;

/**
 * @author Akber A. Choudhry
 */
public class BalanceResponse extends OsrsResponse {


	private Balance balance;
	
	/* (non-Javadoc)
	 * @see com.dgwave.osrs.resp.OsrsResponse#populate(Map<String, String> attrMap)
	 */
	@Override
	public void populate(Map<String, String> attrMap, String level) {
		BigDecimal bd = null;
		for (Iterator<String> it1 = attrMap.keySet().iterator(); it1.hasNext();) {
			String key = (String) it1.next();
				if (KEY_BALANCE.equals(key)) {
					bd = new BigDecimal(attrMap.get(key));
					getBalance().setBalance(bd);
				} else if (KEY_HOLD_BALANCE.equals(key)) {
					bd = new BigDecimal(attrMap.get(key));
					getBalance().setHoldBalance(bd);
				} 
			}
	}

	public Balance getBalance() {
		return balance;
	}

	public void setBalance(Balance balance) {
		this.balance = balance;
	}


}
