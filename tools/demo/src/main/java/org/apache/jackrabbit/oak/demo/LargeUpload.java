/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.oak.demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.jackrabbit.oak.blob.cloud.azure.blobstorage.AzureDataStore;
import org.apache.jackrabbit.oak.commons.PropertiesUtil;
import org.apache.jackrabbit.oak.plugins.blob.datastore.directaccess.DataRecordUpload;

/**
 * Stores one large binary in an {@link AzureDataStore} (v8 backend) running against Azurite,
 * using Oak's direct binary upload: initiate, HTTP PUT the parts, complete.
 * <p>
 * The part size follows the algorithm documented on
 * {@code org.apache.jackrabbit.api.binary.BinaryUpload} ("the easiest way ... is to use the
 * maxPartSize as the value for partSize"). The HTTP PUT is the one from Oak's own test client,
 * {@code oak-jcr/.../BinaryAccessTestUtils.httpPut}: a plain {@link HttpURLConnection} without
 * streaming mode, which keeps the part in heap until the request is sent.
 * <p>
 * Nothing here differs between the "before" and "after" state of the repository; only the part
 * sizes advertised by the Oak server change.
 */
public final class LargeUpload {

    private static final String ACCOUNT_NAME = "devstoreaccount1";
    private static final String ACCOUNT_KEY =
            "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";

    private LargeUpload() {
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> opts = parse(args);
        long size = parseSize(opts.getOrDefault("size", "2g"));
        String host = opts.getOrDefault("azurite", "127.0.0.1:10000");
        String container = opts.getOrDefault("container", "oak-demo");
        File file = new File(opts.getOrDefault("file", System.getProperty("java.io.tmpdir") + "/oak-demo-" + opts.getOrDefault("size", "2g") + ".bin"));
        File home = Files.createTempDirectory("oak-demo-ds").toFile();

        System.out.printf("Binary: %s (%,d bytes = %d MB), JVM max heap %d MB%n",
                file, size, size >> 20, Runtime.getRuntime().maxMemory() >> 20);
        ensureFile(file, size);

        try (HeapGraph heap = new HeapGraph(System.out, Long.parseLong(opts.getOrDefault("interval", "500"))).start()) {
            try {
                run(size, host, container, file, home, heap);
                System.out.printf("%nRESULT: SUCCESS - %d MB stored through %s; peak heap %d MB of %d MB%n",
                        size >> 20, AzureDataStore.class.getSimpleName(), heap.peakUsedBytes() >> 20, heap.maxBytes() >> 20);
            } catch (OutOfMemoryError oom) {
                heap.sample();
                System.out.printf("%nRESULT: CRASH - %s while uploading a part of the size Oak advertised; peak heap %d MB of %d MB%n",
                        oom, heap.peakUsedBytes() >> 20, heap.maxBytes() >> 20);
                oom.printStackTrace(System.out);
                System.out.flush();
                Runtime.getRuntime().halt(3);
            }
        }
    }

    private static void run(long size, String host, String container, File file, File home, HeapGraph heap) throws Exception {
        heap.phase("starting AzureDataStore");
        AzureDataStore ds = newDataStore(host, container, home);
        System.out.printf("AzureDataStore backend: %s%n", ds.getBackend().getClass().getName());

        heap.phase("initiateDataRecordUpload");
        DataRecordUpload upload = ds.initiateDataRecordUpload(size, -1);
        long minPart = upload.getMinPartSize();
        long maxPart = upload.getMaxPartSize();
        List<URI> uris = new ArrayList<>(upload.getUploadURIs());
        System.out.printf("Server advertised: minPartSize=%s maxPartSize=%s uploadURIs=%d%n",
                human(minPart), human(maxPart), uris.size());

        long partSize = size < minPart ? size : Math.min(size, maxPart);
        long numParts = (size + partSize - 1) / partSize;
        if (numParts > uris.size()) {
            throw new IllegalStateException("Not enough upload URIs for part size " + partSize);
        }
        System.out.printf("Client plan (BinaryUpload javadoc algorithm): %d part(s) of %s%n", numParts, human(partSize));

        try (InputStream in = new FileInputStream(file)) {
            for (int i = 0; i < numParts; i++) {
                long len = Math.min(partSize, size - (long) i * partSize);
                heap.phase(String.format("PUT part %d/%d (%s)", i + 1, numParts, human(len)));
                int code = httpPut(uris.get(i), len, new BoundedInputStream(in, len));
                if (code != HttpURLConnection.HTTP_CREATED) {
                    throw new IOException("Part " + (i + 1) + " failed with HTTP " + code);
                }
            }
        }

        heap.phase("completeDataRecordUpload");
        var record = ds.completeDataRecordUpload(upload.getUploadToken());
        System.out.printf("Record %s length=%,d bytes%n", record.getIdentifier(), record.getLength());
        if (record.getLength() != size) {
            throw new IllegalStateException("Stored length " + record.getLength() + " != " + size);
        }
        heap.phase("done");
        ds.close();
    }

