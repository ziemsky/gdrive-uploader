package com.ziemsky.uploader

import io.kotlintest.IsolationMode
import io.kotlintest.specs.BehaviorSpec

abstract class UploaderAbstractBehaviourSpec : BehaviorSpec() { // todo move to test-shared-resources
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf
}