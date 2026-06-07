# capacitor-usb-serial

A Capacitor v7 plugin that exposes the full functionality of
[mik3y/usb-serial-for-android](https://github.com/mik3y/usb-serial-for-android) to
JavaScript/TypeScript. Talk to USB‑to‑serial adapters and serial‑over‑USB devices
(FTDI, Prolific PL2303, Silicon Labs CP210x, CH340/CH341, CDC/ACM) plugged into an
Android device over USB‑C / OTG — no root required.

> **Platforms:** Android only. iOS and web are stubs that reject every call with
> `UNSUPPORTED_PLATFORM` (Android is the only platform the underlying library supports).

## Install

```bash
npm install capacitor-usb-serial
npx cap sync
```

The Android library is pulled from JitPack. Ensure JitPack is available to your app's
Gradle (the plugin's own `build.gradle` already declares it; if your app overrides
repositories, add it):

```gradle
// android/build.gradle (project) — repositories
maven { url 'https://jitpack.io' }
```

## How objects map across the bridge

Native serial objects can't cross the Capacitor bridge, so the plugin is **handle-based**:

- Discovery returns a `deviceId` per attached device.
- `open()` returns a `portId`; every later call takes that `portId`.
- All binary data crosses as **base64 strings**.
- Continuous reads are delivered as **events** (`data` / `error`).

## Permission model (mirrors the native library)

USB permission must be granted **before** a port can be opened:

- `requestPermission({ deviceId })` shows the system dialog and resolves `{ granted }`.
- `open()` rejects with `NEEDS_PERMISSION` if you never asked, or `PERMISSION_DENIED`
  if the user declined.

## Verification snippet

Copy‑paste into any Capacitor app to confirm the plugin works end‑to‑end:

```ts
import { UsbSerial } from 'capacitor-usb-serial';

async function demo() {
  const { devices } = await UsbSerial.listDevices();
  if (devices.length === 0) {
    console.log('No USB serial devices connected');
    return;
  }
  const device = devices[0];
  console.log('Found', device.driverType, device.deviceName);

  if (!device.hasPermission) {
    const { granted } = await UsbSerial.requestPermission({ deviceId: device.deviceId });
    if (!granted) throw new Error('Permission denied');
  }

  const { portId } = await UsbSerial.open({ deviceId: device.deviceId, portNum: 0 });
  await UsbSerial.setParameters({
    portId,
    baudRate: 115200,
    dataBits: 8,
    stopBits: 1,
    parity: 'none',
  });

  // Stream incoming bytes (base64).
  const sub = await UsbSerial.addListener('data', ({ data }) => {
    console.log('RX', atob(data));
  });
  await UsbSerial.startReading({ portId });

  // Send something (base64-encoded).
  await UsbSerial.write({ portId, data: btoa('AT\r\n') });

  // ...later:
  // await UsbSerial.stopReading({ portId });
  // await sub.remove();
  // await UsbSerial.close({ portId });
}
```

## Optional: auto-attach (launch/foreground on plug-in)

Add to your app's `android/app/src/main/AndroidManifest.xml` inside your main
`<activity>`:

```xml
<intent-filter>
  <action android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED" />
</intent-filter>
<meta-data
  android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED"
  android:resource="@xml/device_filter" />
```

A template `device_filter.xml` ships with the plugin
(`android/src/main/res/xml/device_filter.xml`); copy it into your app's `res/xml/` and
list the VID/PID pairs you care about. When the app cold-starts from an attach intent the
device appears in the next `listDevices()`, and an `attached` event is replayed
best‑effort to the first registered listener.

## API overview

Discovery & prober: `listDevices`, `registerDriver`.
Permission: `requestPermission`, `hasPermission`.
Lifecycle: `open`, `close`, `isOpen`, `getPortInfo`.
Config: `setParameters`.
I/O: `read`, `write`, `writeAsync`.
Streaming: `startReading`, `stopReading`, `getStreamState`, `getStreamConfig`, `data`/`error` events.
Control lines: `getControlLines`, `getSupportedControlLines`, `getCD/CTS/DSR/DTR/RI/RTS`, `setDTR`, `setRTS`.
Flow control: `setFlowControl`, `getFlowControl`, `getSupportedFlowControl`, `getXON` (+ `CHAR_XON`/`CHAR_XOFF`).
Maintenance: `purgeHwBuffers`, `setBreak`, `setReadQueue`, `getReadQueueConfig`.
Events: `attached`, `detached`, `data`, `error`.

See `src/definitions.ts` for the complete typed contract and error codes.

## Error codes

Every rejection carries a stable `code`: `NO_DEVICE`, `NEEDS_PERMISSION`,
`PERMISSION_DENIED`, `PORT_NOT_OPEN`, `INVALID_PARAMS`, `INVALID_STATE`, `IO_ERROR`,
`DEVICE_DISCONNECTED`, `UNSUPPORTED_OPERATION`, `UNSUPPORTED_PLATFORM`.

## License

MIT
