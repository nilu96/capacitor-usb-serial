package com.capacitorusbserial.plugin

import com.getcapacitor.PluginCall
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.io.IOException

/**
 * Proves the shared bridge helper converts thrown exceptions into coded rejections — so no
 * exception ever escapes across the bridge (Req 14.2). The type->code mapping itself is
 * covered by MapExceptionTest; here we verify the reject wiring.
 */
class BridgeRejectTest {
    @Test
    fun convertsUsbSerialErrorToItsCode() {
        val call = mock(PluginCall::class.java)
        rejectWith(call, UsbSerialError(UsbSerialErrorCode.PORT_NOT_OPEN, "closed"))
        verify(call).reject("closed", "PORT_NOT_OPEN")
    }

    @Test
    fun convertsUnsupportedOperationToUnsupportedOperationCode() {
        val call = mock(PluginCall::class.java)
        rejectWith(call, UnsupportedOperationException("no flow control"))
        verify(call).reject("no flow control", "UNSUPPORTED_OPERATION")
    }

    @Test
    fun convertsPlainIoExceptionToIoError() {
        val call = mock(PluginCall::class.java)
        rejectWith(call, IOException("transport blip"))
        verify(call).reject("transport blip", "IO_ERROR")
    }

    @Test
    fun usesCodeNameWhenMessageNull() {
        val call = mock(PluginCall::class.java)
        rejectWith(call, IllegalArgumentException())
        verify(call).reject("INVALID_PARAMS", "INVALID_PARAMS")
    }
}
