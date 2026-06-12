package com.capacitorusbserial.plugin

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.hoho.android.usbserial.driver.ProbeTable
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialProber
import java.io.IOException
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

/**
 * Core plugin logic. Wraps usb-serial-for-android behind a handle-based facade. All public
 * methods either return a JSObject result (or Unit) or throw [UsbSerialError]; the bridge
 * layer converts thrown errors into `call.reject(message, code)`.
 *
 * Hardware I/O for a given port is always executed on that port's single-threaded executor
 * (see [onPort]) so the bulk read endpoint has a single owner and per-port operations are
 * serialized. The bridge dispatches each delegation on a background thread, so [onPort]'s
 * blocking wait never touches the main thread.
 */
class UsbSerialImpl(
    private val context: Context,
    private val emitter: (eventName: String, payload: JSObject) -> Unit,
    private val resolvePermission: (callbackId: String, granted: Boolean) -> Unit,
) {
    companion object {
        const val ACTION_USB_PERMISSION = "com.capacitorusbserial.plugin.USB_PERMISSION"
        const val EXTRA_DEVICE_ID = "com.capacitorusbserial.plugin.DEVICE_ID"
        private const val DEFAULT_READ_BUFFER = 16 * 1024
        private const val DEFAULT_READ_TIMEOUT = 1000
        private const val DEFAULT_WRITE_TIMEOUT = 1000
    }

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val store = HandleStore()

    // Custom prober state (Task 20). When null, the default prober is used.
    private var customProber: UsbSerialProber? = null
    private val customTable = ProbeTable()
    private var hasCustomMappings = false

    // deviceId -> callbackId awaiting a permission broadcast (Task 21).
    private val pendingPermission = ConcurrentHashMap<String, String>()

    // Buffered cold-start attach payload, replayed to the first 'attached' listener (Req 12.5).
    @Volatile private var bufferedAttach: JSObject? = null

    private fun prober(): UsbSerialProber = customProber ?: UsbSerialProber.getDefaultProber()

    // ----------------------------------------------------------------------
    // Task 19 — Discovery
    // ----------------------------------------------------------------------

    fun listDevices(): JSObject {
        val drivers: List<UsbSerialDriver> = prober().findAllDrivers(usbManager)
        val arr = JSArray()
        for (driver in drivers) {
            val device = driver.device
            val deviceId = store.registerDevice(device)
            val info = JSObject()
            info.put("deviceId", deviceId)
            info.put("vendorId", device.vendorId)
            info.put("productId", device.productId)
            info.put("deviceName", device.deviceName)
            info.put("serialNumber", safeSerial(device, driver))
            info.put("driverType", EnumMaps.driverTypeName(driver))
            info.put("portCount", driver.ports.size)
            info.put("hasPermission", usbManager.hasPermission(device))
            arr.put(info)
        }
        return JSObject().put("devices", arr)
    }

    /** getSerial() requires permission and an open-ish handle on some drivers; null-safe. */
    private fun safeSerial(
        device: android.hardware.usb.UsbDevice,
        @Suppress("UNUSED_PARAMETER") driver: UsbSerialDriver,
    ): String? =
        if (!usbManager.hasPermission(device)) {
            null
        } else {
            runCatching {
                val conn = usbManager.openDevice(device) ?: return null
                try {
                    conn.serial
                } finally {
                    conn.close()
                }
            }.getOrNull()
        }

    // ----------------------------------------------------------------------
    // Task 20 — Custom prober registration
    // ----------------------------------------------------------------------

    fun registerDriver(
        vendorId: Int,
        productId: Int,
        driverType: String,
        replaceDefaults: Boolean,
    ) {
        val driverClass = EnumMaps.driverClassFromType(driverType)
        customTable.addProduct(vendorId, productId, driverClass)
        hasCustomMappings = true
        // Rebuild a prober: optionally seeded from the default table, then augmented.
        val table =
            if (replaceDefaults) {
                customTable
            } else {
                val merged = UsbSerialProber.getDefaultProbeTable()
                merged.addProduct(vendorId, productId, driverClass)
                merged
            }
        customProber = UsbSerialProber(table)
    }

    // ----------------------------------------------------------------------
    // Task 21 — Permission
    // ----------------------------------------------------------------------

    fun hasPermission(deviceId: String): JSObject {
        val device = store.getDevice(deviceId)
        return JSObject().put("granted", usbManager.hasPermission(device))
    }

    /** Issues the system permission prompt; resolution arrives via [onPermissionResult]. */
    fun requestPermission(deviceId: String, callbackId: String) {
        val device = store.getDevice(deviceId)
        if (usbManager.hasPermission(device)) {
            resolvePermission(callbackId, true)
            return
        }
        store.markPermissionRequested(deviceId)
        pendingPermission[deviceId] = callbackId
        // Explicit intent (scoped to our own package) + FLAG_IMMUTABLE. On Android 14
        // (API 34+) the system rejects a PendingIntent that is both FLAG_MUTABLE and wraps
        // an implicit intent, so requestPermission() silently never shows the dialog. The
        // UsbManager still delivers its result extras to an immutable intent, so immutability
        // costs us nothing here. See https://developer.android.com/reference/android/app/PendingIntent
        val intent =
            Intent(ACTION_USB_PERMISSION).apply {
                setPackage(context.packageName)
                putExtra(EXTRA_DEVICE_ID, deviceId)
            }
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pi = PendingIntent.getBroadcast(context, deviceId.hashCode(), intent, flags)
        usbManager.requestPermission(device, pi)
    }

    /** Called by the broadcast receiver when a permission result arrives. */
    fun onPermissionResult(deviceId: String, granted: Boolean) {
        val callbackId = pendingPermission.remove(deviceId) ?: return
        resolvePermission(callbackId, granted)
    }

    // ----------------------------------------------------------------------
    // Task 22 — Open / close / isOpen
    // ----------------------------------------------------------------------

    fun open(deviceId: String, portNum: Int): JSObject {
        val device = store.getDevice(deviceId)
        if (!usbManager.hasPermission(device)) {
            val code =
                if (store.wasPermissionRequested(deviceId)) {
                    UsbSerialErrorCode.PERMISSION_DENIED
                } else {
                    UsbSerialErrorCode.NEEDS_PERMISSION
                }
            throw UsbSerialError(code, "USB permission not granted for device $deviceId")
        }
        val driver =
            prober().probeDevice(device)
                ?: throw UsbSerialError(UsbSerialErrorCode.NO_DEVICE, "No driver for device $deviceId")
        if (portNum < 0 || portNum >= driver.ports.size) {
            throw UsbSerialError(UsbSerialErrorCode.INVALID_PARAMS, "portNum $portNum out of range")
        }
        val connection =
            usbManager.openDevice(device)
                ?: throw UsbSerialError(UsbSerialErrorCode.IO_ERROR, "openDevice returned null")
        val port = driver.ports[portNum]
        try {
            port.open(connection)
        } catch (e: IOException) {
            connection.close()
            throw UsbSerialError(UsbSerialErrorCode.IO_ERROR, "Failed to open port: ${e.message}")
        }
        val executor = Executors.newSingleThreadExecutor()
        val handle = store.addPort(deviceId, port, connection, executor)
        return JSObject().put("portId", handle.portId)
    }

    fun close(portId: String) {
        // Validate existence first so a bad id reports PORT_NOT_OPEN.
        store.getPort(portId)
        store.reapPort(portId)
    }

    fun isOpen(portId: String): JSObject {
        val handle = store.getPortOrNull(portId)
        val open = handle?.port?.isOpen ?: false
        return JSObject().put("isOpen", open)
    }

    // ----------------------------------------------------------------------
    // Task 23 — setParameters / getPortInfo
    // ----------------------------------------------------------------------

    fun setParameters(
        portId: String,
        baudRate: Int,
        dataBits: Int,
        stopBits: Double,
        parity: String,
    ) {
        val mappedData = EnumMaps.dataBitsFromNumber(dataBits)
        val mappedStop = EnumMaps.stopBitsFromNumber(stopBits)
        val mappedParity = EnumMaps.parityFromString(parity)
        val handle = store.getPort(portId)
        onPort(handle) { handle.port.setParameters(baudRate, mappedData, mappedStop, mappedParity) }
    }

    fun getPortInfo(portId: String): JSObject {
        val handle = store.getPort(portId)
        val driver = handle.port.driver
        return JSObject()
            .put("deviceId", handle.deviceId)
            .put("portNum", handle.port.portNumber)
            .put("serialNumber", runCatching { handle.connection.serial }.getOrNull())
            .put("driverType", EnumMaps.driverTypeName(driver))
    }

    // ----------------------------------------------------------------------
    // Task 24 — One-shot read / write
    // ----------------------------------------------------------------------

    fun read(portId: String, length: Int?, timeout: Int?): JSObject {
        val handle = store.getPort(portId)
        if (handle.stream != null) {
            throw UsbSerialError(
                UsbSerialErrorCode.INVALID_STATE,
                "Cannot one-shot read while a stream is active on port $portId",
            )
        }
        val buf = ByteArray(length ?: DEFAULT_READ_BUFFER)
        val count =
            onPortIo(handle) { handle.port.read(buf, timeout ?: DEFAULT_READ_TIMEOUT) }
        val data = if (count <= 0) "" else Base64Util.encode(buf.copyOf(count))
        return JSObject().put("data", data)
    }

    fun write(portId: String, data: String, timeout: Int?): JSObject {
        val handle = store.getPort(portId)
        val bytes = Base64Util.decode(data)
        val stream = handle.stream
        if (stream != null) {
            // Route through the manager's async write path while it owns the port.
            stream.writeAsync(bytes)
        } else {
            onPortIo(handle) { handle.port.write(bytes, timeout ?: DEFAULT_WRITE_TIMEOUT) }
        }
        return JSObject().put("bytesWritten", bytes.size)
    }

    fun writeAsync(portId: String, data: String) {
        val handle = store.getPort(portId)
        val bytes = Base64Util.decode(data)
        val stream = handle.stream
        if (stream != null) {
            stream.writeAsync(bytes)
        } else {
            // No stream: fire-and-forget on the per-port executor so we resolve immediately.
            runCatching {
                handle.executor.submit { runCatching { handle.port.write(bytes, DEFAULT_WRITE_TIMEOUT) } }
            }
        }
    }

    // ----------------------------------------------------------------------
    // Task 29 — Streaming control
    // ----------------------------------------------------------------------

    fun startReading(
        portId: String,
        readTimeout: Int?,
        writeTimeout: Int?,
        readBufferSize: Int?,
        writeBufferSize: Int?,
        readQueueBufferCount: Int?,
        threadPriority: Int?,
    ) {
        val handle = store.getPort(portId)
        if (handle.stream?.isRunning() == true) {
            throw UsbSerialError(UsbSerialErrorCode.INVALID_STATE, "Stream already running on port $portId")
        }
        val manager =
            SerialStreamManager(
                portId = portId,
                port = handle.port,
                onData = { bytes ->
                    emitter("data", JSObject().put("portId", portId).put("data", Base64Util.encode(bytes)))
                },
                onError = { e -> onStreamError(handle, e) },
            )
        manager.applyTuning(
            readTimeout,
            writeTimeout,
            readBufferSize,
            writeBufferSize,
            readQueueBufferCount,
            threadPriority,
        )
        manager.start()
        handle.stream = manager
    }

    fun stopReading(portId: String) {
        val handle = store.getPort(portId)
        handle.stream?.stop()
        handle.stream = null
    }

    fun getStreamState(portId: String): JSObject {
        val handle = store.getPort(portId)
        val state = handle.stream?.state() ?: "stopped"
        return JSObject().put("state", state)
    }

    fun getStreamConfig(portId: String): JSObject {
        val handle = store.getPort(portId)
        val stream =
            handle.stream
                ?: throw UsbSerialError(UsbSerialErrorCode.INVALID_STATE, "No active stream on port $portId")
        val cfg = stream.configSnapshot()
        return JSObject()
            .put("readTimeout", cfg.readTimeout)
            .put("writeTimeout", cfg.writeTimeout)
            .put("readBufferSize", cfg.readBufferSize)
            .put("writeBufferSize", cfg.writeBufferSize)
            .put("readQueueBufferCount", cfg.readQueueBufferCount)
    }

    private fun onStreamError(handle: PortHandle, e: Exception) {
        emitter("error", JSObject().put("portId", handle.portId).put("message", e.message ?: "stream error"))
        // If the device is gone, perform detach cleanup and notify.
        if (isDisconnect(handle, e)) {
            val deviceId = handle.deviceId
            store.reapDevice(deviceId)
            emitter("detached", JSObject().put("deviceId", deviceId))
        }
    }

    // ----------------------------------------------------------------------
    // Task 25 — Control lines
    // ----------------------------------------------------------------------

    fun getControlLines(portId: String): JSObject {
        val handle = store.getPort(portId)
        val lines = onPort(handle) { handle.port.controlLines }
        return JSObject()
            .put("rts", lines.contains(com.hoho.android.usbserial.driver.UsbSerialPort.ControlLine.RTS))
            .put("cts", lines.contains(com.hoho.android.usbserial.driver.UsbSerialPort.ControlLine.CTS))
            .put("dtr", lines.contains(com.hoho.android.usbserial.driver.UsbSerialPort.ControlLine.DTR))
            .put("dsr", lines.contains(com.hoho.android.usbserial.driver.UsbSerialPort.ControlLine.DSR))
            .put("cd", lines.contains(com.hoho.android.usbserial.driver.UsbSerialPort.ControlLine.CD))
            .put("ri", lines.contains(com.hoho.android.usbserial.driver.UsbSerialPort.ControlLine.RI))
    }

    fun getSupportedControlLines(portId: String): JSObject {
        val handle = store.getPort(portId)
        val lines = onPort(handle) { handle.port.supportedControlLines }
        val obj = JSObject()
        for (cl in com.hoho.android.usbserial.driver.UsbSerialPort.ControlLine.values()) {
            obj.put(cl.name.lowercase(), lines.contains(cl))
        }
        return obj
    }

    fun getControlLine(portId: String, line: String): JSObject {
        val handle = store.getPort(portId)
        val value =
            onPort(handle) {
                when (line) {
                    "cd" -> handle.port.cd
                    "cts" -> handle.port.cts
                    "dsr" -> handle.port.dsr
                    "dtr" -> handle.port.dtr
                    "ri" -> handle.port.ri
                    "rts" -> handle.port.rts
                    else -> throw UsbSerialError(UsbSerialErrorCode.INVALID_PARAMS, "Unknown line $line")
                }
            }
        return JSObject().put("value", value)
    }

    fun setDTR(portId: String, value: Boolean) {
        val handle = store.getPort(portId)
        onPort(handle) { handle.port.dtr = value }
    }

    fun setRTS(portId: String, value: Boolean) {
        val handle = store.getPort(portId)
        onPort(handle) { handle.port.rts = value }
    }

    // ----------------------------------------------------------------------
    // Task 26 — Flow control
    // ----------------------------------------------------------------------

    fun setFlowControl(portId: String, mode: String) {
        val fc = EnumMaps.flowControlFromString(mode)
        val handle = store.getPort(portId)
        onPort(handle) { handle.port.flowControl = fc }
    }

    fun getFlowControl(portId: String): JSObject {
        val handle = store.getPort(portId)
        val fc = onPort(handle) { handle.port.flowControl }
        return JSObject().put("mode", EnumMaps.flowControlToString(fc))
    }

    fun getSupportedFlowControl(portId: String): JSObject {
        val handle = store.getPort(portId)
        val set = onPort(handle) { handle.port.supportedFlowControl }
        val arr = JSArray()
        for (fc in set) arr.put(EnumMaps.flowControlToString(fc))
        return JSObject().put("modes", arr)
    }

    fun getXON(portId: String): JSObject {
        val handle = store.getPort(portId)
        val value = onPort(handle) { handle.port.xon }
        return JSObject().put("value", value)
    }

    // ----------------------------------------------------------------------
    // Task 27 — Maintenance
    // ----------------------------------------------------------------------

    fun purgeHwBuffers(portId: String, purgeWrite: Boolean, purgeRead: Boolean) {
        val handle = store.getPort(portId)
        onPort(handle) { handle.port.purgeHwBuffers(purgeWrite, purgeRead) }
    }

    fun setBreak(portId: String, value: Boolean) {
        val handle = store.getPort(portId)
        onPort(handle) { handle.port.setBreak(value) }
    }

    fun setReadQueue(portId: String, bufferCount: Int, bufferSize: Int) {
        if (bufferCount <= 0 || bufferSize <= 0) {
            throw UsbSerialError(UsbSerialErrorCode.INVALID_PARAMS, "bufferCount/bufferSize must be > 0")
        }
        val handle = store.getPort(portId)
        onPort(handle) { handle.port.setReadQueue(bufferCount, bufferSize) }
    }

    fun getReadQueueConfig(portId: String): JSObject {
        val handle = store.getPort(portId)
        return onPort(handle) {
            JSObject()
                .put("bufferCount", handle.port.readQueueBufferCount)
                .put("bufferSize", handle.port.readQueueBufferSize)
        }
    }

    // ----------------------------------------------------------------------
    // Attach/detach + teardown support (used by the receiver/plugin, tasks 30/31)
    // ----------------------------------------------------------------------

    fun handleAttached(device: android.hardware.usb.UsbDevice, coldStart: Boolean) {
        val deviceId = store.registerDevice(device)
        val payload =
            JSObject()
                .put("deviceId", deviceId)
                .put("vendorId", device.vendorId)
                .put("productId", device.productId)
                .put("deviceName", device.deviceName)
        if (coldStart) bufferedAttach = payload
        emitter("attached", payload)
    }

    fun onDeviceDetached(device: android.hardware.usb.UsbDevice) {
        val deviceId = store.deviceIdFor(device)
        if (store.hasDevice(deviceId) || store.portsForDevice(deviceId).isNotEmpty()) {
            store.reapDevice(deviceId)
        }
        emitter("detached", JSObject().put("deviceId", deviceId))
    }

    /** Replay any buffered cold-start attach to a freshly-registered listener (Req 12.5). */
    fun flushBufferedAttach() {
        val payload = bufferedAttach ?: return
        bufferedAttach = null
        emitter("attached", payload)
    }

    fun teardown() {
        store.reapAll()
        pendingPermission.clear()
    }

    val permissionAction: String get() = ACTION_USB_PERMISSION

    // ----------------------------------------------------------------------
    // Per-port execution helpers
    // ----------------------------------------------------------------------

    /** Run [block] on the port's executor, blocking for the result; unwrap thrown errors. */
    private fun <T> onPort(handle: PortHandle, block: () -> T): T {
        val future =
            try {
                handle.executor.submit(Callable { block() })
            } catch (e: RejectedExecutionException) {
                throw UsbSerialError(UsbSerialErrorCode.PORT_NOT_OPEN, "Port ${handle.portId} is closed")
            }
        try {
            return future.get()
        } catch (e: java.util.concurrent.ExecutionException) {
            throw e.cause ?: e
        }
    }

    /** Like [onPort] but classifies IOExceptions as disconnect vs transient IO. */
    private fun <T> onPortIo(handle: PortHandle, block: () -> T): T =
        try {
            onPort(handle, block)
        } catch (e: IOException) {
            if (isDisconnect(handle, e)) {
                store.reapDevice(handle.deviceId)
                throw UsbSerialError(UsbSerialErrorCode.DEVICE_DISCONNECTED, "Device disconnected")
            }
            throw UsbSerialError(UsbSerialErrorCode.IO_ERROR, e.message ?: "I/O error")
        }

    private fun isDisconnect(handle: PortHandle, e: Throwable): Boolean {
        if (e is UsbSerialError) return e.code == UsbSerialErrorCode.DEVICE_DISCONNECTED
        val device = runCatching { handle.connection }.getOrNull()
        // Device considered gone if it is no longer present in the system device list.
        val present = usbManager.deviceList.values.any { it.deviceName == handle.port.device.deviceName }
        return !present || device == null || !handle.port.isOpen
    }
}
