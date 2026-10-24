# Demo A build notes (running log)

## 2026-10-24 — build session

- OOM mechanism (verified by reading `f14f6e3e`): `AzureConstants.AZURE_BLOB_MAX_MULTIPART_UPLOAD_PART_SIZE = 4000 MB`
  is shared by v8 and v12; `AzureBlobStoreBackendV8.initiateHttpUpload()` returns it as `DataRecordUpload.getMaxPartSize()`.
  Oak does not allocate the part itself; the part size reaches the client, and a client that buffers parts of
  the advertised size (as AEM's upload clients do) allocates it. Reproduced honestly with a plain
  `HttpURLConnection` client that follows the `BinaryUpload` javadoc (part size = maxPartSize).
- Pre-fix run (`-Xmx1g`, 2 GB): `RESULT: CRASH - java.lang.OutOfMemoryError: Java heap space`, 2.0 s wall,
  trace through `ByteArrayOutputStream` ← `PosterOutputStream` ← `LargeUpload.httpPut`.
- Trunk run (same command): 21 parts × 100 MB, 2 GB stored, 11.3 s wall, peak heap 464 MB.
- Reference test `demo/reference/AzureDataStoreV8PartSizeTest.java`: pre-fix fails
  (`v8 min part size expected:<10485760> but was:<262144>`), trunk passes (3.6 s with Azurite via Testcontainers).
- Builds: Maven 3.9.9 via the GCS Central mirror; `oak-blob-cloud-azure -am` warm ≈ 40 s (trunk) / 22 s (pre-fix);
  `tools/demo/build.sh` total ≈ 39 s warm.
- Replay PR #1 (replay/OAK-11267-pr2409 → replay-base) created 00:24 UTC; Devin Review triggered at 00:24 UTC.
- Gotchas: `AzureDataStoreUtils.getAzureConfig()` in the test tree forces `blob.azure.v12.enabled=true`, so a v8
  repro must build its own `Properties`. Pre-signed URLs come back as `https://127.0.0.1:…`; the client rewrites
  them to `http://` for Azurite. `git_create_pr` returned a 504 once but the PR had not been created; retry worked.
- 00:26 UTC Devin Review on PR #1 completed: 3 bug, 1 security, 1 flag; the v8 part-size regression was not among them
  (full text in demo/review/). CI on PR #1: 1 passed.
- 00:28 UTC rehearsal 1 started: https://devin-gtm.devinenterprise.com/sessions/20d3392b3fe046e785dec812bf5433a9
  (prompt = one line "Repository michael-cog-gtm/jackrabbit-oak, branch demo/pre-fix." + the verbatim demo prompt;
  child sessions cannot be pointed at a branch any other way).
- 00:36 UTC Michael approved the repo blueprint; snapshot build triggered automatically. Rehearsal 1 runs on a cold VM
  (no Maven, empty ~/.m2); later rehearsals should land on the warm snapshot.
- Rehearsal 1 (cold VM, Ultra): session https://devin-gtm.devinenterprise.com/sessions/20d3392b3fe046e785dec812bf5433a9
  created 00:27:58 UTC → "Done" + PR https://github.com/michael-cog-gtm/jackrabbit-oak/pull/2 at 00:35:14 UTC = **7 min 16 s**
  (CI green at 00:38:30). 5.0 ACUs. Did everything: OOM repro, own Azurite-backed failing test, fix in AzureBlobStoreBackendV8
  only (3 files +83/−7), 548 tests green, PR issue/OAK-12164 → demo/pre-fix. Open question it raised (Devin Review on PR #2):
  v8 AZURE_BLOB_MAX_BINARY_UPLOAD_SIZE still the shared 190 TiB vs. 100 MB × 50,000 URIs = 4.77 TiB — left for Michael.
- Rehearsal 2 (cold VM again — snapshot not rebuilt yet, Ultra): https://devin-gtm.devinenterprise.com/sessions/bd84e88629e242d2a105c5265e648e34
  created 00:39:15 → PR https://github.com/michael-cog-gtm/jackrabbit-oak/pull/3 created 00:45:55 = **6 min 40 s** (final "Done" 00:48:44 after
  CI). 5.4 ACUs. Same result shape: OOM repro, new regression test in AzureBlobStoreBackendV8Test, v8-only fix, 548 tests green.
  (Rehearsal 1 by the same measure: PR created 00:34:45 = 6 min 47 s.)
- **reset.sh bug found by rehearsal 2:** parking by *renaming* `issue/OAK-12164` leaves GitHub's branch-rename redirect, so the next
  session's push to `issue/OAK-12164` re-attached parked PR #2 and the name was "blocked"; the child briefly closed #2 freeing it,
  restored it, and used `issue/OAK-12164-v8-part-size`. Fix: park by copying the ref (`git push origin issue/OAK-12164:rehearsal/<stamp>/…`)
  then deleting the original (closes that PR — a closed PR keeps its diff/evidence). Fallback PR = #3, whose branch name does not collide.