    private static AzureDataStore newDataStore(String host, String container, File home) throws Exception {
        String endpoint = "http://" + host + "/" + ACCOUNT_NAME;
        Properties props = new Properties();
        props.setProperty("accessKey", ACCOUNT_NAME);
        props.setProperty("secretKey", ACCOUNT_KEY);
        props.setProperty("azureBlobEndpoint", endpoint);
        props.setProperty("azureConnectionString", "DefaultEndpointsProtocol=http;AccountName=" + ACCOUNT_NAME
                + ";AccountKey=" + ACCOUNT_KEY + ";BlobEndpoint=" + endpoint + ";");
        props.setProperty("container", container);
        props.setProperty("presignedHttpUploadURIExpirySeconds", "3600");
        props.setProperty("presignedHttpUploadURIDomainOverride", host + "/" + ACCOUNT_NAME);
        props.setProperty("cacheSize", "0");

        AzureDataStore ds = new AzureDataStore();
        Map<String, Object> map = new HashMap<>();
        for (String name : props.stringPropertyNames()) {
            map.put(name, props.getProperty(name));
        }
        PropertiesUtil.populate(ds, map, false);
        ds.setProperties(props);
        ds.init(home.getAbsolutePath());
        ds.setDirectUploadURIExpirySeconds(3600);
        return ds;
    }

    /** Same request Oak's oak-jcr BinaryAccessTestUtils.httpPut sends; Azurite speaks plain HTTP. */
    static int httpPut(URI uri, long contentLength, InputStream in) throws IOException {
        URI target = URI.create(uri.toString().replaceFirst("^https://", "http://"));
        HttpURLConnection connection = (HttpURLConnection) target.toURL().openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Content-Length", String.valueOf(contentLength));
        connection.setRequestProperty("Date", DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX")
                .withZone(ZoneOffset.UTC).format(Instant.now()));
        OutputStream putStream = connection.getOutputStream();
        IOUtils.copy(in, putStream);
        putStream.close();
        return connection.getResponseCode();
    }

    private static void ensureFile(File file, long size) throws IOException {
        if (file.length() == size) {
            return;
        }
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.setLength(size);
        }
    }

    private static Map<String, String> parse(String[] args) {
        Map<String, String> opts = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--") && i + 1 < args.length) {
                opts.put(args[i].substring(2), args[++i]);
            }
        }
        return opts;
    }

    static long parseSize(String s) {
        String v = s.trim().toLowerCase();
        long mult = 1;
        if (v.endsWith("g")) {
            mult = 1L << 30;
        } else if (v.endsWith("m")) {
            mult = 1L << 20;
        } else if (v.endsWith("k")) {
            mult = 1L << 10;
        }
        return Long.parseLong(mult == 1 ? v : v.substring(0, v.length() - 1)) * mult;
    }

    static String human(long bytes) {
        if (bytes % (1L << 30) == 0) {
            return (bytes >> 30) + " GB";
        }
        if (bytes % (1L << 20) == 0) {
            return (bytes >> 20) + " MB";
        }
        if (bytes % (1L << 10) == 0) {
            return (bytes >> 10) + " KB";
        }
        return bytes + " B";
    }
}
