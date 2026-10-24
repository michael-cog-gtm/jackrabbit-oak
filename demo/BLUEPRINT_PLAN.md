# Blueprint plan — michael-cog-gtm/jackrabbit-oak

Goal: a fresh Devin session on this repo can build `oak-blob-cloud-azure` in ~1 minute and run the
`tools/demo` crash tool immediately.

| Step | Why | Verified this session |
|---|---|---|
| Maven 3.9.9 → `~/tools/apache-maven-3.9.9`, on PATH via `$ENVRC` | VM has JDK 17 but no Maven | yes (same tarball URL) |
| `~/.m2/settings.xml` mirroring `central` → maven-central.storage-download.googleapis.com | repo1.maven.org rate-limits the VM | yes |
| `docker pull mcr.microsoft.com/azure-storage/azurite:3.29.0` | Azurite emulator used by the crash tool and `AzuriteDockerRule` | yes |
| `mvn install -pl oak-blob-cloud-azure -am -DskipTests -Dskip.coverage=true -Drat.skip=true` | warms `~/.m2` with every third-party dependency (trunk and `demo/pre-fix` share them) | yes — 39 s warm on trunk, 22 s on `demo/pre-fix` |
| knowledge: build / test / demo commands | so the agent does not rediscover them | — |

Not included: JDK install (OpenJDK 17.0.19 is already on the base image), anything that starts a service
(Azurite is started on demand by `tools/demo/azurite.sh`).
