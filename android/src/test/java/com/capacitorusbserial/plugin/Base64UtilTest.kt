package com.capacitorusbserial.plugin

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Base64UtilTest {
    @Test
    fun roundTripsAll256ByteValues() {
        val all = ByteArray(256) { it.toByte() }
        val decoded = Base64Util.decode(Base64Util.encode(all))
        assertArrayEquals(all, decoded)
    }

    @Test
    fun roundTripsZeroLength() {
        val empty = ByteArray(0)
        val decoded = Base64Util.decode(Base64Util.encode(empty))
        assertEquals(0, decoded.size)
    }

    @Test
    fun encodeProducesSingleLineNoWrap() {
        val big = ByteArray(300) { (it % 256).toByte() }
        val encoded = Base64Util.encode(big)
        assertTrue("encoded must not contain newlines", !encoded.contains('\n'))
    }

    @Test
    fun malformedInputMapsToInvalidParams() {
        try {
            // '*' and '!' are not valid base64 alphabet characters.
            Base64Util.decode("not*valid!base64===")
            fail("expected UsbSerialError")
        } catch (e: UsbSerialError) {
            assertEquals(UsbSerialErrorCode.INVALID_PARAMS, e.code)
        }
    }
}
