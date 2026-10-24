# tools/demo — large-binary upload against a real Azure emulator

Stores one large binary (default 2 GB) through Oak's real `AzureDataStore` (legacy Azure SDK v8
backend) into a local [Azurite](https://github.com/Azure/Azurite) blob emulator, with the JVM heap
capped at 1 GB, and prints a live heap graph while it runs.

```
tools/demo/upload.sh                 # start Azurite, build, upload 2 GB with -Xmx1g
tools/demo/upload.sh 512m            # different binary size
HEAP=2g tools/demo/upload.sh         # different heap cap
SKIP_BUILD=1 tools/demo/upload.sh    # reuse the last build
tools/demo/azurite.sh reset          # wipe the emulator
```

`build.sh` installs Maven 3.9.9 into `~/tools` and a Maven Central mirror into `~/.m2/settings.xml`
if they are missing, builds `oak-blob-cloud-azure` (and the Oak modules it depends on) into `~/.m2`,
then compiles the small client in this directory. Needs JDK 11+ and Docker.

## What the client does

`LargeUpload` is a direct-binary-access client written against the documented
`org.apache.jackrabbit.api.binary.BinaryUpload` algorithm, the same contract AEM's upload clients
follow:

1. `AzureDataStore.initiateDataRecordUpload(size, -1)` — Oak answers with the min/max part size
   and a list of pre-signed upload URLs.
2. The client picks the part size the javadoc recommends (`maxPartSize`, capped at the file size)
   and PUTs each part to its URL with a plain `java.net.HttpURLConnection`.
3. `AzureDataStore.completeDataRecordUpload(token)` — Oak commits the block list and returns the
   record; the client checks the stored length.

**Honesty note.** The `OutOfMemoryError` happens in step 2, inside the client's HTTP layer: like
AEM's upload clients, it buffers each part in memory before sending it, so it can only ever hold a
part that fits in the heap. Oak's server-side code does not allocate the part. What Oak controls is
the *advertised* part size, and that is the regression: a backend that says "parts may be up to
4000 MB" makes a compliant client try to buffer a 2 GB part; a backend that says "at most 100 MB"
makes the same client send 21 parts of 100 MB. The stack trace the tool prints is the real one.

## Branch note

The branch `demo/pre-fix` stands in for the 1.92 release line (it is upstream commit `f14f6e3e`
plus this kit). Fixes made on it are branched from it and their pull requests target
`demo/pre-fix`, not `trunk` (trunk already contains the later rework and the diff would be
unreadable).
