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

package com.dyanet.osrs;

import java.time.LocalDateTime;

/**
 * Whether a domain is managed by the calling reseller ({@code BELONGS_TO_RSP}).
 *
 * @param domain     the domain checked
 * @param belongs    {@code true} if it belongs to this reseller
 * @param expiryDate the expiry date and time as OpenSRS reports it (no time zone given), when
 *                   {@code belongs} is {@code true}; otherwise {@code null}
 */
public record RspOwnership(String domain, boolean belongs, LocalDateTime expiryDate) {
}
