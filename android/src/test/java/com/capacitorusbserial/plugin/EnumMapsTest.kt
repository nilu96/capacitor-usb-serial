package com.capacitorusbserial.plugin

import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.FtdiSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class EnumMapsTest {
    @Test
    fun parityValidValues() {
        assertEquals(UsbSerialPort.PARITY_NONE, EnumMaps.parityFromString("none"))
        assertEquals(UsbSerialPort.PARITY_ODD, EnumMaps.parityFromString("odd"))
        assertEquals(UsbSerialPort.PARITY_EVEN, EnumMaps.parityFromString("even"))
        assertEquals(UsbSerialPort.PARITY_MARK, EnumMaps.parityFromString("mark"))
        assertEquals(UsbSerialPort.PARITY_SPACE, EnumMaps.parityFromString("space"))
    }

    @Test
    fun stopBitsValidValues() {
        assertEquals(UsbSerialPort.STOPBITS_1, EnumMaps.stopBitsFromNumber(1.0))
        assertEquals(UsbSerialPort.STOPBITS_1_5, EnumMaps.stopBitsFromNumber(1.5))
        assertEquals(UsbSerialPort.STOPBITS_2, EnumMaps.stopBitsFromNumber(2.0))
    }

    @Test
    fun dataBitsValidValues() {
        assertEquals(UsbSerialPort.DATABITS_5, EnumMaps.dataBitsFromNumber(5))
        assertEquals(UsbSerialPort.DATABITS_8, EnumMaps.dataBitsFromNumber(8))
    }

    @Test
    fun flowControlRoundTrip() {
        for (fc in UsbSerialPort.FlowControl.values()) {
            val s = EnumMaps.flowControlToString(fc)
            assertEquals(fc, EnumMaps.flowControlFromString(s))
        }
    }

    @Test
    fun driverClassValidValues() {
        assertEquals(FtdiSerialDriver::class.java, EnumMaps.driverClassFromType("FtdiSerialDriver"))
        assertEquals(CdcAcmSerialDriver::class.java, EnumMaps.driverClassFromType("CdcAcmSerialDriver"))
    }

    @Test
    fun invalidParityThrowsInvalidParams() = assertInvalidParams { EnumMaps.parityFromString("weird") }

    @Test
    fun invalidStopBitsThrowsInvalidParams() = assertInvalidParams { EnumMaps.stopBitsFromNumber(3.0) }

    @Test
    fun invalidDataBitsThrowsInvalidParams() = assertInvalidParams { EnumMaps.dataBitsFromNumber(9) }

    @Test
    fun invalidFlowControlThrowsInvalidParams() = assertInvalidParams { EnumMaps.flowControlFromString("xx") }

    @Test
    fun invalidDriverTypeThrowsInvalidParams() = assertInvalidParams { EnumMaps.driverClassFromType("Nope") }

    private fun assertInvalidParams(block: () -> Unit) {
        try {
            block()
            fail("expected UsbSerialError")
        } catch (e: UsbSerialError) {
            assertEquals(UsbSerialErrorCode.INVALID_PARAMS, e.code)
        }
    }
}
