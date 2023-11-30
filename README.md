OSRS
====
A basic framework for connecting to the OpenSRS registry, executing requests and unmarshalling responses into POJOs
that can then be consumed in your application.

OpenSRS Java Client and API

Documentation: https://dyanet.com/osrs/

API
---

1. Configuration of multiple environments, for example,  `test` and `prod`
2. JAXB Marshalling and Unmarshalling of XML and envelopes
3. Public certificate for SSL and MD5 signature
3. Request and Response model for expansion to other APIs
4. A few basic tests
5. High-performance Apache HTTPClient

Usage
-----
- Checkout the project and build with Maven or download the distribution
- Copy the `config` folder to a folder of your choice
- Set the JVM system property `-Dosrs.config` property in your application to point to this folder
- Use the `activeConfig.xml` to point at the active configuration, for example, test or production
- Set your OpenSRS key and username in the test and/or production configuration
- From within your application,
 - get an instance of the client `OsrsClient.getInstance(false);`
 - create a request object and set its properties `GetBalance req = new GetBalance(); req.setRegistrantIp(null);`
 - read the response `OsrsResponse response = client.sendReceive(req); assertNotNull(response); System.out.println(response);`
 - cast to POJO and read response properties `Balance balance = ((BalanceResponse)response).getBalance();`

