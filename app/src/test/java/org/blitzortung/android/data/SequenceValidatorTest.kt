package org.blitzortung.android.data

import android.os.Build
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.N, Build.VERSION_CODES.M])
class SequenceValidatorTest {

    @Test
    fun higherNumberIsUpdate() {
        val validator = SequenceValidator()

        assertTrue(validator.isUpdate(10))
    }

    @Test
    fun lowerNumberIsNoUpdate() {
        val validator = SequenceValidator()

        validator.isUpdate(10)
        assertFalse(validator.isUpdate(9))
    }
}
