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


import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Akber Choudhry
 */
@JacksonXmlRootElement(localName = "dt_array")
public class DtArray {

    @JacksonXmlElementWrapper(useWrapping = false)
    private List<Object> elements;

    public List<Object> getElements() {
        if (elements == null) {
            elements = new ArrayList<>();
        }
        return elements;
    }

    public void setElements(List<Object> elements) {
        this.elements = elements;
    }

    public void addElement(Object element) {
        getElements().add(element);
    }
    
    public void addDtAssoc(DtAssoc obj) {
        getElements().add(obj);
    }
    
    public void addDtArray(DtArray obj) {
        getElements().add(obj);
    }
    
    public void addDtScalar(DtScalar obj) {
        getElements().add(obj);
    }
    
    public void addDtScalarref(DtScalarref obj) {
        getElements().add(obj);
    }
    
    public void addItem(Item obj) {
        getElements().add(obj);
    }
}