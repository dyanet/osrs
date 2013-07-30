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

import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.dgwave.osrs.jaxb package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 * @author Akber Choudhry
 */

@Generated(value = { "From JAXB, modified after generation" })
@XmlRegistry
public class ObjectFactory {


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.dgwave.osrs.jaxb
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link DtScalarref }
     * 
     */
    public DtScalarref createDtScalarref() {
        return new DtScalarref();
    }

    /**
     * Create an instance of {@link DtArray }
     * 
     */
    public DtArray createDtArray() {
        return new DtArray();
    }

    /**
     * Create an instance of {@link DataBlock }
     * 
     */
    public DataBlock createDataBlock() {
        return new DataBlock();
    }

    /**
     * Create an instance of {@link Header }
     * 
     */
    public Header createHeader() {
        return new Header();
    }

    /**
     * Create an instance of {@link Item }
     * 
     */
    public Item createItem() {
        return new Item();
    }

    /**
     * Create an instance of {@link DtAssoc }
     * 
     */
    public DtAssoc createDtAssoc() {
        return new DtAssoc();
    }

    /**
     * Create an instance of {@link Body }
     * 
     */
    public Body createBody() {
        return new Body();
    }

    /**
     * Create an instance of {@link OPSEnvelope }
     * 
     */
    public OPSEnvelope createOPSEnvelope() {
        return new OPSEnvelope();
    }

    /**
     * Create an instance of {@link DtScalar }
     * 
     */
    public DtScalar createDtScalar() {
        return new DtScalar();
    }

}
