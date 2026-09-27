# osrs: notes for agents and maintainers

Java client for the OpenSRS (Tucows) reseller XML API (XCP over HTTPS POST, MD5-signed).
Maven multi-module: `osrs-parent` (the root POM) and `osrs-api`. Group `com.dyanet.osrs`,
Java 21, default branch `master`. The repository is **public**.

## Never commit credentials

`osrs-api/src/test/resources/osrs-test.properties` holds **placeholders only**. A real horizon
key was once committed there, and that key has since been rotated. Real credentials come from `OSRS_CONFIG` (or
`-Dosrs.config`) pointing at a private file, or, in CI, from the `OPENSRS_API_KEY` repository
secret (manual **CI** runs only; see `.github/workflows/ci.yml`). The packaged
`osrs-*.properties` in `src/main/resources` are templates with placeholder values.

## Build and test

- `mvn -B verify`: offline unit tests plus the JaCoCo gate (75% instructions).
- `mvn -B verify -Pintegration`: adds tests tagged `integration`, which hit `horizon.opensrs.net`.
  OpenSRS keys are usually IP-allowlisted, so a refused connection normally means the key's
  access rules.
- Jackson versions come from `jackson-bom` in the parent (`jackson.version`); keep every
  Jackson module on it. Jackson 3 (`tools.jackson`) is a major migration: don't take it
  implicitly.
- `commons-beanutils` was removed (unused, and vulnerable at 1.9.4). Don't re-add it for
  convenience.

## Releasing to Maven Central

1. Bump the version in **both** POMs (root and `osrs-api/pom.xml` parent reference), without
   `-SNAPSHOT`, in a PR, and merge to `master` (or a `release/**` branch).
2. `.github/workflows/release.yml` checks that `v<version>` isn't tagged yet, then runs
   `mvn -B -Prelease deploy`: tests, sources and javadoc jars, GPG signatures (BouncyCastle
   signer from `MAVEN_GPG_KEY`, so no keyring is needed), and an upload to the **Sonatype Central
   Portal** with `autoPublish=false` / `waitUntil=validated`. It then tags `v<version>`.
3. A maintainer clicks **Publish** at central.sonatype.com → **Deployments** (or **Drop**).
   Nothing is on Maven Central until then; this is the same approve-it-yourself model as the
   npm packages.

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
