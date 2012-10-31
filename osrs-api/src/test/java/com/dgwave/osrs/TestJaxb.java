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

import static org.junit.Assert.fail;

import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.dgwave.osrs.jaxb.Body;
import com.dgwave.osrs.jaxb.DataBlock;
import com.dgwave.osrs.jaxb.DtAssoc;
import com.dgwave.osrs.jaxb.Header;
import com.dgwave.osrs.jaxb.Item;
import com.dgwave.osrs.jaxb.OPSEnvelope;
import com.dgwave.osrs.jaxb.ObjectFactory;

public class TestJaxb {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testCreateOPSEnvelope() {
		try {
			JAXBContext jc = JAXBContext.newInstance("com.dgwave.osrs.jaxb");
			ObjectFactory objFactory = new ObjectFactory();
			OPSEnvelope opsEnvelope = objFactory.createOPSEnvelope();
			Header header = objFactory.createHeader();
			header.setVersion("0.9");
			opsEnvelope.setHeader(header);
			
			Item domain = objFactory.createItem();
			domain.setKey("domain");
			domain.setStringValue("dgwave.com");
			
			DtAssoc attrs = objFactory.createDtAssoc();
			attrs.addItem(domain);
			Item attributes = objFactory.createItem();
			attributes.setKey("attributes");
			attributes.addDtAssoc(attrs);
			
			
			Item protocol = objFactory.createItem();
			protocol.setKey("protocol");
			protocol.setStringValue("XCP");
			Item action = objFactory.createItem();
			action.setKey("action");
			action.setStringValue("belongs_to_rsp");
			Item obj = objFactory.createItem();
			obj.setKey("object");
			obj.setStringValue("domain");
			
			DtAssoc top = objFactory.createDtAssoc();
			top.addItem(protocol);
			top.addItem(action);
			top.addItem(obj);
			top.addItem(attributes);
			
			DataBlock dBlock = objFactory.createDataBlock();
			dBlock.addDtAssoc(top);
			
			Body body = objFactory.createBody();
			body.setDataBlock(dBlock);
			
			opsEnvelope.setBody(body);
			
			Marshaller m = jc.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
			m.setProperty("com.sun.xml.internal.bind.xmlHeaders", 
            	"<?xml version='1.0' encoding='UTF-8' standalone='no'?>\n" +
            	"<!DOCTYPE OPS_envelope SYSTEM 'ops.dtd'>");
			
			m.marshal(opsEnvelope, System.out);

		} catch (JAXBException e) {
			e.printStackTrace();
			fail("JAXB exception in test");
			
		}
	}

	@Test
	public void testParseEnvelope() {
		String xml = "<?xml version='1.0' encoding='UTF-8' standalone='no'?>" +
"<!DOCTYPE OPS_envelope SYSTEM 'src/main/resources/ops.dtd'>" +
"<OPS_envelope>" +
"<header>" +
"<version>0.9</version>" +
"</header>" +
"<body>" +
"<data_block>" +
"<dt_assoc>" +
"<item key=\"protocol\">XCP</item>" +
"<item key=\"action\">REPLY</item>" +
"<item key=\"object\">DOMAIN</item>" +
"<item key=\"attributes\">" +
"<dt_assoc>" +
"<item key=\"belongs_to_rsp\">1</item>" +
"<item key=\"domain_expdate\">2007-08-26 11:40:14</item>" +
"</dt_assoc>" +
"</item>" +
"<item key=\"response_text\">Query successful</item>" +
"<item key=\"is_success\">1</item>" +
"<item key=\"response_code\">200</item>" +
"</dt_assoc>" +
"</data_block>" +
"</body>" +
"</OPS_envelope>";
		
		OPSEnvelope opsEnvelope = null;
		try {
			JAXBContext jc = JAXBContext.newInstance("com.dgwave.osrs.jaxb");
			Unmarshaller u = jc.createUnmarshaller();
			opsEnvelope = (OPSEnvelope)u.unmarshal(new StringReader(xml));
		} catch (Exception e) {
			e.printStackTrace();
			fail("unmarshalling error");
		}

		try {
			JAXBContext jc = JAXBContext.newInstance("com.dgwave.osrs.jaxb");
		Marshaller m = jc.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		//m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
		//m.setProperty("com.sun.xml.bind.xmlHeaders", 
        //	"<?xml version='1.0' encoding='UTF-8' standalone='no'?>\n"); 			
		//m.setProperty("com.sun.xml.bind.xmlHeaders", 
        //	"<!DOCTYPE OPS_envelope SYSTEM 'ops.dtd'>"); 
		
		m.marshal(opsEnvelope, System.out);		
		} catch (Exception e) {
			e.printStackTrace();
			fail("marshalling error");
		}
		
	}
}
