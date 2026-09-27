OSRS
====
[![CI](https://github.com/dyanet/osrs/actions/workflows/ci.yml/badge.svg)](https://github.com/dyanet/osrs/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/dyanet/osrs/branch/master/graph/badge.svg)](https://codecov.io/gh/dyanet/osrs)

A basic framework for connecting to the OpenSRS registry, executing requests and unmarshalling responses into POJOs that can then be consumed in your application.

OpenSRS Java Client and API

Documentation: https://dyanet.com/osrs/

API
---

1. Configuration of multiple environments, for example,  `test` and `prod`
2. Jackson Marshalling and Unmarshalling of XML and envelopes
3. Public certificate for SSL and MD5 signature
3. Request and Response model for expansion to other APIs
4. A few basic tests
5. High-performance Apache HTTPClient

Installation
------------
Maven Central (from 0.9.4):

```xml
<dependency>
  <groupId>com.dyanet.osrs</groupId>
  <artifactId>osrs-api</artifactId>
  <version>0.9.4</version>
</dependency>
```

Requires Java 21+.

Usage
-----
- Add the dependency above, or check out the project and build with Maven
- Configuration is a simple `.properties` file, selected in this order:
  1. `-Dosrs.config=<path-or-classpath-resource>` &mdash; explicit JVM override
  2. the `OSRS_CONFIG` environment variable, same semantics &mdash; convenient for containers
     and `.env`-style deployments that export env vars instead of passing `-D` flags
     (for example, mount a secret at `/run/secrets/osrs.properties` and
     `export OSRS_CONFIG=/run/secrets/osrs.properties`)
  3. otherwise `osrs-<env>.properties` on the classpath, where `<env>` is `-Dosrs.env`
     (default `test`); `osrs-test.properties` and `osrs-prod.properties` templates ship
     in `osrs-api/src/main/resources`
- Set your OpenSRS key and username (`osrs.userName`, `osrs.key`) in the chosen configuration file
- From within your application,
 - get an instance of the client `OsrsClient.getInstance(false);`
 - create a request object and set its properties `GetBalance req = new GetBalance(); req.setRegistrantIp(null);`
 - read the response `OsrsResponse response = client.sendReceive(req); assertNotNull(response); System.out.println(response);`
 - cast to POJO and read response properties `Balance balance = ((BalanceResponse)response).getBalance();`

Testing
-------
- `mvn test` (or `mvn verify`) runs the fast, offline unit test suite and enforces a minimum
  instruction coverage bar via JaCoCo (see `osrs-api/pom.xml`)
- Tests tagged `integration` exercise the live OpenSRS `test` registry over the network and are
  excluded by default. Run them with `mvn verify -Pintegration`, pointing `OSRS_CONFIG` at a
  **private** copy of `osrs-test.properties` with your horizon credentials. Never commit a real
  key: this repository is public. In CI, run the **CI** workflow manually (Actions → CI → Run
  workflow); it uses the `OPENSRS_API_KEY` secret, and your key's IP access rules must allow the runner.

Releasing
---------
Bump the version in both POMs (no `-SNAPSHOT`) in a PR and merge it to `master`. The **Release**
workflow builds, tests, signs and uploads the release to the Sonatype Central Portal, then tags
`v<version>`. The deployment is validated but **not published** until a maintainer clicks
**Publish** at [central.sonatype.com](https://central.sonatype.com) → Deployments. See
[AGENTS.md](AGENTS.md#releasing-to-maven-central) for the one-time setup.

License
-------
[Apache License 2.0](LICENSE)

