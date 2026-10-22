# Log4j 2 maintenance (2026-10-22)

The parent POM now selects Log4j API and Core **2.25.5**. The register
module's two hard-coded 2.11.1 overrides now use the same parent property.
This keeps the existing Java 8 source/target and LMAX Disruptor 3.4.2,
without changing the Spring, Jackson, gRPC or model-serving APIs.
There is no existing Maven dependency lockfile to regenerate.

## Why this version

Apache's [security advisories](https://logging.apache.org/security.html)
include the old 2.11.1 Core version in affected ranges for Log4Shell
(CVE-2021-44228) and other vulnerabilities. Version 2.25.5 is the security
maintenance release on the 2.25 line, including the API JSON serialization
fix for CVE-2026-49844; stopping at 2.25.4 would introduce that known issue.
This is a dependency-level finding, not proof of exploitation in this app.

Apache's [release notes](https://logging.apache.org/log4j/2.x/release-notes.html#release-notes-2-25-5)
and [installation requirements](https://logging.apache.org/log4j/2.x/manual/installation.html#requirements)
document this release and the Java 8 runtime floor. The newer 2.26 feature
line is deliberately not required for this bounded maintenance update.

## Validation

With Maven 3.9.11 and a Java 8 JDK:

```sh
mvn clean test
mvn test org.apache.maven.plugins:maven-dependency-plugin:3.1.1:tree \
  -Dincludes=org.apache.logging.log4j,org.slf4j,ch.qos.logback,log4j
```

All six reactor projects compile and pass the available tests. The four
new `Log4jSecurityTest` tests check API/Core alignment, literal message
lookup syntax, RFC 8259-compatible non-finite map values and ordinary
finite numbers. No tests existed in this checkout before this change;
this is not an application integration suite.

Separate logging-only Java 8 smoke checks load the unchanged serving-server
XML and router properties with Log4j 2.25.5 / Disruptor 3.4.2, verify all 11
configured rolling-file outputs and clean shutdown, without starting the
server, router, Redis, ZooKeeper, inference or training. They use only a
local `${java:version}` string, not an LDAP/JNDI URL. They also pass with
JVM network connect/listen/accept operations explicitly denied.

## Remaining scope

This is **not** a clean-bill-of-health for the repository. In particular:

- ZooKeeper 3.5.5 still brings the separate, end-of-life
  `log4j:log4j:1.2.17` artifact. Migrating/removing it and validating all
  downstream logging behavior is a separate task. Its `slf4j-log4j12`
  binding was already excluded; this change does not alter SLF4J routing.
- Other legacy dependencies remain, including existing Spring and Jackson
  update PRs. They are not modified or superseded here.
- Current live Dependabot data could not be refreshed under the known
  access gate. Historical alert counts are not current scan results, and
  independent manifest/advisory checks do not replace live alerts.
- No production, federated multi-party, native-storage, full packaging,
  throughput or deployment validation was performed. Compilation is not
  evidence of safe operation for those surfaces.
- This change is intended for a draft PR. No merge, release or deployment
  is part of this update.
