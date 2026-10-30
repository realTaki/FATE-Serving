# Targeted MyBatis maintenance (Java 8)

Only `serving-server/pom.xml` changes MyBatis **3.4.6 → 3.5.6**, the published
fixed boundary for existing high Dependabot **#28**, CVE-2020-26945 /
GHSA-qq48-m4jx-xqh8. The project stays on MyBatis 3.x, Java 8 and the existing
mybatis-spring 1.3.2. Other dependency versions and production source are unchanged.

Important: 3.5.6's upstream change checks for a JEP-290 serialization filter and
warns once when it is missing. **The upgrade does not configure a restrictive
filter or make arbitrary untrusted deserialization safe.** The test uses an
explicit filter only in a disposable child JVM; no application JVM settings,
service config, database, real user object or credential is changed or used.
A deployment-specific filter policy remains an independent operational decision.

## Bounded validation

- Existing official old/new JAR and POM bytes match Maven Central checksums.
  Candidate base classes: 996, maximum class major 52; executed on Temurin 8u504.
- The same test class is compiled against the old 3.4.6 API. Candidate **6/6**
  passes: exact artifact guard, ordinary serialized-cache copy, synthetic mapper
  annotation/bound parameters, reflection/type-handler defaults, one-time
  missing-filter warning, and a configured-filter rejection of a harmless
  synthetic class. There is no database query or network/service execution.
- Baseline 3.4.6 passes the four ordinary controls. The version guard and
  missing-filter-warning guard fail as expected; these are not exploit claims.
- Current five-module `validate dependency:tree` succeeds fully offline:
  server resolves mybatis 3.5.6 and unchanged mybatis-spring 1.3.2.
  **This goal does not compile or test the whole reactor.**
- A planned Spring-adapter fixture stopped before compilation when its JDBC
  test prerequisites were not present. No new JDBC dependency was added just
  to make a fixture pass. Actual Spring-adapter/database integration, complete
  reactor/app/package/resources, Redis/ZooKeeper/models/native/platform and
  production acceptance remain unverified.

The standalone JUnit source is in the server test tree. Its fresh-JVM checks
build a classpath from the actual test/MyBatis/JUnit/Hamcrest code sources, not
Maven's launcher classpath, to support both forked and in-process test runners.

This independent draft leaves existing PRs 10/12/13/14/16 untouched and does not
merge, deploy, change settings or dismiss an alert. #28 remains open until an
explicitly authorized merge and actual GitHub closure readback. Empty native
application CI is not a passing CI or whole-repository acceptance.

Sources:
- https://github.com/advisories/GHSA-qq48-m4jx-xqh8
- https://github.com/mybatis/mybatis-3/releases/tag/mybatis-3.5.6
- https://github.com/mybatis/mybatis-3/pull/2079
