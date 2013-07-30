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

/**
 * Runtime Exception that wraps other underlying exceptions
 * 
 * @author Akber A. Choudhry
 */
public class OsrsException extends RuntimeException {

	/**
	 * Please regenerate after every save
	 */
	private static final long serialVersionUID = 4249866493131804800L;

	/**
	 * @param msg
	 */
	public OsrsException(String msg) {
		super(msg);
	}

	/**
	 * @param exception
	 */
	public OsrsException(Throwable exception) {
		super(exception);
	}

	/**
	 * @param msg
	 * @param exception
	 */
	public OsrsException(String msg, Throwable exception) {
		super(msg, exception);
	}

}
