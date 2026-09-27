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

import java.math.BigDecimal;

/**
 * The price of a domain operation ({@code DOMAIN GET_PRICE}), in the reseller account's
 * currency. It includes the OpenSRS and ICANN fees.
 *
 * @param domain          the domain priced
 * @param period          the number of years priced
 * @param type            the operation priced
 * @param price           the price
 * @param registryPremium whether the registry treats it as a premium name; {@code null} when
 *                        not reported (not premium)
 * @param premiumGroup    the registry's premium group, or {@code null}
 */
public record PriceQuote(String domain, int period, PriceType type, BigDecimal price,
        Boolean registryPremium, String premiumGroup) {

    /** What a price is for; sent as {@code reg_type}. */
    public enum PriceType {
        /** A new registration ({@code new}, the default). */
        NEW,
        /** A renewal ({@code renewal}). */
        RENEWAL,
        /** A transfer in ({@code transfer}). */
        TRANSFER,
        /** A trade, i.e. a change of registrant ({@code trade}). */
        TRADE;

        String wireValue() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
