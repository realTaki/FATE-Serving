# Java 8 Jackson maintenance

This extends the existing Dependabot Jackson update: keep annotations,
databind and the selected transitive core at 2.18.11 in both
`fate-serving-core` and `serving-server`. The server's previous prerelease
2.10.0.pr1 property must not override the core's updated dependency.
No application code, Java floor, or other dependency version is changed.
This is a same-major dependency update, not a claim that eight minor-release
steps have no behavioral differences.

## Actual Dependabot scope

The 2026-10-27 snapshot has 24 open databind alerts (13 high, 11 medium)
in those two manifests: #13, #29, #40, #42, #43, #108, #109, #110, #111, #112, #113, #114, #115, #116, #118, #119, #120, #121, #122, #123, #124, #125, #128, #129.
Version 2.18.11 is outside all 24 exported affected ranges. That is version
coverage, **not** evidence that GitHub has closed them or that every reported
feature is reachable in this application. No merge is included here.

In particular, the official advisories for
[generic PTV validation](https://github.com/advisories/GHSA-j3rv-43j4-c7qm),
[array component validation](https://github.com/advisories/GHSA-rmj7-2vxq-3g9f),
[unknown type-ID retention](https://github.com/advisories/GHSA-wv8q-qhhj-9h54),
and [forward identity references](https://github.com/advisories/GHSA-cxp5-3px4-pw24)
identify specific opt-in deserialization shapes. Tests deliberately construct
those shapes using harmless local beans; they do not establish production
exploitability or execute gadget side effects.

## Reproduction and observed evidence

Using the existing Temurin 8u504 and Maven 3.9.11:

```sh
mvn clean test org.apache.maven.plugins:maven-dependency-plugin:3.1.1:tree \
  -Dincludes=com.fasterxml.jackson.core,com.fasterxml.jackson.datatype
```

All six reactor projects compile and their 11 available tests pass: the
three preserved Spring regressions, six Jackson regressions, and two server
compatibility tests. The resolved annotations/core/databind trio is 2.18.11
wherever present. The server tests exercise the actual InferenceRequest
conversion path and the unchanged jsr310 2.9.6 runtime module's LocalDate
and Instant round trips; that module is not upgraded in this bounded change.

The Jackson tests check the exact versions, denied generic and array
components without constructing them, 2,500 distinct fallback type IDs with
the upstream 1,000-entry cache bound, a 400-reference reverse-resolution
linear-work guard with ordering/object identity retained, and duplicate or
unresolved identity errors. The separate matched 2.18.10 control passes
three focused tests but fails the cache and reverse-resolution guards;
2.18.11 passes all five. The old 2.10.0 control also fails the generic guard
and identity/cache checks, but lacks the later array-validator helper API;
its NoSuchMethodError is not counted as a demonstrated array exploit.

An initial test incorrectly required only two fallback cache entries and
failed against 2.18.11. Inspection of the exact fixed artifact established
its documented-in-code 1,000-entry bound; the fixture was corrected to test
that bound over 2,500 distinct IDs, not weakened to accept unbounded growth.

The three candidate JARs and seven supporting POMs matched official Maven
Central SHA256 values. The additional JUnit 5.11.4 BOM POM was obtained over
TLS, but its exact `.pom.sha256` request returned HTTP 403; no retry or
alternate route was used, and no official-checksum claim is made for that
metadata-only POM. It supplies version management, not a new runtime JAR.

## Limits

No application CI is defined in the tree. The provider's dynamic Dependabot
workflow and empty exact-head check/status sets are not a green application
CI result. These are compilation and bounded local regressions, not full
packaging, live serving, Redis/ZooKeeper/MySQL, federated inference, model,
native-storage, network, production, or deployment acceptance.

Other real alerts remain, including separate Log4j work in draft PR6 and
Spring end-of-life findings. Default-branch and PR6 content stay unchanged.
