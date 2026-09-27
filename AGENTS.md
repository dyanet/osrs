# osrs: notes for agents and maintainers

Java client for the OpenSRS (Tucows) reseller XML API (XCP over HTTPS POST, MD5-signed).
Group `com.dyanet.osrs`, Java 21, default branch `master`. The repository is **public**.

## Modules

One repo, one version, separate artifacts:

| Module | Role | Published |
|---|---|---|
| `osrs-parent` (root POM) | Parent **and BOM**: its `dependencyManagement` lists the published `com.dyanet.osrs` artifacts and their runtime deps (slf4j). Test deps go in `<dependencies>`, never in `dependencyManagement`, so importing the BOM doesn't pin consumers' test libraries. | yes |
| `osrs-api` | The base every other module uses: `XcpCodec` (nested `dt_assoc`/`dt_array`, XXE-safe StAX parser), `OsrsSignature`, `Transport` (`JdkHttpTransport`, `StubTransport` for tests), `OsrsConfig`, `OsrsClient` (send/execute, retries, error mappers), the generic exceptions, and `lookup` (the only command kept here). Runtime deps: slf4j-api only. | yes |
| `osrs-domains` | Domain commands (`Domains`): balance, belongs_to_rsp, deleted domains; registration/renewal/management go here. `DomainException` maps domain response codes. | yes |
| `osrs-transfers`, `osrs-dns` | Command families being written. | no (`incubating` profile) |

Rules:
- A command-family module holds only its commands' knowledge: request attributes, reply
  models, and a response-code `OsrsErrorMapper`. XML, signing, HTTP, config, retries and generic
  errors stay in `osrs-api`. Family modules depend on `osrs-api` only (not on each other unless
  unavoidable), and each has its own package (`com.dyanet.osrs.<family>`) and
  `osrs.module.name` (the jar's `Automatic-Module-Name`).
- **Unfinished modules live in the `incubating` profile** of the root POM. CI builds them
  (`mvn verify -Pincubating`); a release (`mvn -Prelease deploy`) doesn't, so nothing
  half-done reaches Central. To publish one: move it to the default `<modules>`, add it to the
  parent's `dependencyManagement`, add it to CI's dry-run loop and the Codecov `files`, and
  remove its `jacoco.skip`.
- Only commands marked `idempotent(true)` (reads) are ever retried, and only on transport
  failures. Never mark register/renew/transfer or anything that charges as idempotent.
- Response codes come from the OpenSRS docs (domains.opensrs.guide `codes` page), not memory.
  The docs conflict on `555` ("already renewed" in the code table, "invalid ip address" in the
  troubleshooting guide), so it is an auth error only when the text mentions an IP address.
- Tests use `StubTransport` for unit tests and `HttpLoopbackTest` (a real local HTTP server
  that checks the signature) for the transport; fixtures are the documented sample replies.
  Coverage gates: `osrs-api` 90%, other published modules 80%.

## Never commit credentials

`osrs-api/src/test/resources/osrs-test.properties` holds **placeholders only**. A real horizon
key was once committed there, and that key has since been rotated. Real credentials come from `OSRS_CONFIG` (or
`-Dosrs.config`) pointing at a private file, or, in CI, from the `OPENSRS_API_KEY` repository
secret (manual **CI** runs only; see `.github/workflows/ci.yml`). Config templates with
placeholder values are in `config/*.properties.example`; since 1.0.0 none ship inside the jar.

## Build and test

- `mvn -B verify -Pincubating`: offline unit tests plus each module's JaCoCo gate.
- `mvn -B verify -Pintegration`: adds tests tagged `integration`, which hit `horizon.opensrs.net`.
  OpenSRS keys are usually IP-allowlisted, so a refused connection normally means the key's
  access rules.
- Keep `osrs-api` free of third-party runtime dependencies beyond slf4j-api: it uses the JDK's
  StAX parser and `java.net.http`. 1.0.0 dropped Jackson and Apache HttpClient 4; don't
  re-add them (or `commons-beanutils`, removed in 0.9.4 as unused and vulnerable).
- 0.9.x signed requests with `BigInteger.toString(16)`, which drops leading zero bytes, so
  about 1 request in 256 failed authentication. `OsrsSignature` uses `HexFormat`; a regression
  test covers it.

## Releasing to Maven Central

1. Bump the version in the root POM **and every module's `<parent>` reference** (all modules,
   incubating ones too), without `-SNAPSHOT`, in a PR, and merge to `master` (or a
   `release/**` branch). All published modules release together at one version.
