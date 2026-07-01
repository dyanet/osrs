/*
 * Copyright 2012-2026, Dyanet Inc., Akber A. Choudhry,
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
 * An OpenSRS {@code dt_array} holds an ordered list of {@code item} elements,
 * each of which typically wraps a {@code dt_assoc} describing one record
 * (for example, a single deleted domain).
 *
 * @author Akber Choudhry
 */
@JacksonXmlRootElement(localName = "dt_array")
public class DtArray {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "item")
    private List<Item> items;

    public List<Item> getItems() {
        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    // For backward compatibility with the existing API
    @JsonIgnore
    public List<Object> getElements() {
        List<Object> elements = new ArrayList<>();
        if (items != null) {
            elements.addAll(items);
        }
        return elements;
    }

    public void addElement(Object element) {
        if (element instanceof Item) {
            addItem((Item) element);
        }
    }

    public void addItem(Item obj) {
        getItems().add(obj);
    }
}