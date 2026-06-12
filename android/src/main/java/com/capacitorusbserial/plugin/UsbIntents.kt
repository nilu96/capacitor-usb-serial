package com.capacitorusbserial.plugin

import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager

/**
 * Single home for version-gated USB intent extras extraction, shared by the runtime
 * broadcast receiver and the plugin's cold-start launch-intent inspection.
 */
internal object UsbIntents {
    @Suppress("DEPRECATION")
    fun usbDevice(intent: Intent): UsbDevice? =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
}
