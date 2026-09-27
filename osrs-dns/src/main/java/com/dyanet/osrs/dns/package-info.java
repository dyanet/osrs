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

/**
 * OpenSRS DNS commands (incubating, not published yet): DNS zones
 * ({@code CREATE_DNS_ZONE}, {@code GET_DNS_ZONE}, {@code SET_DNS_ZONE}, {@code DELETE_DNS_ZONE},
 * {@code FORCE_DNS_NAMESERVERS}), DNSSEC, and domain forwarding.
 *
 * <p>Like every command family, this module holds only the commands' knowledge: request
 * attributes, reply models and response-code mapping. Encoding, signing, sending and generic
 * errors come from {@code osrs-api}.
 */
package com.dyanet.osrs.dns;
