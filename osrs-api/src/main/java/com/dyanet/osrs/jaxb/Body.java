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

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * @author Akber Choudhry
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "dataBlock"
})
@XmlRootElement(name = "body")
public class Body {

    @XmlElement(name = "data_block", required = true)
    protected DataBlock dataBlock;

    /**
     * Gets the value of the dataBlock property.
     * 
     * @return
     *     possible object is
     *     {@link DataBlock }
     *     
     */

    public DataBlock getDataBlock() {
        return dataBlock;
    }

    /**
     * Sets the value of the dataBlock property.
     * 
     * @param value
     *     allowed object is
     *     {@link DataBlock }
     *     
     */

    public void setDataBlock(DataBlock value) {
        this.dataBlock = value;
    }

}
