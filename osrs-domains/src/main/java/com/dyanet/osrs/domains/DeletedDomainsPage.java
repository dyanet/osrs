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

import java.util.List;

/**
 * One page of {@code GET_DELETED_DOMAINS} results.
 *
 * @param total    how many deleted domains match in all
 * @param pageSize the page size OpenSRS used
 * @param page     the page returned, as OpenSRS numbers it
 * @param domains  the entries on this page
 */
public record DeletedDomainsPage(int total, int pageSize, int page, List<DeletedDomain> domains) {
}
