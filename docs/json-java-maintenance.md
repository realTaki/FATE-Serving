# JSON-Java narrow maintenance scope

## Exact dependency change

Dependabot PR16 initially changes only `serving-server/pom.xml` from
`org.json:json:20151123` to `20231013`. This follow-on retains that bot commit
and changes the same direct dependency in `fate-serving-core` and `federatedml`
to `20231013`. No new dependencies, service behavior, JVM settings, credentials,
CI workflows, deployment, or other production-code changes are included.
The existing Java 8 project floor is unchanged: the candidate artifact's
29 base classes have maximum class-file major 52 and were actually run on
Temurin 8u504. Java 6/7 is not supported or tested by this upgrade.

The exact existing Dependabot alert mapping is:

| Path | CVE-2023-5072 / GHSA-4jq9-2xhw-jpx7 | CVE-2022-45688 / GHSA-3vqj-43w4-2q58 |
| --- | --- | --- |
| fate-serving-core/pom.xml | #69 | #44 |
| federatedml/pom.xml | #70 | #45 |
| serving-server/pom.xml | #71 | #46 |

All six are high and remain open until GitHub actually marks them fixed after
an authorized merge; branch-version coverage is not alert closure.
The reviewed advisory's first patched versions are respectively 20231013 and
20230227. Official sources:
- https://github.com/advisories/GHSA-4jq9-2xhw-jpx7
- https://github.com/advisories/GHSA-3vqj-43w4-2q58
- https://github.com/stleary/JSON-java/releases/tag/20231013
- https://github.com/stleary/JSON-java/pull/759
- https://github.com/stleary/JSON-java/pull/772

## Focused synthetic regression

`scripts/maintenance-json-regression/JsonCompatibilityRegression.java` is a
standalone Java 8 harness, not an automatically discovered Maven/JUnit suite.
It compiles against the old 20151123 API using exact, unmodified production
classes (no stubs): CommonPreProcessing, Configuration, DSLParser and their
minimal supporting types. Eight ordinary checks cover JSON access/roundtrip,
actual preprocessing, DSL topological order/module/upstream mappings, and
Configuration loading from disposable synthetic files only. Three guards
check the loaded artifact version, tiny embedded-NUL rejection and a 7-character
array-key input. Candidate 20231013 passes all 11. Baseline 20151123 passes all
8 ordinary controls and the tiny embedded-NUL guard, but fails the exact-version
and array-key guards. The baseline already rejecting that embedded-NUL input
must not be presented as a differential CVE reproduction. There is no high-depth,
large-allocation, live-service or XML-exhaustion test.

Use an existing verified JDK8 and JARs; these commands do not download or install
anything. Supply artifact paths explicitly. The synthetic output directory must
be new or empty; the harness refuses a nonempty directory.

```sh
# Set JAVA_HOME, JSON_BASELINE, JSON_PATCHED, LOG4J_API to verified local artifacts.
# JSON_BASELINE=json-20151123.jar; JSON_PATCHED=json-20231013.jar;
# LOG4J_API=the project's log4j-api-2.25.5.jar.
work=$(mktemp -d)
mkdir "$work/classes"
core=fate-serving-core/src/main/java/com/webank/ai/fate/serving/core/bean
server=serving-server/src/main/java/com/webank/ai/fate/serving
"$JAVA_HOME/bin/javac" -source 8 -target 8 -encoding UTF-8 \
  -cp "$JSON_BASELINE:$LOG4J_API" -d "$work/classes" \
  "$core/Context.java" "$core/ReturnResult.java" "$core/Dict.java" \
  "$core/StatusCode.java" "$core/Configuration.java" \
  federatedml/src/main/java/com/webank/ai/fate/serving/federatedml/DSLParser.java \
  "$server/bean/PreProcessingResult.java" \
  "$server/adapter/processing/PreProcessing.java" \
  "$server/adapter/processing/CommonPreProcessing.java" \
  scripts/maintenance-json-regression/JsonCompatibilityRegression.java
"$JAVA_HOME/bin/java" -Xmx96m -Xss1m -cp "$work/classes:$JSON_PATCHED:$LOG4J_API" \
  JsonCompatibilityRegression "$work/candidate"
# Expected nonzero exit, with the two guard failures described above:
"$JAVA_HOME/bin/java" -Xmx96m -Xss1m -cp "$work/classes:$JSON_BASELINE:$LOG4J_API" \
  JsonCompatibilityRegression "$work/baseline"
```

## Verification boundaries

The recorded focused processes use one CPU, a 96 MiB heap, 15 CPU-second and
30 wall-second limits, no real service, sockets, database, model, private
configuration or credentials. Only disposable synthetic adapter config is read.
A Log4j no-provider diagnostic is expected in this intentionally minimal
API-only classpath; it is not a service/logging integration acceptance.
The unmodified parser's pre-existing varargs/unchecked compile warning remains.

The bounded offline reactor check is `validate dependency:tree
-Dincludes=org.json:json`, not compile/test/package/install. With the three
POM fields unified, core/federatedml/server and router's transitive dependency
must all resolve 20231013. Existing original Maven-cache files are unchanged.
Official Maven Central JAR/POM SHA1 sidecars and local SHA256 bind both versions.

The date-version span contains other compatibility changes, including concrete
numeric backing types. The fixture deliberately checks the Number contract;
it does not certify every external caller's casts or permissive malformed input.
Complete reactor compilation/tests, package/resources, XML nesting limits,
real service/inference/storage/native/platform and production behavior remain
unverified. This does not combine the independent MyBatis PR17, certify its
merge tree, configure a restrictive deserialization filter or authorize merging
any PR. Exact-head empty checks/statuses/Actions are not green CI.
