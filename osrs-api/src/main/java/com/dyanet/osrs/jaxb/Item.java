/*
 * Copyright 2012-2023, Dyanet Inc., Akber A. Choudhry,
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


package com.dyanet.osrs.jaxb;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.processing.Generated;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlMixed;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.NormalizedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * @author Akber Choudhry
 */

@Generated(value = { "From JAXB, modified after generation" })
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "dtAass"   
})
@XmlRootElement(name = "item")
public class Item {

    @XmlAttribute(required = true)
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String key;
    
    @XmlAttribute(name = "class")
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String clazz;

    /**
     * Gets the value of the key property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */

    public String getKey() {
        return key;
    }

    /**
     * Sets the value of the key property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */

    public void setKey(String value) {
        this.key = value;
    }

    /**
     * Gets the value of the clazz property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */

    public String getClazz() {
        return clazz;
    }

    /**
     * Sets the value of the clazz property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */

    public void setClazz(String value) {
        this.clazz = value;
    }


    @XmlAnyElement(lax=true)
    @XmlMixed

    protected List<Object> dtAass;

    /**
     * Gets the value of the dtAass property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the dtAass property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDtAass().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DtAssoc }
     * {@link DtArray }
     * {@link DtScalar }
     * {@link DtScalarref }
     * 
     * 
     */

    public List<Object> getDtAass() {
        if (dtAass == null) {
            dtAass = new ArrayList<Object>();
        }
        return this.dtAass;
    }

    public void addDtAass(Object obj) {
      getDtAass().add(obj);
    }
    
    public void addDtAssoc(DtAssoc obj) {
      getDtAass().add(obj);
    }
    
    public void addDtArray(DtArray obj) {
      getDtAass().add(obj);
    }
    
    public void addDtScalar(DtScalar obj) {
      getDtAass().add(obj);
    }
    
    public void addDtScalarref(DtScalarref obj) {
      getDtAass().add(obj);
    }    

    public void setStringValue(String obj) {
      getDtAass().add(obj);
    }
    
    public String getStringValue() {
      for (Iterator<Object> iterator = dtAass.iterator(); iterator.hasNext();) {
      Object type = (Object) iterator.next();
      if (type instanceof String) {
        return (String) type;
        
      } else {
        return null;
      }
    }
      return null;
    }   
}
