# Fastjson 1.2.84 regression scope

This compatible 1.2.x update pins the four existing direct Fastjson declarations
in `fate-serving-core`, `register`, `federatedml` and `serving-server` to 1.2.84.
It does not migrate to fastjson2, enable autoType in application code, or change
the Java 8 requirement, services, stores, models or application configuration.

## Actual alert mapping

The original repository's Dependabot instances were read as open on 2026-10-28:

| Instances | Original manifests | Advisory | First patched version |
| --- | --- | --- | --- |
| #103, #104 | core, register | GHSA-jm7w-5684-pvh8 | 1.2.48 |
| #77, #78, #79, #80 | core, register, federatedml, server | GHSA-pv7h-hx5h-mgfj | 1.2.83 |

Version 1.2.84 is above both actual alert ranges and also includes the upstream
1.2.84 autoType hardening. Version coverage is not a confirmed alert closure or
proof that the original application was remotely exploitable. In particular,
this change must not be described as an account-wide vulnerability cleanup.

## Reproducible checks

With Java 8 and the existing Maven 3.9.11/cache, run the six-project reactor:

```sh
mvn -o -B -ntp clean test dependency:tree -Dincludes=com.alibaba:fastjson
```

`FastjsonSecurityTest` covers the actually loaded artifact's embedded Maven
version, typed cache-style JSON, UTF-8 nested byte serialization, the actual
`HostFederatedParams` DTO, rejection of illegal type names before a recording
class loader, and a harmless explicit nested-class accept entry. The recording
loader rejects every request; the inputs are non-URL dummy strings. Local
`ParserConfig` objects do not alter the application's global configuration.
`FastjsonInferenceConversionTest` exercises the actual `InferenceRequest`
serialization and typed parse with ordinary feature data.

The official 1.2.84 artifact still has the constant `JSON.VERSION = "1.2.83"`.
Consequently the version guard reads the loaded artifact's
`META-INF/maven/com.alibaba/fastjson/pom.properties`, rather than trusting that
stale/inlinable constant. The official JAR/POM SHA-256 and Java 8 class-file
compatibility are checked separately in the maintenance evidence.

These bounded tests do not start servers or access Redis, ZooKeeper, SQL,
models, files from users, remote class loaders, JNDI, RPC or inference workloads.
Project resource/configuration bodies, complete packaging/native integration,
production runtime and full performance/security assurance remain unverified.
Existing unrelated dependency alerts and application CI gaps remain separate.

## Upstream evidence

- [1.2.84 release](https://github.com/alibaba/fastjson/releases/tag/1.2.84)
- [Upstream hardening change](https://github.com/alibaba/fastjson/commit/ad353ff71e27a587bfb18cab329572fd5cc44ea6)
- [GHSA-jm7w-5684-pvh8](https://github.com/advisories/GHSA-jm7w-5684-pvh8)
- [GHSA-pv7h-hx5h-mgfj](https://github.com/advisories/GHSA-pv7h-hx5h-mgfj)
