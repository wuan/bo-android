package org.blitzortung.android.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
