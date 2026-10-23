# Spring 5.2 patch maintenance

This extends the existing Dependabot PR #5 without rewriting its bot commit.
The shared `spring.version` moves from 5.2.20.RELEASE to 5.2.22.RELEASE;
Java 8, Spring Boot, and unrelated dependencies are unchanged.

## Actual Dependabot mapping

The open-alert export checked on 2026-10-23 identifies these direct dependencies
in the root `pom.xml` on the unchanged default branch:

| Alert | Package | Advisory | First patched release |
| --- | --- | --- | --- |
| [39](https://github.com/realTaki/FATE-Serving/security/dependabot/39) | spring-beans | GHSA-36p3-wjmg-h94x / CVE-2022-22965 | 5.2.20.RELEASE |
| [81](https://github.com/realTaki/FATE-Serving/security/dependabot/81) | spring-context | GHSA-g5mm-vmx4-3rg7 / CVE-2022-22968 | 5.2.21.RELEASE |
| [73](https://github.com/realTaki/FATE-Serving/security/dependabot/73) | spring-beans | GHSA-hh26-6xwr-ggv7 / CVE-2022-22970 | 5.2.22.RELEASE |

The original PR's 5.2.20 version addresses the first version range, but not the
two subsequent patches. Official Spring guidance:
[22965](https://spring.io/security/cve-2022-22965/),
[22968](https://spring.io/security/cve-2022-22968/), and
[22970](https://spring.io/security/cve-2022-22970/).
Version-range matching does not establish production exploitability. In
particular, the published CVE-2022-22965 exploit has JDK 9+ and WAR/Tomcat
prerequisites; this repository targets Java 8.

## Verification and limits

`SpringDataBindingSecurityTest` checks case-insensitive blocked property names,
nested blocked names, an allowed property, and local application-context bean
registration/resolution. On the original 5.2.20 PR head, the two blocked-field
tests fail (the context smoke passes); on 5.2.22 all three pass.

The complete six-project Maven reactor was clean-compiled and tested offline
using Temurin Java 8 and Maven 3.9.11; the resolved Spring dependency tree was
also inspected. No application, Redis, ZooKeeper, inference workload, network
exploit, packaging/deployment, or production performance test is implied.
There are no pre-existing unit tests in this PR's baseline.

This is **not a complete Spring security upgrade**. The real open alerts
117 (spring-core, GHSA-659m-px2c-25wj), 90 (spring-context,
GHSA-4gc7-5j7h-4qph), and 91 (spring-context, GHSA-4wp7-92pw-q264)
still affect this unsupported 5.x line; the export has no first patched
version for those 5.x ranges. Spring Boot and Jackson alerts are separate
unresolved work. No alerts are dismissed and the default branch is unchanged.
