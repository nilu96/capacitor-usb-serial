package com.capacitorusbserial.plugin

import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.Ch34xSerialDriver
import com.hoho.android.usbserial.driver.Cp21xxSerialDriver
import com.hoho.android.usbserial.driver.FtdiSerialDriver
import com.hoho.android.usbserial.driver.ProlificSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort

/**
 * Pure mapping helpers between the JS string/number enums and the library's int/enum
 * constants. Every helper throws UsbSerialError(INVALID_PARAMS) on an unrecognized value.
 */
object EnumMaps {
    fun parityFromString(parity: String): Int =
        when (parity) {
            "none" -> UsbSerialPort.PARITY_NONE
            "odd" -> UsbSerialPort.PARITY_ODD
            "even" -> UsbSerialPort.PARITY_EVEN
            "mark" -> UsbSerialPort.PARITY_MARK
            "space" -> UsbSerialPort.PARITY_SPACE
            else -> invalid("parity", parity)
        }

    fun stopBitsFromNumber(stopBits: Double): Int =
        when (stopBits) {
            1.0 -> UsbSerialPort.STOPBITS_1
            1.5 -> UsbSerialPort.STOPBITS_1_5
            2.0 -> UsbSerialPort.STOPBITS_2
            else -> invalid("stopBits", stopBits)
        }

    fun dataBitsFromNumber(dataBits: Int): Int =
        when (dataBits) {
            5 -> UsbSerialPort.DATABITS_5
            6 -> UsbSerialPort.DATABITS_6
            7 -> UsbSerialPort.DATABITS_7
            8 -> UsbSerialPort.DATABITS_8
            else -> invalid("dataBits", dataBits)
        }

    fun flowControlFromString(mode: String): UsbSerialPort.FlowControl =
        when (mode) {
            "none" -> UsbSerialPort.FlowControl.NONE
            "rts_cts" -> UsbSerialPort.FlowControl.RTS_CTS
            "dtr_dsr" -> UsbSerialPort.FlowControl.DTR_DSR
            "xon_xoff" -> UsbSerialPort.FlowControl.XON_XOFF
            "xon_xoff_inline" -> UsbSerialPort.FlowControl.XON_XOFF_INLINE
            else -> invalid("flowControl", mode)
        }

    fun flowControlToString(mode: UsbSerialPort.FlowControl): String =
        when (mode) {
            UsbSerialPort.FlowControl.NONE -> "none"
            UsbSerialPort.FlowControl.RTS_CTS -> "rts_cts"
            UsbSerialPort.FlowControl.DTR_DSR -> "dtr_dsr"
            UsbSerialPort.FlowControl.XON_XOFF -> "xon_xoff"
            UsbSerialPort.FlowControl.XON_XOFF_INLINE -> "xon_xoff_inline"
        }

    fun driverClassFromType(driverType: String): Class<out UsbSerialDriver> =
        when (driverType) {
            "FtdiSerialDriver" -> FtdiSerialDriver::class.java
            "ProlificSerialDriver" -> ProlificSerialDriver::class.java
            "Cp21xxSerialDriver" -> Cp21xxSerialDriver::class.java
            "Ch34xSerialDriver" -> Ch34xSerialDriver::class.java
            "CdcAcmSerialDriver" -> CdcAcmSerialDriver::class.java
            else -> invalid("driverType", driverType)
        }

    /** Reverse map: a driver instance's simple class name -> the JS DriverType string. */
    fun driverTypeName(driver: UsbSerialDriver): String = driver.javaClass.simpleName

    private fun invalid(field: String, value: Any): Nothing =
        throw UsbSerialError(UsbSerialErrorCode.INVALID_PARAMS, "Unsupported $field: $value")
}
