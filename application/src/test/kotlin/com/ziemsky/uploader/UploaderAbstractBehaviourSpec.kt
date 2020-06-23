package com.ziemsky.uploader

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec

abstract class UploaderAbstractBehaviourSpec : BehaviorSpec() { // todo move to test-shared-resources
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf
}