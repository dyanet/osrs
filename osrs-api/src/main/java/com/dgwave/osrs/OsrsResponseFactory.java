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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.dgwave.osrs.jaxb.DtArray;
import com.dgwave.osrs.jaxb.DtAssoc;
import com.dgwave.osrs.jaxb.Item;
import com.dgwave.osrs.jaxb.OPSEnvelope;
import com.dgwave.osrs.req.OsrsRequest;
import com.dgwave.osrs.resp.OsrsResponse;

/**
 * Deep dive into the envelope and create the right implementation
 * 
 * @author Akber Choudhry
 *
 */
public class OsrsResponseFactory {

	public OsrsResponse createResponse(OsrsRequest request, OPSEnvelope resp) throws OsrsException {
		
		OsrsResponse response = createOsrsResponse(request.getModel());
		
		if (resp == null) { //nothing came
			response.setError(true);
			return response;
		}
		// find error code and populate
		
		DtAssoc dt = (DtAssoc) resp.getBody().getDataBlock().getDtAass().get(0);
		List<Object> items = dt.getDtAassi();
		for (Iterator<Object> iterator = items.iterator(); iterator.hasNext();) {
			Object object = (Object) iterator.next();
			if (object instanceof Item) {
				Item item = (Item) object;
				if ("action".equals(item.getKey())) {
					response.setAction(item.getStringValue());
				} else if ("object".equals(item.getKey())) {
					response.setObject(item.getStringValue());
				} else if ("response_text".equals(item.getKey())) {
					response.setErrorMessage(item.getStringValue());
				} else if ("response_code".equals(item.getKey())) {
					try {
						response.setErrorCode(Integer.parseInt(item.getStringValue()));
					} catch (NumberFormatException e) {
						response.setErrorCode(999);
					}
				} else if ("is_success".equals(item.getKey())) {
					int tf = Integer.parseInt(item.getStringValue());
					response.setError(tf==1?false:true);
				} else if ("protocol".equals(item.getKey())) {
					response.setProtocol(item.getStringValue());
				}else  {
					populate(response, item);
				}
			}
		}
		
		
		return response;
	}

	public void populate(OsrsResponse response, Item outerItem) {
		if ("attributes".equals(outerItem.getKey())) { // look or \n
						
			processContainerList(response, outerItem.getDtAass(),null);
		}	
	}

	
	private void processContainerList(OsrsResponse response, List<Object> list, String level) {

		cleanList(list); // because of mixed content

		for (Iterator<Object> it2 = list.iterator(); it2.hasNext();) {
			Object obj2 = it2.next();
			if (obj2 instanceof DtAssoc) {
				response.populate(
						processItemList(response,
								((DtAssoc)obj2).getDtAassi()
								), level);
			} else if (obj2 instanceof DtArray) {
				response.populate(
						processItemList(response, ((DtArray)obj2).getDtAassi()
								),level);
			} else {
				// ignore - mostly \n
			}					
		}
	}

	private Map<String,String> processItemList(OsrsResponse response, List<Object> items) {
		cleanList(items);
		Map<String,String> assocValues = new HashMap<String, String>();
		for (Iterator<Object> it3 = items.iterator(); it3.hasNext();) {
			Object obj3 = (Object) it3.next();
			if (obj3 instanceof Item) {
				Item item = (Item)obj3;
				String value = item.getStringValue();
				if (value != null) { // simple item value
					assocValues.put(item.getKey(), value);
				} else { // this is an element in one type of item
					processContainerList(response, item.getDtAass(),item.getKey());
				}
			}
		}
		return assocValues;	
	}

/*	private void processArray(Node item) {
		String key = ;
		NodeList array = item.getChildNodes();
		
	}*/


	private void cleanList(List<Object> list) {
		for (Iterator<Object> it1 = list.iterator(); it1.hasNext();) {
			Object obj = (Object) it1.next();
			if (obj instanceof String) {
				// remove - mostly \n
				it1.remove();
			} else {
				//good
			} 
		}
	}
	
	private OsrsResponse createOsrsResponse(String model) throws OsrsException {
		boolean isList = false;
		if (model.endsWith("s")) {
			isList = true;
		}
		
		try {
		  Class<?> clazz = Class.forName("com.dgwave.osrs.resp." + model + "Response");
		  Class<?> clayy;
		  if (isList) {
			  clayy = Class.forName("java.util.List");
		  } else {
			  clayy = Class.forName("com.dgwave.osrs.model." + model);
		  }
		  Method method = clazz.getDeclaredMethod("set"+model, clayy);
	      OsrsResponse r = (OsrsResponse)clazz.newInstance();
			if (r != null) {
				Object m = null;
				if (model.endsWith("s")) {
					m = new ArrayList<Object>();
				} else {
					m = clayy.newInstance(); // the model
				}
				if (method != null && m != null) {		
					method.invoke(r,m);
					return r;
				} else {
					return null;
				}
			} else {
				return null;
			}
		} catch (Exception e) {
			throw new OsrsException("Exception creating model and response", e);
		}
	}
}
