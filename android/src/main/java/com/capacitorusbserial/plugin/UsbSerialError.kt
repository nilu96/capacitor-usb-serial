package com.capacitorusbserial.plugin

import java.io.IOException

/** Stable error codes mirrored 1:1 with the TypeScript `UsbSerialErrorCode` union. */
enum class UsbSerialErrorCode {
    NO_DEVICE,
    NEEDS_PERMISSION,
    PERMISSION_DENIED,
    PORT_NOT_OPEN,
    INVALID_PARAMS,
    INVALID_STATE,
    IO_ERROR,
    DEVICE_DISCONNECTED,
    UNSUPPORTED_OPERATION,
    UNSUPPORTED_PLATFORM,
}

/**
 * Internal exception carrying a stable [code]. Thrown by the impl layer and converted to
 * `call.reject(message, code.name)` by the bridge.
 */
class UsbSerialError(
    val code: UsbSerialErrorCode,
    message: String,
) : Exception(message)

/**
 * Maps an arbitrary thrown exception to a stable error code. Order matters: the specific
 * RuntimeException subtypes are matched before the IOException catch-all so that, e.g., a
 * driver's `UnsupportedOperationException` is never misreported as a disconnect/IO error.
 *
 * Note: disconnect detection (IOException on a known-detached port -> DEVICE_DISCONNECTED)
 * is decided at the call site, which constructs a UsbSerialError(DEVICE_DISCONNECTED)
 * directly; a plain IOException reaching here is treated as a transient IO_ERROR.
 */
fun mapException(e: Throwable): UsbSerialErrorCode =
    when (e) {
        is UsbSerialError -> e.code
        is UnsupportedOperationException -> UsbSerialErrorCode.UNSUPPORTED_OPERATION
        is IllegalStateException -> UsbSerialErrorCode.INVALID_STATE
        is IllegalArgumentException -> UsbSerialErrorCode.INVALID_PARAMS
        is IOException -> UsbSerialErrorCode.IO_ERROR
        else -> UsbSerialErrorCode.IO_ERROR
    }
