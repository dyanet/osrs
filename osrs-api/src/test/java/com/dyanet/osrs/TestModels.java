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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Date;

import org.junit.jupiter.api.Test;

import com.dyanet.osrs.model.Balance;
import com.dyanet.osrs.model.DeletedDomain;
import com.dyanet.osrs.model.Domain;
import com.dyanet.osrs.model.Registry;

/**
 * Offline coverage for the plain data models and the {@link OsrsException}.
 */
public class TestModels {

    @Test
    public void domainConvenienceConstructorSetsName() {
        Domain d = new Domain("example.com");
        assertEquals("example.com", d.getName());
    }

    @Test
    public void domainAccessorsRoundTrip() {
        Domain d = new Domain();
        Date created = new Date(1_000_000L);
        Date expires = new Date(2_000_000L);
        d.setName("dgwave.com");
        d.setRegistry(Registry.COM);
        d.setBelongsToRsp(true);
        d.setAutoRenew(true);
        d.setLetExpire(true);
        d.setSponsoringRsp("dyanet");
        d.setRegistryCreateDate(created);
        d.setExpireDate(expires);

        assertEquals("dgwave.com", d.getName());
        assertSame(Registry.COM, d.getRegistry());
        assertTrue(d.isBelongsToRsp());
        assertTrue(d.isAutoRenew());
        assertTrue(d.isLetExpire());
        assertEquals("dyanet", d.getSponsoringRsp());
        assertEquals(created, d.getRegistryCreateDate());
        assertEquals(expires, d.getExpireDate());
    }

    @Test
    public void balanceAccessorsRoundTrip() {
        Balance b = new Balance();
        b.setBalance(new BigDecimal("10000"));
        b.setHoldBalance(new BigDecimal("0.00"));
        assertEquals(new BigDecimal("10000"), b.getBalance());
        assertEquals(new BigDecimal("0.00"), b.getHoldBalance());
    }

    @Test
    public void deletedDomainAccessorsRoundTrip() {
        DeletedDomain dd = new DeletedDomain();
        Date del = new Date(3_000_000L);
        Date exp = new Date(4_000_000L);
        dd.setName("gone.com");
        dd.setReason("expired");
        dd.setDeleteDate(del);
        dd.setDeleteDateEpoch(3_000L);
        dd.setExpireDate(exp);
        dd.setExpireDateEpoch(4_000L);

        assertEquals("gone.com", dd.getName());
        assertEquals("expired", dd.getReason());
        assertEquals(del, dd.getDeleteDate());
        assertEquals(3_000L, dd.getDeleteDateEpoch());
        assertEquals(exp, dd.getExpireDate());
        assertEquals(4_000L, dd.getExpireDateEpoch());
    }

    @Test
    public void registryEnumExposesDescriptions() {
        assertEquals("Global", Registry.COM.getDesc());
        assertEquals("Canada", Registry.CA.getDesc());
        assertEquals("Australia", Registry.AU.getDesc());
        assertSame(Registry.NET, Registry.valueOf("NET"));
        assertEquals(5, Registry.values().length);
    }

    @Test
    public void osrsExceptionConstructors() {
        OsrsException m = new OsrsException("boom");
        assertEquals("boom", m.getMessage());

        Throwable cause = new IllegalStateException("root");
        OsrsException wrapped = new OsrsException(cause);
        assertSame(cause, wrapped.getCause());

        OsrsException both = new OsrsException("boom", cause);
        assertEquals("boom", both.getMessage());
        assertSame(cause, both.getCause());
        assertNotNull(both);
        assertFalse(both instanceof Exception && !(both instanceof RuntimeException));
    }
}
