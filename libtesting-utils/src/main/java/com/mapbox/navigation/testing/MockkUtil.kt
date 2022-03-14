package com.mapbox.navigation.testing

import io.mockk.MockKVerificationScope
import io.mockk.verify

fun verifyOnce(verifyBlock: MockKVerificationScope.() -> Unit) {
    verify(exactly = 1, verifyBlock = verifyBlock)
}

fun verifyNoOne(verifyBlock: MockKVerificationScope.() -> Unit) {
    verify(exactly = 0, verifyBlock = verifyBlock)
}
