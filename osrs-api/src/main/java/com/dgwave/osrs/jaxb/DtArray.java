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
 * @author Akber A. Choudhry
 */

@Generated(value = { "From JAXB, modified after generation" })
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "dtAassi"
})
@XmlRootElement(name = "dt_array")
public class DtArray {

    @XmlElements({
        @XmlElement(name = "dt_assoc", type = DtAssoc.class),
        @XmlElement(name = "dt_array", type = DtArray.class),
        @XmlElement(name = "dt_scalar", type = DtScalar.class),
        @XmlElement(name = "dt_scalarref", type = DtScalarref.class),
        @XmlElement(name = "item", type = Item.class)
    })
    protected List<Object> dtAassi;

    /**
     * Gets the value of the dtAassi property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the dtAassi property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDtAassi().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DtAssoc }
     * {@link DtArray }
     * {@link DtScalar }
     * {@link DtScalarref }
     * {@link Item }
     * 
     * 
     */

    public List<Object> getDtAassi() {
        if (dtAassi == null) {
            dtAassi = new ArrayList<Object>();
        }
        return this.dtAassi;
    }

}
