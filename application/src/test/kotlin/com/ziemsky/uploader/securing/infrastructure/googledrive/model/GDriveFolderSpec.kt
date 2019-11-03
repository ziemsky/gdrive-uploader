package com.ziemsky.uploader.securing.infrastructure.googledrive.model

import io.kotlintest.IsolationMode
import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.BehaviorSpec
import io.kotlintest.tables.row

class GDriveFolderSpec : BehaviorSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    init {

        Given("Valid Google Drive item id") {

            val validIds = arrayOf(
                    "0B7BjJByfiGCmWnBYQUFwcElWRkU",
                    "1ZmeZBczGsWDR99tm4NlLW3rjzX78p_qo",
                    "14zJk2NHrUcuARFKSJ2QoAi_IIYFCqH0V",
                    "1PNTqjs-Cvhqyixqz7TjklB2-BOjipaAc",
                    "1Eosa66R-D3lFD6avXoWKRRFBhxuWuvO4",
                    "1mjxt3w1f1OTQG6xeAaDiGMEC8Au3VBr-"
            ).map { row(it) }.toTypedArray()

            And("a valid Google Drive folder name") {

                val validFolderName = "valid folder name"

                When("creating new instance of GDriveFolder") {

                    val createNewInstance = { name: String, id: String -> GDriveFolder(name, id) }

                    Then("""new instance is created without errors
                          | and with provided name and id
                          | """.trimMargin()) {

                        forall(*validIds) { expectedId ->
                            val actualGDriveFolder = createNewInstance.invoke(validFolderName, expectedId)

                            actualGDriveFolder.id shouldBe expectedId
                            actualGDriveFolder.name shouldBe validFolderName
                        }
                    }
                }
            }
        }

        Given("Invalid Google Drive item id") {

            // @formatter:off
            val invalidGoogleDriveItemIds = arrayOf(
                    row("",                                             "empty name"),
                    row("                                           ",  "blank name"),
                    // create one row per illegal character
                    *"¬`|!\"£$%^&*()+={[}]:;@'~#|\\<,>.?/ \t\n\r"
                            .map { illegalCharacter ->
                                row(
                                        "${illegalCharacter}_valid_suffix_padding_to_max_len",
                                        "name with illegal character: '$illegalCharacter'"
                                )
                            }
                            .toTypedArray()
            )
            // @formatter:on

            And("a valid Google Drive item name") {

                val validFolderName = "valid folder name"

                When("creating new instance of GDriveFolder") {

                    val createNewInstance = { name: String, id: String -> GDriveFolder(name, id) }

                    Then("exception is thrown") {

                        forall(*invalidGoogleDriveItemIds) { invalidGoogleDriveItemId, testCase ->

                            val actualException = shouldThrow<IllegalArgumentException> {
                                createNewInstance.invoke(validFolderName, invalidGoogleDriveItemId)
                            }
                            actualException.message shouldBe "Id should be non-empty, non-blank and comprise only characters 0-9, a-z, A-Z, '-' and '_', but was '$invalidGoogleDriveItemId'."
                        }
                    }
                }
            }
        }

        Given("Valid folder name") {

            val validFolderNameTestCases = arrayOf(
                    ".",
                    "..",
                    "any non empty string, non-blank, can have numbers such as 0-9"
            ).map { row(it) }.toTypedArray()

            And("valid Google Drive item id") {

                val validGoogleDriveItemId = "1PNTqjs-Cvhqyixqz7TjklB2-BOjipaAc"

                When("creating new instance of GDriveFolder") {

                    val createNewInstance = { name: String, id: String -> GDriveFolder(name, id) }

                    Then("""new instance is created without errors
                          | and with provided name and id
                          | """.trimMargin()) {

                        forall(*validFolderNameTestCases) { folderName ->
                            val actualGDriveFolder = createNewInstance.invoke(folderName, validGoogleDriveItemId)

                            actualGDriveFolder.id shouldBe validGoogleDriveItemId
                            actualGDriveFolder.name shouldBe folderName
                        }
                    }
                }
            }
        }

        Given("Invalid folder name") {

            // @formatter:off
            val invalidGoogleDriveFolderNames = arrayOf(
                    row("",     "empty name"),
                    row(" ",    "blank name (space)"),
                    row("\t",   "blank name (\\t)"),
                    row("\n",   "blank name (\\n)"),
                    row("\r",   "blank name (\\r)")
            )
            // @formatter:on

            And("valid Google Drive item id") {

                val validGoogleDriveItemId = "1PNTqjs-Cvhqyixqz7TjklB2-BOjipaAc"

                When("creating new instance of GDriveFolder") {

                    val createNewInstance = { name: String, id: String -> GDriveFolder(name, id) }

                    Then("exception is thrown") {
                        forall(*invalidGoogleDriveFolderNames) { invalidFolderName, testCase ->

                            val actualException = shouldThrow<IllegalArgumentException> {
                                createNewInstance.invoke(invalidFolderName, validGoogleDriveItemId)
                            }
                            actualException.message shouldBe "Name should be non-empty and non-blank, but was '$invalidFolderName'."
                        }
                    }
                }
            }
        }
    }
}