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
| `osrs-api` | The base: XML envelope encoding/decoding (nested `dt_assoc`/`dt_array`), request signing, HTTPS transport, configuration, errors, retries, the basic commands that belong to no family (domain `lookup`, `suggest` (name_suggest), `price`, account `balance`, `belongsToRsp`), and `OsrsSession`, an in-order request queue. Only runtime dependency: `slf4j-api`. |
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
    PriceQuote p = client.price("example.com", 1, PriceType.NEW);    // osrs-api
    NameSuggestResult ideas = client.suggest(NameSuggestQuery.of("example", ".com", ".net"));
    Balance balance = client.balance();                              // osrs-api
    DeletedDomainsPage gone = Domains.on(client)                     // osrs-domains
        .deletedDomains(DeletedDomainsQuery.all());
}
```

### Sending requests in order

Calls on an `OsrsClient` may run in parallel. When requests depend on each other, send them
through an `OsrsSession`: it runs them strictly one at a time, in the order submitted, so a slow
OpenSRS never has several of them in flight. If one fails, everything queued after it is
cancelled (not sent), and the session reports it in plain language:

```java
try (OsrsSession session = client.openSession("order 1042")) {
    session.onFlush(flush -> showToUser(flush.message()));
    session.submit("register example.com", c -> /* a family command */ c.balance());
    session.submit(XcpRequest.builder("DOMAIN", "GET_BALANCE").build());
    session.drain();
}
```

> The request "register example.com" did not go through (OpenSRS said: Registration Failed:
> over quota, code 440). To keep your account consistent, all 2 requests waiting after it were
> cancelled and not sent: ... Fix the problem, then send them again.

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
valid reply arrived; `OsrsRequestCancelledException` for session requests cancelled after an
earlier failure.

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

