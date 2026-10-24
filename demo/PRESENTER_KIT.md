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
| **Today** | Devin: crash → failing test → fix → passing upload → PR in **≈ 5–7 minutes** (3 rehearsals: 6:47, 6:40 cold; 4:36 warm). | |

Talking point: the IDE assistant helped *write* #2989; it could not *run* 1.92.0 against Azure, so the
regression shipped. Devin's machine is the difference.

## Fallback: recording and PR

- Screen recording (crash → failing test → fix from PR #3 → passing test → identical upload succeeds), recorded by the
  testing agent on the build VM, 8 MB mp4: https://devin-gtm.devinenterprise.com/attachments/2f074816-cd7c-4251-ad88-da3a570b4270/oak-demo-a-full-proof-edited.mp4
  Key frames are in `demo/evidence/recording/` (01 OOM + heap graph … 05 SUCCESS + heap graph) with the terminal transcript.
- Best rehearsal PR (left open on purpose): https://github.com/michael-cog-gtm/jackrabbit-oak/pull/3 (rehearsal 2, 6 min 40 s prompt → PR)
- Replay of the original #2409 with Devin Review's verdict: https://github.com/michael-cog-gtm/jackrabbit-oak/pull/1

## Devin Review on the original change (replay)

Devin Review ran on PR #1 (the exact #2409 diff, 45 files) at 00:24 UTC on 2026-10-24 and finished in
about 2 minutes. Verbatim finding titles (full text in `demo/review/`):

- **[bug]** Blob enumeration includes metadata records — `AzureBlobStoreBackend.java:388-405`
- **[bug]** Large binary uploads use oversized blocks — `AzureBlobStoreBackend.java:286-290`
  ("When a file exceeds 4000 MiB, `uploadBlob` sets its block size to the entire file length. Azure limits each staged block to 4000 MiB, so the binary upload fails.")
- **[bug]** Connection strings bypass the configured proxy — `AzureBlobContainerProvider.java:164-166`
- **[security]** Verbose logs expose Azure SAS tokens — `AzureHttpRequestLoggingPolicy.java:58-62`
- **[flag]** Delegation-key request loses clock-skew allowance — `AzureBlobContainerProvider.java:241-242`

It did **not** flag the OAK-12164 regression itself (the v8 backend picking up the shared 256 KB / 4000 MB
limits). Say so: the honest point is that a reviewer that cannot run the code can miss what a machine that
runs it finds in two minutes — which is exactly step 2 of the live run. (The "oversized blocks" bug it did
find is the sibling issue on the v12 write path.)

## Reset between runs

```
demo/reset.sh            # archives issue/OAK-12164* under rehearsal/<stamp>/… and deletes them (their PRs close but stay readable);
                         # never touches the fallback PR branch (KEEP_BRANCHES); checks demo/pre-fix
demo/reset.sh --delete   # same without the archive copy
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
- A session cannot be started "on a branch", so the typed prompt is preceded by one line:
  `Repository michael-cog-gtm/jackrabbit-oak, branch demo/pre-fix.` (that is how all rehearsals were run).
- Branch-name collision: Devin follows AGENTS.md and picks `issue/OAK-12164`. If that name still exists on GitHub
  (or was *renamed* — GitHub keeps a redirect), the push collides and Devin improvises. Always run `demo/reset.sh`
  first; it deletes the name outright. The fallback PR #3 lives on `issue/OAK-12164-v8-part-size` so it never collides.
- Devin Review on the fix PR raises a legitimate follow-up (v8 still uses the shared 190 TiB max-binary cap vs.
  100 MB × 50,000 URIs ≈ 4.77 TiB; pre-#2409 it was 4.75 TB). Devin reports it and asks instead of silently widening
  the PR — a good moment to show restraint. Michael's call whether to say "yes, do it" live.

## Rehearsals

| # | Session | Wall time | Outcome | PR |
|---|---|---|---|---|
| 1 | https://devin-gtm.devinenterprise.com/sessions/20d3392b3fe046e785dec812bf5433a9 (cold VM, no blueprint yet) | **6 min 47 s** prompt → PR opened (00:27:58 → 00:34:45 UTC); final report 7 min 16 s | OOM reproduced, own Azurite-backed failing test, v8-only fix (3 files +83/−7), 548 tests green, 5.0 ACUs | https://github.com/michael-cog-gtm/jackrabbit-oak/pull/2 (closed by reset.sh — branch name reused; evidence intact) |
| 2 | https://devin-gtm.devinenterprise.com/sessions/bd84e88629e242d2a105c5265e648e34 (cold VM) | **6 min 40 s** prompt → PR opened (00:39:15 → 00:45:55 UTC); final report 9 min 29 s incl. CI wait | Same shape; regression test added to `AzureBlobStoreBackendV8Test`, 548 tests green, 5.4 ACUs | https://github.com/michael-cog-gtm/jackrabbit-oak/pull/3 **(fallback PR, left open)** |
| 3 | https://devin-gtm.devinenterprise.com/sessions/8ef1f062d48c447cac3297d6d5af7b9c (**warm blueprint snapshot**) | **4 min 36 s** prompt → PR opened (01:04:02 → 01:08:38 UTC); final report 4 min 56 s, CI green at 7 min 06 s | Same shape: OOM repro, regression test in `AzureBlobStoreBackendV8Test`, v8-only fix (2 files +36/−7), 548 tests green | https://github.com/michael-cog-gtm/jackrabbit-oak/pull/4 (open; second fallback) |

Median prompt → PR: **6 min 40 s** cold, **4 min 36 s** warm. All three runs fit the 8-minute slot; the warm run leaves ~3 min for narration.
Every run raised the same legitimate follow-up (190 TiB max-binary cap) and asked instead of widening the PR.

## Links

- Branch `demo/pre-fix` (start state): https://github.com/michael-cog-gtm/jackrabbit-oak/tree/demo/pre-fix
- Branch `demo/kit-on-trunk` (after-state comparison): https://github.com/michael-cog-gtm/jackrabbit-oak/tree/demo/kit-on-trunk
- Replay PR #1: https://github.com/michael-cog-gtm/jackrabbit-oak/pull/1
- Build session: https://devin-gtm.devinenterprise.com/sessions/3173d5f14f82437eabb5db678769cd42
- Everything else: `demo/MANIFEST.md`