2. `.github/workflows/release.yml` checks that `v<version>` isn't tagged yet, then runs
   `mvn -B -Prelease deploy`: tests, sources and javadoc jars, GPG signatures (BouncyCastle
   signer from `MAVEN_GPG_KEY`, so no keyring is needed), and an upload to the **Sonatype Central
   Portal** with `autoPublish=false` / `waitUntil=validated`. It then tags `v<version>`.
3. A maintainer clicks **Publish** at central.sonatype.com → **Deployments** (or **Drop**).
   Nothing is on Maven Central until then; this is the same approve-it-yourself model as the
   npm packages.
4. After publishing, update the product page **https://dyanet.com/osrs/** (versions, Maven
   coordinates, what each artifact covers) and make sure the dyanet.com **Apps** menu page has
   a box for osrs linking to it. The site runs on Ghost; these are content changes there, not
   in this repo.
5. The README is rewritten with the full coordinates table, usage and testing for every
   artifact once all the command-family modules are complete; until then keep it accurate for
   what is published.

The workflow skips with a notice while its secrets are missing.

### One-time setup (maintainer)

- **Namespace:** on central.sonatype.com → View Namespaces, add `com.dyanet` and verify it with a
  DNS TXT record on `dyanet.com` (Central checks the exact domain).
- **Token:** generate a Central Portal user token and store it as the `CENTRAL_TOKEN_USERNAME`
  and `CENTRAL_TOKEN_PASSWORD` secrets.
- **Signing key:** create a GPG key (Ed25519 or RSA 3072+; the BouncyCastle signer handles both,
  and the release key `B638678F1468A7AD4062F7A59328B8D7D104B4B6` is Ed25519). Publish the public
  key to `keyserver.ubuntu.com` or `keys.openpgp.org`, which Central checks. From WSL, where
  `gpg --send-keys` often fails, upload with `curl --data-urlencode keytext@key.asc
  https://keyserver.ubuntu.com/pks/add`, or `POST` JSON `{"keytext": …}` to
  `https://keys.openpgp.org/vks/v1/upload` (the old `curl -T` upload now returns 404). Store the armored secret key
  (`gpg --armor --export-secret-keys <id>`) as `MAVEN_GPG_PRIVATE_KEY`, and its passphrase as
  `MAVEN_GPG_PASSPHRASE`.

### Troubleshooting notes (from the first setup)

- The central-publishing plugin needs a `<server id="central">` entry in `settings.xml` even for
  dry runs; `actions/setup-java` writes it from `server-id` / `server-username` /
  `server-password`.
- Javadoc errors fail the release, because the javadoc jar is required (`doclint` is
  `all,-missing`). A broken `@param` tag in `OsrsClient` did exactly that before 0.9.4. CI's
  "Dry-run the Maven Central artifacts" step builds and signs the release artifacts on every PR
  to catch this early.
- `central-publishing-maven-plugin` ≥ 0.9 skips bundling entirely with `-DskipPublishing`, so
  CI dry-runs use `mvn -Prelease verify` and check the `.asc` signatures instead.
- Central rejects uploads whose POM lacks `licenses`, `developers` or `scm`
  (`connection`/`developerConnection`/`url`); these live in the root POM and are inherited.

## CI conventions

- JDK matrix 21 and 25 (Temurin), `fail-fast: false`, with concurrency cancelling superseded runs.
- Dependabot: grouped weekly Maven and monthly Actions updates. Semver-major updates are
  ignored and handled in the monthly security pass.
- Monthly security pass: check the runtime tree
  (`mvn dependency:list -DincludeScope=runtime`) against OSV (`api.osv.dev` querybatch, ecosystem
  `Maven`); the Claude GitHub App can't read Dependabot alerts. Update to the latest compatible
  versions, add tests, bump, merge and publish on Central. Record the pass in the "Monthly sec
  updates" project notes.

## Related

- Dyanet's npm packages (and Qern software such as qpost and qaim, whose SDKs and clients are
  published to npmjs) use the staged, OIDC-authenticated npm release process; this repository
  uses the Maven Central equivalent described above.
