package com.ziemsky.uploader.securing.infrastructure.googledrive

import com.google.api.client.http.FileContent
import com.google.api.services.drive.model.File
import com.ziemsky.uploader.securing.infrastructure.BlockingRetryingExecutor
import com.ziemsky.uploader.securing.infrastructure.googledrive.model.GDriveFolder
import io.kotlintest.IsolationMode
import io.kotlintest.data.forall
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.matchers.withClue
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.BehaviorSpec
import io.kotlintest.tables.row
import io.mockk.*
import java.net.SocketTimeoutException
import java.time.Duration

class GDriveRetryingClientSpec : BehaviorSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    init {

        val rootFolderName = "rootFolderName"
        val rootFolder = GDriveFolder(rootFolderName, "root_folder_id")

        Given("Instance of retrying client") { // todo restructure - move setup to background, 'Give' focus on inputs?

            mockkObject(BlockingRetryingExecutor.Companion)

            val gDriveDirectClient: GDriveDirectClient = mockk()

            val expectedTimeout = Duration.ofSeconds(10)

            val gDriveRetryingClient = GDriveRetryingClient(gDriveDirectClient, expectedTimeout)


            When("uploading a file") {

                val fileToUpload = mockk<File>()
                val fileContentToUpload = mockk<FileContent>()

                Then("delegates the action to the retrying executor") {
                    assertThatRetryingClientDelegatesToDirectClientThroughRetryingExecutor(
                            clientReturnValue = mockk<Unit>(),
                            expectedInteractionWithDirectClient = { gDriveDirectClient.upload(fileToUpload, fileContentToUpload) },
                            retryingClientActionToVerify = { gDriveRetryingClient.upload(fileToUpload, fileContentToUpload) },
                            expectedTimeout = expectedTimeout
                    )
                }
            }


            When("getting top level daily folders") {

                val expectedTopLevelDailyFolders: List<GDriveFolder> = mockk()


                Then("delegates the action to the retrying executor") {
                    assertThatRetryingClientDelegatesToDirectClientThroughRetryingExecutor(
                            clientReturnValue = expectedTopLevelDailyFolders,
                            expectedInteractionWithDirectClient = { gDriveDirectClient.childFoldersOf(rootFolder) },
                            retryingClientActionToVerify = { gDriveRetryingClient.childFoldersOf(rootFolder) },
                            expectedTimeout = expectedTimeout
                    )
                }
            }


            When("deleting folder") {

                val folderToDelete = mockk<GDriveFolder>()

                Then("delegates the action to the retrying executor") {
                    assertThatRetryingClientDelegatesToDirectClientThroughRetryingExecutor(
                            clientReturnValue = mockk<Unit>(),
                            expectedInteractionWithDirectClient = { gDriveDirectClient.deleteFolder(folderToDelete) },
                            retryingClientActionToVerify = { gDriveRetryingClient.deleteFolder(folderToDelete) },
                            expectedTimeout = expectedTimeout
                    )
                }
            }


            When("creating top level folder") {

                val folderToCreateName = "top level folder to delete"
                val folderCreated = mockk<GDriveFolder>()

                Then("delegates the action to the retrying executor") {
                    assertThatRetryingClientDelegatesToDirectClientThroughRetryingExecutor(
                            clientReturnValue = folderCreated,
                            expectedInteractionWithDirectClient = { gDriveDirectClient.createTopLevelFolder("rootFolderId", folderToCreateName) },
                            retryingClientActionToVerify = { gDriveRetryingClient.createTopLevelFolder("rootFolderId", folderToCreateName) },
                            expectedTimeout = expectedTimeout
                    )
                }
            }
        }
    }

    private fun <CLIENT_RETURN_VALUE_TYPE> assertThatRetryingClientDelegatesToDirectClientThroughRetryingExecutor(
            clientReturnValue: CLIENT_RETURN_VALUE_TYPE,
            expectedInteractionWithDirectClient: () -> CLIENT_RETURN_VALUE_TYPE,
            retryingClientActionToVerify: () -> CLIENT_RETURN_VALUE_TYPE,
            expectedTimeout: Duration
    ) {
        val actionCaptureSlot = slot<() -> Unit>()
        val timeOutCaptureSlot = slot<Duration>()
        val retryableExceptionPredicateCaptureSlot = slot<(Throwable) -> Boolean>()
        val actionOnExpirationCaptureSlot = slot<() -> Unit>()

        programRetryingExecutorMock(
                actionCaptureSlot,
                timeOutCaptureSlot,
                retryableExceptionPredicateCaptureSlot,
                actionOnExpirationCaptureSlot
        )

        programDirectClientMock(expectedInteractionWithDirectClient, clientReturnValue)


        val retryingClientReturnedValue = retryingClientActionToVerify.invoke()


        verifyDelegationToDirectClient(
                clientReturnValue,
                retryingClientReturnedValue,
                expectedInteractionWithDirectClient
        )

        verifyCorrectTimeoutApplied(timeOutCaptureSlot, expectedTimeout)

        verifyRetryOnExceptionPredicate(retryableExceptionPredicateCaptureSlot.captured)

        verifyActionOnExpiration(actionOnExpirationCaptureSlot, expectedTimeout)
    }

    private fun verifyActionOnExpiration(actionOnExpirationCaptureSlot: CapturingSlot<() -> Unit>, expectedTimeout: Duration) {
        shouldThrow<TimeoutException> {
            actionOnExpirationCaptureSlot.captured.invoke()
        }.message shouldBe "Giving up on retrying; timeout expired: $expectedTimeout"
    }

    private fun verifyRetryOnExceptionPredicate(actualRetryableExceptionPredicate: (Throwable) -> Boolean) {

        // @formatter:off
        forall(
                row(GoogleJsonResponseExceptionParameters.RETRYABLE.asException(), "GoogleJsonResponseException"),
                row(SocketTimeoutException(),                                      "SocketTimeoutException")
        ) { predicateSatisfyingException, exceptionDescription ->
            withClue("Exception was not deemed 'retryable' despite meeting expected criteria") {
                actualRetryableExceptionPredicate.invoke(predicateSatisfyingException) shouldBe true
            }
        }
        // @formatter:on


        // @formatter:off
        forall(
                row("Exception of wrong type",
                        IllegalAccessException()
                ),
                row("Exception with wrong error info domain",
                        GoogleJsonResponseExceptionParameters.RETRYABLE
                                .copy(errorInfoDomain = "invalidErrorInfoDomain")
                                .asException()
                ),
                row("Exception with wrong error info reason",
                        GoogleJsonResponseExceptionParameters.RETRYABLE
                                .copy(errorInfoReason = "invalidErrorInfoReason")
                                .asException()
                ),
                row("Exception with wrong HTTP response status code",
                        GoogleJsonResponseExceptionParameters.RETRYABLE
                                .copy(httpResponseCode = 405)
                                .asException()
                )
        ) { testCaseDescription, exception ->
            withClue("$testCaseDescription was deemed 'retryable' despite not meeting expected criteria") {
                actualRetryableExceptionPredicate.invoke(exception) shouldBe false
            }
        }
        // @formatter:on
    }

    private fun verifyCorrectTimeoutApplied(timeOutCaptureSlot: CapturingSlot<Duration>, expectedTimeout: Duration) {
        timeOutCaptureSlot.captured shouldBeSameInstanceAs expectedTimeout
    }

    private fun <CLIENT_RETURN_VALUE_TYPE> verifyDelegationToDirectClient(
            clientReturnValue: CLIENT_RETURN_VALUE_TYPE,
            retryingClientReturnedValue: CLIENT_RETURN_VALUE_TYPE,
            expectedInteractionWithDirectClient: () -> CLIENT_RETURN_VALUE_TYPE
    ) {

        confirmVerified(clientReturnValue as Any)
        if (retryingClientReturnedValue !is Unit) {
            retryingClientReturnedValue shouldBeSameInstanceAs clientReturnValue
        }

        verify(
                exactly = 1,
                verifyBlock = { expectedInteractionWithDirectClient.invoke() }
        )
    }

    private fun <CLIENT_RETURN_VALUE_TYPE> programDirectClientMock(expectedInteractionWithDirectClient: () -> CLIENT_RETURN_VALUE_TYPE, clientReturnValue: CLIENT_RETURN_VALUE_TYPE) {
        every { expectedInteractionWithDirectClient.invoke() } returns clientReturnValue
    }

    private fun programRetryingExecutorMock(
            actionCaptureSlot: CapturingSlot<() -> Unit>,
            timeOutCaptureSlot: CapturingSlot<Duration>,
            retryableExceptionPredicateCaptureSlot: CapturingSlot<(Throwable) -> Boolean>,
            actionOnExpirationCaptureSlot: CapturingSlot<() -> Unit>
    ) {
        every {
            BlockingRetryingExecutor.retryOnException(
                    action = capture(actionCaptureSlot),
                    timeout = capture(timeOutCaptureSlot),
                    retryableExceptionPredicate = capture(retryableExceptionPredicateCaptureSlot),
                    actionOnExpiration = capture(actionOnExpirationCaptureSlot)
            )
        } answers {
            actionCaptureSlot.invoke()
            Unit
        }
    }
}

