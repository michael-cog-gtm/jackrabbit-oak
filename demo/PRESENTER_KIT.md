# Demo A — "Crash it, then don't" (Apache Jackrabbit Oak)

Devin on its own machine: reproduces a real out-of-memory crash against a real Azure emulator,
fixes the regression the way Oak's maintainers ask for in `AGENTS.md`, proves the same upload now
works, and opens the PR — in minutes, for a bug that took the project six months to settle.

## The prompt Michael types (verbatim, nothing else)

> Customers on 1.92.0 are hitting out-of-memory errors uploading large binaries to Azure. Jira OAK-12164 says the multipart part-size limits changed from 10 MB / 100 MB to 256 KB / 4 GB and are now shared with the v12 code. Reproduce the crash against a real Azure emulator on your machine using the tools in tools/demo, fix it so the v8 path uses its original limits without touching v12, prove the same upload now works, and open a PR with the before and after.

Start the session on repo `michael-cog-gtm/jackrabbit-oak`, branch `demo/pre-fix`.

## What the audience sees

| Step | What Devin does | What to say | Expected time |
|---|---|---|---|
| 1 | Reads `AGENTS.md`, `tools/demo/README.md`, finds `AzureBlobStoreBackendV8.initiateHttpUpload()` and the shared constants in `AzureConstants` | "It reads the repo's own rules first — branch names, commit format, license headers, no new Guava." | 1–2 min |
| 2 | Starts Azurite in Docker, builds `oak-blob-cloud-azure`, runs `tools/demo/upload.sh` → live heap graph → `java.lang.OutOfMemoryError: Java heap space` | "That is a real JVM dying with a 1 GB heap, uploading a 2 GB file through the real Oak code, against a real Azure emulator. Oak told the client a part could be 4 GB." | 1–3 min (cold build is the long pole) |
| 3 | Writes a failing JUnit 4 test (Azurite-backed: a 2 GB upload on v8 must advertise 10 MB / 100 MB), sees it fail | "Test first. It fails for the right reason: 256 KB instead of 10 MB." | 1 min |
| 4 | Restores the v8 limits in the v8 backend only, leaves `v12` untouched, commit `OAK-12164: …` on `issue/OAK-12164` | "Minimal fix, where the maintainers would want it." | 1 min |
| 5 | Reruns the identical upload: 21 parts × 100 MB, 2 GB stored, peak heap ~460 MB; test passes | "Same command, same heap, same file — it works." | <1 min |
| 6 | Opens the PR against `demo/pre-fix` with before/after output | "That PR is the artifact. Nothing here was simulated." | <1 min |

Rehearsal timings (fresh sessions, nothing pre-installed): see "Rehearsals" below.

## The one-slide timeline

| Date | What happened upstream | Who |
|---|---|---|
| **Feb 16, 2026** | #2409 merges: Azure SDK v8 → v12, 45 files, +14,895 / −1,555. Part-size limits silently become shared 256 KB / 4000 MB. Released in Oak 1.92.0. | 1 author |
| **Mar 24, 2026** | OAK-12164 (Adobe): "min: 10M => 256k, max: 100M => 4G … This is causing OOMs on our side." | customer |
| **Apr 15, 2026** | #2858: the whole module is reverted to 1.90.0. | |
| **Aug 14, 2026** | #2989: the rework lands, +7,422 / −42, 35 files, **9 reviewers**, `Ai-Assisted-By: claude`. | 9 reviewers |
| **Today** | Devin: crash → failing test → fix → passing upload → PR in **≈ N minutes** (rehearsal median). | |

Talking point: the IDE assistant helped *write* #2989; it could not *run* 1.92.0 against Azure, so the
regression shipped. Devin's machine is the difference.

## Fallback: recording and PR

- Screen recording of the best run: _(link filled in after recording)_
- Best rehearsal PR (left open on purpose): _(filled in after rehearsals)_
- Replay of the original #2409 with Devin Review's verdict: https://github.com/michael-cog-gtm/jackrabbit-oak/pull/1

## Devin Review on the original change (replay)

_(verdict pasted verbatim once the review completes)_

## Reset between runs

```
demo/reset.sh            # parks issue/OAK-12164 (+ its PR, kept open) under rehearsal/<stamp>/…, checks demo/pre-fix
demo/reset.sh --delete   # deletes the rehearsal branch instead
```
Needs `gh` logged in to the `michael-cog-gtm` org. The script refuses to continue if `demo/pre-fix` is
no longer `f14f6e3e` + the kit commit(s).

## Known gotchas

- **The honest mechanism.** Oak's server code does not allocate the 2 GB; it *advertises* a 4000 MB part
  and a client that buffers parts of the advertised size (AEM's do; `tools/demo/LargeUpload` does) runs
  out of heap. If an architect asks, say exactly that; the kit's README says the same.
- A cold session has no Maven and an empty `~/.m2`; `tools/demo/build.sh` bootstraps both, but the
  first build downloads the Oak dependency tree. Approve the repo blueprint (see MANIFEST) so sessions
  start warm (~40 s module build).
- `repo1.maven.org` rate-limits Devin VMs; the kit and blueprint use the Google-hosted Central mirror.
- If Devin opens the PR against `trunk` instead of `demo/pre-fix`, the diff is unreadable (trunk already
  has the rework). `tools/demo/README.md` tells it to target `demo/pre-fix`.
- Do **not** merge the fix PR; it is the artifact.

## Rehearsals

| # | Session | Wall time | Outcome | PR |
|---|---|---|---|---|
| 1 | _(tbd)_ | | | |
| 2 | _(tbd)_ | | | |
| 3 | _(tbd)_ | | | |

## Links

- Branch `demo/pre-fix` (start state): https://github.com/michael-cog-gtm/jackrabbit-oak/tree/demo/pre-fix
- Branch `demo/kit-on-trunk` (after-state comparison): https://github.com/michael-cog-gtm/jackrabbit-oak/tree/demo/kit-on-trunk
- Replay PR #1: https://github.com/michael-cog-gtm/jackrabbit-oak/pull/1
- Build session: https://devin-gtm.devinenterprise.com/sessions/3173d5f14f82437eabb5db678769cd42
- Everything else: `demo/MANIFEST.md`
