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
