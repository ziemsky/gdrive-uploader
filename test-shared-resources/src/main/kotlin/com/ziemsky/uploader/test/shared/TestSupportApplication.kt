package com.ziemsky.uploader.test.shared

import com.ziemsky.uploader.test.shared.data.TestFixtures
import com.ziemsky.uploader.test.shared.data.TestGDriveProvider
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
