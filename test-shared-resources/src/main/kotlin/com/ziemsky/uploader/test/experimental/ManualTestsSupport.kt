package com.ziemsky.uploader.test.experimental

import com.google.api.services.drive.Drive
import com.ziemsky.uploader.test.experimental.ManualTestsSupport.Companion.queryRunner
import com.ziemsky.uploader.test.experimental.ManualTestsSupport.Companion.testFixtures
import com.ziemsky.uploader.test.shared.TestGDriveProvider
import com.ziemsky.uploader.test.shared.data.TestFixtures
import mu.KotlinLogging
import java.nio.file.Paths


private val log = KotlinLogging.logger {}

class ManualTestsSupport {

    companion object {

        @JvmStatic
        fun testFixtures(): TestFixtures = TestFixtures(
                Paths.get("/tmp/uploader/inbound"),
                drive()
        )

        @JvmStatic
        fun drive(): Drive {
            return TestGDriveProvider(
                    "uploader",
                    Paths.get("conf/local/google/gdrive/secrets/tokens"),
                    Paths.get("conf/local/google/gdrive/secrets/credentials.json"),
                    "Uploader"
            ).drive()
        }

        @JvmStatic
        fun queryRunner(): ExperimentalRetryingQueryRunner {
            return ExperimentalRetryingQueryRunner(drive())
        }
    }
}

private val commands = mapOf(
        Pair("demoRequestRateLimitViolation", { queryRunner().executeQueriesInParallel(200) }),
        Pair("createLocalTestFiles", { testFixtures().createLocalTestFilesToSecure(100, 140_000) })
)

fun main(args: Array<String>) {

    if (args.isEmpty()) {
        log.info { "Supported options:" }
        commands.keys.forEach { log.info { "  $it" } }
        return
    }

    val command = args[0]

    require(commands.containsKey(command)) { "Unsupported command: $command" }

    commands.get(command)?.invoke()
}