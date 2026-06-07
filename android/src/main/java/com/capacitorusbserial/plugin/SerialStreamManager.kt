package com.capacitorusbserial.plugin

import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.util.SerialInputOutputManager

/**
 * Wraps a single [SerialInputOutputManager] for one streaming port. Translates the
 * library's listener callbacks into plugin events and exposes the manager's lifecycle and
 * tuning. Tuning knobs are applied before [start]; changing them after start is rejected by
 * the library (and we never attempt it — a second startReading is rejected upstream).
 */
class SerialStreamManager(
    val portId: String,
    port: UsbSerialPort,
    onData: (ByteArray) -> Unit,
    onError: (Exception) -> Unit,
) {
    private val ioManager: SerialInputOutputManager =
        SerialInputOutputManager(
            port,
            object : SerialInputOutputManager.Listener {
                override fun onNewData(data: ByteArray) = onData(data)

                override fun onRunError(e: Exception) = onError(e)
            },
        )

    /** Validate and apply optional tuning before start(). Throws INVALID_PARAMS on bad ranges. */
    fun applyTuning(
        readTimeout: Int?,
        writeTimeout: Int?,
        readBufferSize: Int?,
        writeBufferSize: Int?,
        readQueueBufferCount: Int?,
        threadPriority: Int?,
    ) {
        readTimeout?.let {
            requireRange(it >= 0, "readTimeout", it)
            ioManager.readTimeout = it
        }
        writeTimeout?.let {
            requireRange(it >= 0, "writeTimeout", it)
            ioManager.writeTimeout = it
        }
        readBufferSize?.let {
            requireRange(it > 0, "readBufferSize", it)
            ioManager.readBufferSize = it
        }
        writeBufferSize?.let {
            requireRange(it > 0, "writeBufferSize", it)
            ioManager.writeBufferSize = it
        }
        readQueueBufferCount?.let {
            requireRange(it > 0, "readQueueBufferCount", it)
            ioManager.setReadQueue(it)
        }
        threadPriority?.let { ioManager.setThreadPriority(it) }
    }

    fun start() = ioManager.start()

    fun stop() = ioManager.stop()

    fun writeAsync(bytes: ByteArray) = ioManager.writeAsync(bytes)

    /** Lowercase state matching the TS StreamState union. */
    fun state(): String = ioManager.state.name.lowercase()

    fun isRunning(): Boolean = ioManager.state == SerialInputOutputManager.State.RUNNING

    /** Snapshot of the manager's current tuning values. */
    fun configSnapshot(): StreamConfigSnapshot =
        StreamConfigSnapshot(
            readTimeout = ioManager.readTimeout,
            writeTimeout = ioManager.writeTimeout,
            readBufferSize = ioManager.readBufferSize,
            writeBufferSize = ioManager.writeBufferSize,
            readQueueBufferCount = ioManager.readQueueBufferCount,
        )

    private fun requireRange(ok: Boolean, field: String, value: Int) {
        if (!ok) throw UsbSerialError(UsbSerialErrorCode.INVALID_PARAMS, "Invalid $field: $value")
    }
}

data class StreamConfigSnapshot(
    val readTimeout: Int,
    val writeTimeout: Int,
    val readBufferSize: Int,
    val writeBufferSize: Int,
    val readQueueBufferCount: Int,
)
