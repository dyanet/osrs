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

package com.dgwave.osrs.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * @author Akber Choudhry
 */

@Generated(value = { "From JAXB, modified after generation" })
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "dtAass"
})
@XmlRootElement(name = "data_block")
public class DataBlock {

    @XmlElements({
        @XmlElement(name = "dt_assoc", required = true, type = DtAssoc.class),
        @XmlElement(name = "dt_array", required = true, type = DtArray.class),
        @XmlElement(name = "dt_scalar", required = true, type = DtScalar.class),
        @XmlElement(name = "dt_scalarref", required = true, type = DtScalarref.class)
    })

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
    
}
