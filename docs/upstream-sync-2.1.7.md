# Upstream synchronization: Serving 1.1.2 to 2.1.7

## Pinned inputs and history

This draft integrates `FederatedAI/FATE-Serving` master at
`a25807586a960051b9acd4e0114f94a13ddc90ef` (2024-12-17), not a moving ref.
The fork input is `3cc6ffd1e4f60bd9793003a865c54f36d7917b44`, including
both the MyBatis PR17 and JSON-Java PR16 merges from 2026-10-30.

The current upstream master and this old fork have **unrelated Git ancestry**.
The synchronization commit has both pinned inputs as parents, with the fork
as first parent. Neither history is replaced, rebased or force-pushed.
Future comparisons can use this explicit integration point.

The exact fork-only delta was inspected from its original upstream baseline
`819287b0b72b5775870938715dc676b426612c5d` through the pinned fork head:
22 paths, with no production Java changes. The original user's only custom
commit added `.gitignore`; its patterns are preserved as a union with upstream.
The remaining changes are the five maintained POMs, seven historical security
records, and eight version-specific test/harness sources. Historical records
remain byte-identical in `docs/`; **their 1.1.2 commands and validation claims
are not claims about this new 2.1.7 tree**. Old test/harness sources remain in
the first-parent history rather than being copied onto removed 2.x APIs.

## Structural migration, not a drop-in patch

This adopts the upstream 2.x source, documentation, protos and assets.
It is a major application migration, not a compatible 1.1.2 maintenance release.

| Previous module | Upstream 2.x disposition |
| --- | --- |
| `serving-server` | `fate-serving-server`, revised inference/model/config APIs |
| `federatedml` | `fate-serving-federatedml` |
| `register` | `fate-serving-register` |
| `router` | replaced by `fate-serving-proxy`; do not assume old config compatibility |
| `fate-serving-core` | heavily reorganized; old `DistributedDTable` storage path removed |
| none | new common, extension, SDK, admin and admin-UI modules |

Old model files, deployment layouts, adapters, configuration and client/API
compatibility need a separate migration review before any real deployment.
No live service, database, model, private config or deployment is touched here.

## Security changes preserved instead of rolled back

| Dependency | Pinned candidate / disposition |
| --- | --- |
| gRPC family and generator | 1.75.0; not upstream's vulnerable 1.45.1 |
| Protobuf utility/runtime/generator | 3.25.8, aligned with gRPC 1.75 |
| Guava | 33.3.1-jre, aligned release family with gRPC's declared Android variant |
| Jackson annotations/core/databind/dataformats | 2.18.11 |
| Log4j 2 API/core/SLF4J bridge/JUL bridge | 2.25.5 |
| Commons IO | 2.15.1 |
| JSON-Java | 20231013, inherited by the renamed modules |
| Spring Framework | upstream Boot 2.7.0 manages 5.3.20, the corresponding fixed alternative to fork 5.2.22 |
| Fastjson / MyBatis | removed by upstream; no dependency or import reintroduced |

The old three `MetadataUtils.attachHeaders` calls disappear with upstream's
removed `DistributedDTable`; copying its obsolete implementation back would
undo the structural migration. The surviving `FateNameResolver` is not
misreported as an existing compiler failure.

The Spring fixed-version correspondence is documented by the official
[CVE-2022-22970 advisory](https://spring.io/security/cve-2022-22970/).
Preserving the old fixes is **not** a complete security audit or a claim that
these unsupported Spring/Boot generations have no remaining vulnerabilities.
Alert closure is assessed only after a separately authorized merge and scan;
this draft itself closes no default-branch alert. Existing PR14 is left open
and unchanged for traceability, pending a later explicit merge/supersession decision.

## Publishing safety

The upstream DockerHub workflow automatically publishes from master/tags and
uses upstream-specific secrets. Its exact source is retained only as
`docs/upstream-reference/docker-images-build-and-push.yml.disabled`, not as
an active GitHub Actions workflow. This sync does not enable publishing,
change repository permissions/settings/secrets, merge, tag or deploy.

## Minimal verification

On 2026-10-30, the existing Temurin Java 8u504 and Maven 3.9.11 successfully
compiled all nine selected reactor projects in about 75 seconds:

```sh
mvn -DskipTests -pl fate-serving-server,fate-serving-proxy,fate-serving-sdk -am compile
```

This includes core, common, register, federatedml, extension, server, proxy and
SDK, with regenerated Protobuf 3.25.8 and gRPC 1.75.0 sources. No existing
upstream production Java needed modification to compile with these versions.
A separate `validate dependency:tree` confirmed one coherent gRPC 1.75.0
family, Protobuf 3.25.8, common-protos 2.59.2, Guava 33.3.1-jre and the
preserved dependency versions above. Fastjson/MyBatis are absent.

The compilation uses the executor's configured proxy and system TLS trust
store; TLS verification is not disabled. Existing toolchain/cache files stay
unchanged; new artifacts and all temporary build output are task-local.
No generated sources, targets, archives or local Maven cache are committed.

A full build, full test suite, admin/admin-UI build, npm/Node installation,
packaging, container build, performance, network/service, model/native storage
and production acceptance are intentionally outside this minimal check.
Empty checks/statuses are not green CI.
