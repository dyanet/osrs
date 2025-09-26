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

/**
 * Factory class for creating Jackson XML objects
 * 
 * @author Akber Choudhry
 */
public class ObjectFactory {

    public OPSEnvelope createOPSEnvelope() {
        return new OPSEnvelope();
    }

    public Header createHeader() {
        return new Header();
    }

    public Body createBody() {
        return new Body();
    }

    public DataBlock createDataBlock() {
        return new DataBlock();
    }

    public DtAssoc createDtAssoc() {
        return new DtAssoc();
    }

    public DtArray createDtArray() {
        return new DtArray();
    }

    public DtScalar createDtScalar() {
        return new DtScalar();
    }

    public DtScalarref createDtScalarref() {
        return new DtScalarref();
    }

    public Item createItem() {
        return new Item();
    }
}