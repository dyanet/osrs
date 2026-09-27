OSRS
====
[![CI](https://github.com/dyanet/osrs/actions/workflows/ci.yml/badge.svg)](https://github.com/dyanet/osrs/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/dyanet/osrs/branch/master/graph/badge.svg)](https://codecov.io/gh/dyanet/osrs)

Java client for the OpenSRS (Tucows) reseller XML (XCP) API.

Documentation: https://dyanet.com/osrs/

Artifacts
---------
All artifacts share the group `com.dyanet.osrs` and one version. Requires Java 21+.

| artifactId | What it covers |
|---|---|
| `osrs-api` | The base: XML envelope encoding/decoding (nested `dt_assoc`/`dt_array`), request signing, HTTPS transport, configuration, errors, retries, and the basic commands that belong to no family: domain `lookup`, account `balance`, `belongsToRsp`. Only runtime dependency: `slf4j-api`. |
| `osrs-domains` | Domain commands: deleted domains so far; registration, renewal and management to follow. |
| `osrs-parent` | Parent POM, also usable as a BOM to keep versions aligned. |

More command families (transfers, DNS) are on the way; this README will get the full list when
they are published.

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.dyanet.osrs</groupId>
      <artifactId>osrs-parent</artifactId>
      <version>1.0.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <dependency>
    <groupId>com.dyanet.osrs</groupId>
    <artifactId>osrs-domains</artifactId> <!-- brings in osrs-api -->
  </dependency>
</dependencies>
```

1.0.0 replaces the 0.9.x API (`OsrsClient.getInstance`, `GetBalance`, `OsrsResponse`...);
0.9.4 remains on Maven Central.

Usage
-----
```java
try (OsrsClient client = OsrsClient.builder()
        .config(OsrsConfig.test("your_reseller_username", apiKey))   // or OsrsConfig.live(...)
        .build()) {
    LookupResult r = client.lookup("example.com");                   // osrs-api
    Balance balance = client.balance();                              // osrs-api
    DeletedDomainsPage gone = Domains.on(client)                     // osrs-domains
        .deletedDomains(DeletedDomainsQuery.all());
}
```

Configuration can also come from a `.properties` file with `OsrsConfig.load()` (or
`OsrsClient.fromDefaultConfig()`), found in this order:
1. `-Dosrs.config=<path-or-classpath-resource>`;
2. the `OSRS_CONFIG` environment variable (handy for containers and mounted secrets);
3. `osrs-<env>.properties` on the classpath, where `<env>` is `-Dosrs.env` (default `test`).

Templates are in [`config/`](config). Keep the real file private: it holds your API key. The
test (`horizon.opensrs.net`) and live keys differ, and the live host only accepts allowlisted IPs.

Failures are unchecked exceptions: `OsrsApiException` (OpenSRS reported a failure; carries the
response code and text), with `OsrsAuthenticationException`, `OsrsUnavailableException` and
`DomainException` for known codes; `OsrsTransportException` and `OsrsProtocolException` when no
valid reply arrived.

Testing
-------
- `mvn verify -Pincubating` runs the offline unit tests of every module and enforces JaCoCo
  coverage minimums.
- In your own tests, `StubTransport` (in `osrs-api`) replaces the network: it records each
  request and answers from a script (`StubTransport.reply(...)` builds OpenSRS-shaped replies).
- Tests tagged `integration` call the live OpenSRS test environment and are excluded by default.
  Run them with `mvn verify -Pintegration`, pointing `OSRS_CONFIG` at a **private** copy of the
  test config with your horizon credentials. Never commit a real key: this repository is public.
  In CI, run the **CI** workflow manually (Actions → CI → Run workflow); it uses the
  `OPENSRS_API_KEY` secret, and your key's IP access rules must allow the runner.

Releasing
---------
Bump the version in the root POM and every module's parent reference (no `-SNAPSHOT`) in a PR and merge it to `master`. The **Release**
workflow builds, tests, signs and uploads the release to the Sonatype Central Portal, then tags
`v<version>`. The deployment is validated but **not published** until a maintainer clicks
**Publish** at [central.sonatype.com](https://central.sonatype.com) → Deployments. See
[AGENTS.md](AGENTS.md#releasing-to-maven-central) for the one-time setup.

License
-------
[Apache License 2.0](LICENSE)

