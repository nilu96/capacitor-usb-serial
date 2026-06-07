package com.capacitorusbserial.plugin

import android.util.Base64

/**
 * Lossless byte <-> base64 string conversion for moving binary payloads across the bridge.
 * Uses NO_WRAP so the encoded string is a single line with no trailing newline.
 */
object Base64Util {
    fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    fun decode(data: String): ByteArray =
        try {
            Base64.decode(data, Base64.NO_WRAP)
        } catch (e: IllegalArgumentException) {
            throw UsbSerialError(UsbSerialErrorCode.INVALID_PARAMS, "Invalid base64 data: ${e.message}")
        }
}
