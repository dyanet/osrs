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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.dyanet.osrs.model.Domain;
import com.dyanet.osrs.req.BelongsToRsp;
import com.dyanet.osrs.req.GetBalance;
import com.dyanet.osrs.req.GetDeletedDomains;

/**
 * Offline coverage for request objects: action/object/model wiring and the
 * attribute maps they build (no network involved).
 */
public class TestRequestBuilding {

    @Test
    public void belongsToRspWiringAndAttributes() {
        BelongsToRsp req = new BelongsToRsp();
        assertEquals("domain", req.getObject());
        assertEquals("belongs_to_rsp", req.getAction());
        assertEquals("Domain", req.getModel());

        req.setDomain(new Domain("dgwave.com"));
        Map<String, Object> attrs = req.getAttributes();
        assertEquals("dgwave.com", attrs.get("domain"));
    }

    @Test
    public void getBalanceWiringAndAttributes() {
        GetBalance req = new GetBalance();
        assertEquals("balance", req.getObject());
        assertEquals("get_balance", req.getAction());
        assertEquals("Balance", req.getModel());

        // No registrant IP set -> the blank param is skipped.
        assertTrue(req.getAttributes().isEmpty());

        req.setRegistrantIp("203.0.113.7");
        assertEquals("203.0.113.7", req.getAttributes().get("registrant_ip"));
    }

    @Test
    public void getDeletedDomainsWiring() {
        GetDeletedDomains req = new GetDeletedDomains();
        assertEquals("domain", req.getObject());
        assertEquals("get_deleted_domains", req.getAction());
        assertEquals("DeletedDomains", req.getModel());
    }

    @Test
    public void blankParamsAreNotAdded() {
        GetDeletedDomains req = new GetDeletedDomains();
        req.setDomain("");
        assertFalse(req.getAttributes().containsKey("domain"), "empty domain must be skipped");
    }

    @Test
    public void cleanDateClampsFutureDatesToTheHorizon() throws Exception {
        SimpleDateFormat ymd = new SimpleDateFormat("yyyy-MM-dd");
        // The request clamps any date beyond its internal horizon (~2031).
        String horizon = ymd.format(new Date(1924991999000L));

        GetDeletedDomains req = new GetDeletedDomains();
        req.setDeletedFrom(ymd.parse("1999-06-15"));    // before the horizon, passes through
        req.setDeletedTo(ymd.parse("3000-01-01"));      // far future, gets clamped

        Map<String, Object> attrs = req.getAttributes();
        assertEquals("1999-06-15", attrs.get("del_from"));
        assertEquals(horizon, attrs.get("del_to"));
    }

    @Test
    public void pageLimitDrivesPaginationParams() {
        GetDeletedDomains req = new GetDeletedDomains();
        assertFalse(req.getAttributes().containsKey("limit"), "no limit until set");

        req.setPageLimit(25);
        req.setPageNumber(3);
        Map<String, Object> attrs = req.getAttributes();
        assertEquals("25", attrs.get("limit"));
        assertEquals("3", attrs.get("page"));
    }
}
