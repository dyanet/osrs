/*
 * Copyright 2012-2023, Dyanet Inc., Akber A. Choudhry,
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.dyanet.osrs.model.Balance;
import com.dyanet.osrs.model.DeletedDomain;
import com.dyanet.osrs.req.GetBalance;
import com.dyanet.osrs.req.GetDeletedDomains;
import com.dyanet.osrs.resp.BalanceResponse;
import com.dyanet.osrs.resp.DeletedDomainsResponse;
import com.dyanet.osrs.resp.OsrsResponse;


public class TestRequests {

    OsrsClient client;
    
    @BeforeEach
    public void setUp() throws Exception {
        client = OsrsClient.getInstance(false);
    }

    @AfterEach
    public void tearDown() throws Exception {
        client = null;
    }

    @Test
    public void testGetBalance() {
        try {
            GetBalance req = new GetBalance();
            req.setRegistrantIp(null);            
            OsrsResponse response = client.sendReceive(req);
            assertNotNull(response);
            System.out.println(response);
            
            Balance balance = ((BalanceResponse)response).getBalance();
            System.out.println("Balance is: " + balance.getBalance());
            System.out.println("Hold Balance is: " + balance.getHoldBalance());
            
        } catch (OsrsException e) {
            e.printStackTrace();
            fail ("Error while creating string from request");
        }
    }
    
    @Test
    public void testGetDeleteDomains() {
        try {
            GetDeletedDomains req = new GetDeletedDomains();
            DateFormat dfm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            req.setDeletedTo(dfm.parse("2031-09-04 13:00:00"));
            OsrsResponse response = client.sendReceive(req);
            assertNotNull(response);
            System.out.println(response);
            
            List<DeletedDomain> delDomainList = ((DeletedDomainsResponse)response).getDeletedDomains();
            System.out.println("Number in deleted domain list is: " + delDomainList.size());
            if (delDomainList.size() >0) System.out.println("First deleted domain is: " + delDomainList.get(0));
            
        } catch (Exception e) {
            e.printStackTrace();
            fail ("Error while creating string from request");
        }
    }    
}
