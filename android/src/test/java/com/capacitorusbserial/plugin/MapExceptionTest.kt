package com.capacitorusbserial.plugin

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class MapExceptionTest {
    @Test
    fun unsupportedOperationMapsToUnsupportedOperation() {
        assertEquals(
            UsbSerialErrorCode.UNSUPPORTED_OPERATION,
            mapException(UnsupportedOperationException("nope")),
        )
    }

    @Test
    fun illegalArgumentMapsToInvalidParams() {
        assertEquals(
            UsbSerialErrorCode.INVALID_PARAMS,
            mapException(IllegalArgumentException("bad")),
        )
    }

    @Test
    fun illegalStateMapsToInvalidState() {
        assertEquals(
            UsbSerialErrorCode.INVALID_STATE,
            mapException(IllegalStateException("wrong state")),
        )
    }

    @Test
    fun ioExceptionMapsToIoError() {
        assertEquals(UsbSerialErrorCode.IO_ERROR, mapException(IOException("io")))
    }

    @Test
    fun usbSerialErrorKeepsItsOwnCode() {
        assertEquals(
            UsbSerialErrorCode.PORT_NOT_OPEN,
            mapException(UsbSerialError(UsbSerialErrorCode.PORT_NOT_OPEN, "closed")),
        )
    }

    @Test
    fun unknownExceptionFallsBackToIoError() {
        assertEquals(UsbSerialErrorCode.IO_ERROR, mapException(RuntimeException("???")))
    }
}
