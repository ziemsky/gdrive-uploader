package com.ziemsky.gdriveuploader

import io.kotlintest.shouldBe
import io.kotlintest.specs.BehaviorSpec

class Test : BehaviorSpec({
    given("come conditions") {
        `when`("some action is performed") {
            then("some outcome gets verified") {
                2 shouldBe 2
            }
        }
    }
})