/*
 * Copyright 2012-2025, Dyanet Inc., Akber A. Choudhry,
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

package com.dyanet.osrs.jackson;

import java.util.ArrayList;
import java.util.List;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Akber Choudhry
 */
@JacksonXmlRootElement(localName = "data_block")
public class DataBlock {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "dt_assoc")
    private List<DtAssoc> dtAssocs;

    public List<DtAssoc> getDtAssocs() {
        if (dtAssocs == null) {
            dtAssocs = new ArrayList<>();
        }
        return dtAssocs;
    }

    public void setDtAssocs(List<DtAssoc> dtAssocs) {
        this.dtAssocs = dtAssocs;
    }

    // For backward compatibility with the existing API
    @JsonIgnore
    public List<Object> getElements() {
        List<Object> elements = new ArrayList<>();
        if (dtAssocs != null) {
            elements.addAll(dtAssocs);
        }
        return elements;
    }
    
    public void addDtAssoc(DtAssoc obj) {
        getDtAssocs().add(obj);
    }
    
    public void addDtArray(DtArray obj) {
        // For now, we'll treat arrays as assocs for simplicity
        // This can be extended later if needed
    }
    
    public void addDtScalar(DtScalar obj) {
        // For now, we'll treat scalars as assocs for simplicity
        // This can be extended later if needed
    }
    
    public void addDtScalarref(DtScalarref obj) {
        // For now, we'll treat scalarrefs as assocs for simplicity
        // This can be extended later if needed
    }
}