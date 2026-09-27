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

package com.dyanet.osrs.domains;

import java.time.Instant;

/**
 * One entry of {@code GET_DELETED_DOMAINS}.
 *
 * @param name       the domain
 * @param deletedAt  when it was deleted ({@code delete_date_epoch})
 * @param expiredAt  when it expired ({@code expiredate_epoch}), or {@code null}
 * @param reason     OpenSRS's reason: {@code Auction}, {@code By-Request}, {@code Expired},
 *                   {@code Historical} or {@code Transferred}
 */
public record DeletedDomain(String name, Instant deletedAt, Instant expiredAt, String reason) {
}
