package com.capacitorusbserial.plugin

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import com.hoho.android.usbserial.driver.UsbSerialPort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
class HandleStoreTest {
    private fun mockDevice(id: Int, name: String): UsbDevice {
        val d = mock(UsbDevice::class.java)
        `when`(d.deviceId).thenReturn(id)
        `when`(d.deviceName).thenReturn(name)
        return d
    }

    private fun addPort(store: HandleStore, deviceId: String): PortHandle {
        val port = mock(UsbSerialPort::class.java)
        val conn = mock(UsbDeviceConnection::class.java)
        val exec = Executors.newSingleThreadExecutor()
        return store.addPort(deviceId, port, conn, exec)
    }

    @Test
    fun createLookupReap() {
        val store = HandleStore()
        val deviceId = store.registerDevice(mockDevice(1, "/dev/bus/usb/001/002"))
        val handle = addPort(store, deviceId)

        assertEquals(handle.portId, store.getPort(handle.portId).portId)

        store.reapPort(handle.portId)
        verify(handle.port).close()
        verify(handle.connection).close()
        assertTrue(handle.executor.isShutdown)
    }

    @Test
    fun staleDeviceIdThrowsNoDevice() {
        val store = HandleStore()
        try {
            store.getDevice("missing")
            fail("expected NO_DEVICE")
        } catch (e: UsbSerialError) {
            assertEquals(UsbSerialErrorCode.NO_DEVICE, e.code)
        }
    }

    @Test
    fun reapDeviceInvalidatesDeviceAndPorts() {
        val store = HandleStore()
        val deviceId = store.registerDevice(mockDevice(2, "/dev/bus/usb/001/003"))
        val handle = addPort(store, deviceId)

        store.reapDevice(deviceId)

        assertFalse(store.hasDevice(deviceId))
        try {
            store.getPort(handle.portId)
            fail("expected PORT_NOT_OPEN")
        } catch (e: UsbSerialError) {
            assertEquals(UsbSerialErrorCode.PORT_NOT_OPEN, e.code)
        }
    }

    @Test
    fun unknownPortIdThrowsPortNotOpen() {
        val store = HandleStore()
        try {
            store.getPort("port_999")
            fail("expected PORT_NOT_OPEN")
        } catch (e: UsbSerialError) {
            assertEquals(UsbSerialErrorCode.PORT_NOT_OPEN, e.code)
        }
    }

    @Test
    fun permissionRequestedTracking() {
        val store = HandleStore()
        val deviceId = store.registerDevice(mockDevice(3, "/dev/bus/usb/001/004"))
        assertFalse(store.wasPermissionRequested(deviceId))
        store.markPermissionRequested(deviceId)
        assertTrue(store.wasPermissionRequested(deviceId))
    }

    @Test
    fun concurrentAddProducesUniquePorts() {
        val store = HandleStore()
        val deviceId = store.registerDevice(mockDevice(4, "/dev/bus/usb/001/005"))
        val threads = 16
        val latch = CountDownLatch(threads)
        val created = AtomicInteger(0)
        val ids = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

        repeat(threads) {
            Thread {
                val h = addPort(store, deviceId)
                ids.add(h.portId)
                created.incrementAndGet()
                latch.countDown()
            }.start()
        }
        latch.await()

        assertEquals(threads, created.get())
        assertEquals("all port ids must be unique", threads, ids.size)
        assertEquals(threads, store.portsForDevice(deviceId).size)
        assertNotNull(store.getPort(ids.first()))
    }
}
