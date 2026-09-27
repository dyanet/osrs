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

import java.math.BigDecimal;

/**
 * The reseller account's funds ({@code GET_BALANCE}).
 *
 * @param balance     total funds, including the amount held for pending transactions
 * @param holdBalance the part of {@code balance} held for pending transactions
 */
public record Balance(BigDecimal balance, BigDecimal holdBalance) {

    /**
     * @return what can be spent now: {@code balance - holdBalance}
     */
    public BigDecimal available() {
        return balance.subtract(holdBalance);
    }
}
