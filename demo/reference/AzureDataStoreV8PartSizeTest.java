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
package org.apache.jackrabbit.oak.blob.cloud.azure.blobstorage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.apache.jackrabbit.oak.plugins.blob.datastore.directaccess.DataRecordUpload;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * OAK-12164: the v8 (legacy SDK) backend must keep advertising its original
 * 10 MB / 100 MB multipart part-size limits. With the shared v12 values
 * (256 KB / 4000 MB) a 2 GB upload is advertised as a single 2 GB part, which
 * clients that buffer parts of the advertised size cannot hold in memory.
 */
public class AzureDataStoreV8PartSizeTest {

    private static final long MB = 1024L * 1024L;
    private static final long TWO_GB = 2048L * MB;

    @ClassRule
    public static AzuriteDockerRule azurite = new AzuriteDockerRule();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder(new java.io.File("target"));

    @Test
    public void v8PartSizeLimitsFor2GbUpload() throws Exception {
        Properties props = new Properties();
        props.setProperty(AzureConstants.AZURE_BLOB_ENDPOINT, azurite.getBlobEndpoint());
        props.setProperty(AzureConstants.AZURE_STORAGE_ACCOUNT_NAME, AzuriteDockerRule.ACCOUNT_NAME);
        props.setProperty(AzureConstants.AZURE_STORAGE_ACCOUNT_KEY, AzuriteDockerRule.ACCOUNT_KEY);
        props.setProperty(AzureConstants.AZURE_BLOB_CONTAINER_NAME, "oak-12164-part-size");
        props.setProperty(AzureConstants.AZURE_CREATE_CONTAINER, "true");

        AzureDataStore ds = new AzureDataStore();
        ds.setProperties(props);
        ds.init(folder.newFolder().getAbsolutePath());
        try {
            ds.setDirectUploadURIExpirySeconds(60);
            DataRecordUpload upload = ds.initiateDataRecordUpload(TWO_GB, -1);

            assertEquals("v8 min part size", 10 * MB, upload.getMinPartSize());
            assertEquals("v8 max part size", 100 * MB, upload.getMaxPartSize());
            assertTrue("2 GB must be split into at least 21 parts, got " + upload.getUploadURIs().size(),
                    upload.getUploadURIs().size() >= 21);
        } finally {
            ds.close();
        }
    }
}
