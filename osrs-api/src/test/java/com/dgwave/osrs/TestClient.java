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

import static com.dgwave.osrs.Config.OSRSDIR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.dgwave.osrs.jaxb.DtAssoc;
import com.dgwave.osrs.jaxb.Item;
import com.dgwave.osrs.jaxb.OPSEnvelope;
import com.dgwave.osrs.model.Domain;
import com.dgwave.osrs.req.BelongsToRsp;
import com.dgwave.osrs.resp.OsrsResponse;

public class TestClient {

	OsrsClient client;
	
	@Before
	public void setUp() throws Exception {
		client = OsrsClient.getInstance(false);
	}

	@After
	public void tearDown() throws Exception {
		client = null;
	}
	
	
	@Test
	public void testGetEmptyEnvelope() {
		try {
			OPSEnvelope evp  = client.getEmptyEnvelope();
			assertNotNull(evp);
			assertEquals("Version", "0.9", evp.getHeader().getVersion());
			
		} catch (OsrsException e) {
			e.printStackTrace();
			fail ("Error while creating empty envelope");
		}
	}
	
	@Test
	public void testCreateEnvelope() {
		try {
			String sampleResponse = "<?xml version='1.0' encoding='UTF-8' standalone='no'?>\n" +
			"<!DOCTYPE OPS_envelope SYSTEM '" + OSRSDIR + "/ops.dtd'>\n" +
			"<OPS_envelope>\n" +
			"<header>\n" +
			"<version>0.9</version>\n" +
			"</header>\n" +
			"<body>\n" +
			"<data_block>\n" +
			"<dt_assoc>\n" +
			"<item key=\"protocol\">XCP</item>\n" +
			"<item key=\"action\">REPLY</item>\n" +
			"<item key=\"object\">DOMAIN</item>\n" +
			"<item key=\"attributes\">\n" +
			"<dt_assoc>\n" +
			"<item key=\"belongs_to_rsp\">1</item>\n" +
			"<item key=\"domain_expdate\">2007-08-26 11:40:14</item>\n" +
			"</dt_assoc>\n" +
			"</item>\n" +
			"<item key=\"response_text\">Query successful</item>\n" +
			"<item key=\"is_success\">1</item>\n" +
			"<item key=\"response_code\">200</item>\n" +
			"</dt_assoc>\n" +
			"</data_block>\n" +
			"</body>\n" +
			"</OPS_envelope>\n";
			OPSEnvelope evp  = client.createEnvelope(sampleResponse);
			assertNotNull(evp);
			assertEquals("Version ", "0.9", evp.getHeader().getVersion());
			Item item = (Item)((DtAssoc)evp.getBody().getDataBlock().getDtAass().get(0)).getDtAassi().get(5);
			assertEquals("Simple ", "is_success", item.getKey());
			assertNotNull(item.getStringValue());
			assertEquals("Value ", "1", item.getStringValue());
			
		} catch (OsrsException e) {
			e.printStackTrace();
			fail ("Error while creating envelope from string");
		}
	}

	@Test
	public void testGetString() {
		try {
			BelongsToRsp req = new BelongsToRsp();
			req.setDomain(new Domain("dgwave.com"));
			String togo = client.getString(req);
			assertNotNull(togo);
		} catch (OsrsException e) {
			e.printStackTrace();
			fail ("Error while creating string from request");
		}
	}
	
	@Test
	public void testSendReceive() {
		try {
			BelongsToRsp req = new BelongsToRsp();
			req.setDomain(new Domain("dgwave.com"));			
			OsrsResponse response = client.sendReceive(req);
			assertNotNull(response);
			System.out.println(response.getAction());
			System.out.println(response.getObject());
			System.out.println(response.getErrorCode());
			System.out.println(response.getErrorMessage());
			
		} catch (OsrsException e) {
			e.printStackTrace();
			fail ("Error while creating string from request");
		}
	}
}
