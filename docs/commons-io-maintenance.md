# Commons IO maintenance (Java 8)

The root POM and `register/pom.xml` now both use Commons IO 2.15.1, replacing
2.6 and 2.4 respectively. This is the existing locally cached Java 8-compatible
release, beyond the 2.14.0 fix for CVE-2024-47554 / GHSA-78wr-2p64-hpwj.
The two exact Dependabot targets are #83 (root) and #82 (register).

Sources:
- https://commons.apache.org/proper/commons-io/security.html
- https://commons.apache.org/proper/commons-io/changes.html
- https://github.com/advisories/GHSA-78wr-2p64-hpwj

GitHub rates this advisory high; Apache rates it low. The update removes the
reported vulnerable versions, not proof that this application exposes an
exploitable XML-reading endpoint. Current application imports use
`IOUtils.closeQuietly`; no direct `XmlStreamReader` use was identified.

The bounded local regression suite covers the hardened encoding-pattern
boundary, normal UTF-8/UTF-16 XML reading, binary copy, and closeQuietly's
null/throwing-close behavior. Old 2.4/2.6 controls are checked separately.
These are not a general complexity proof, an XML parser security audit, or
full serving/Redis/ZooKeeper/model/native/package/deployment verification.
No application service, network endpoint, model, or production config is used.

This independent draft does not include or replace the separate Log4j/Jackson
PRs. It changes neither Java 8 support nor other dependency versions. Actual
Dependabot closure must be read after an explicitly authorized merge; creating
this draft does not close either alert. Empty native CI is not a passing CI.
