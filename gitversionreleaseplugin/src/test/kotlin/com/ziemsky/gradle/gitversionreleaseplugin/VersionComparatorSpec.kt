package com.ziemsky.gradle.gitversionreleaseplugin

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe

class VersionComparatorSpec : BehaviorSpec() {

    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    init {

        Given("Pairs of versions in various 'greater than', 'equal', 'less than' configurations") {

            // @formatter:off
            val testCases = listOf(
                    row("1.0.0", "0.2.3",  1, "L > R: major greater"                                      ),
                    row("0.1.0", "0.0.1",  1, "L > R: major not greater, minor greater"                   ),
                    row("0.1.0", "0.0.1",  1, "L > R: major not greater, minor not greater, patch greater"),

                    row("0.0.0", "0.0.0",  0, "L = R: all positions equal"                                ),
                    row("1.1.1", "1.1.1",  0, "L = R: all positions equal"                                ),
                    row("2.2.2", "2.2.2",  0, "L = R: all positions equal"                                ),

                    row("0.2.3", "1.0.0", -1, "L < R: major lesser"                                       ),
                    row("0.0.1", "0.1.0", -1, "L < R: major not lesser, minor lesser"                     ),
                    row("0.0.1", "0.1.0", -1, "L < R: major not greater, minor not lesser, patch lesser"  )
            ).toTypedArray()
            // @formatter:on



            When("comparing versions from given pair") {

                Then("returns comparison value according to semantics or major.minor.patch segments") {

                    io.kotest.data.forAll(*testCases) { left: String, right: String, expectedResult: Int, testCaseDescription: String ->
                        val leftVersion = Version.from(left)
                        val rightVersion = Version.from(right)

                        VersionComparator.compare(leftVersion, rightVersion) shouldBe expectedResult
                    }

                }
            }
        }
    }
}
