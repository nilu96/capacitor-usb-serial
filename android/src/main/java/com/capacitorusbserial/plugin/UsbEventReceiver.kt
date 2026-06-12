package com.capacitorusbserial.plugin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager

/**
 * Routes the three USB-related broadcasts to the impl:
 *  - the plugin's own permission-result action (resolves the saved permission call),
 *  - ACTION_USB_DEVICE_ATTACHED (emit `attached`),
 *  - ACTION_USB_DEVICE_DETACHED (emit `detached`, reap handles, fail pending ops).
 *
 * Registered at runtime in UsbSerialPlugin.load() and unregistered in handleOnDestroy().
 */
class UsbEventReceiver(private val impl: UsbSerialImpl) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            UsbSerialImpl.ACTION_USB_PERMISSION -> {
                val deviceId = intent.getStringExtra(UsbSerialImpl.EXTRA_DEVICE_ID) ?: return
                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                impl.onPermissionResult(deviceId, granted)
            }
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                val device = UsbIntents.usbDevice(intent) ?: return
                impl.handleAttached(device, coldStart = false)
            }
            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                val device = UsbIntents.usbDevice(intent) ?: return
                impl.onDeviceDetached(device)
            }
        }
    }
}
