package com.ziemsky.gdriveuploader.test.shared

import com.ziemsky.gdriveuploader.test.shared.data.TestFixtures
import com.ziemsky.gdriveuploader.test.shared.data.TestGDriveProvider
import java.nio.file.Paths


fun main(args: Array<String>) {
    TestFixtures(
            Paths.get("/tmp/inbound"),
            TestGDriveProvider(
                    "uploader",
                    Paths.get("conf/local/google/gdrive/secrets/tokens"),
                    Paths.get("conf/local/google/gdrive/secrets/credentials.json"),
                    "Uploader"
            ).drive()
//    ).clearTempDir()
    ).createTestFilesFixtures(10)

}
