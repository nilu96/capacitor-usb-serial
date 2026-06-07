# Testing capacitor-usb-serial

Two layers: automated tests for the platform‑agnostic logic (run in CI, no hardware), and
a manual hardware procedure that exercises real USB I/O (cannot be automated).

## Automated tests

```bash
# TypeScript (web stub + contract)
npm test

# Android unit tests (helpers, mapping, handle store, bridge conversion)
cd android && ./gradlew testDebugUnitTest
```

Covered:

| Area | Test |
|---|---|
| Web stub rejects every method `UNSUPPORTED_PLATFORM`; no-op listener | `src/web.test.ts` |
| Exception→code mapping ordering | `MapExceptionTest.kt` |
| Base64 lossless for all 256 byte values + zero-length; malformed→`INVALID_PARAMS` | `Base64UtilTest.kt` |
| Enum/driver mapping valid + invalid | `EnumMapsTest.kt` |
| Handle store create/lookup/reap, stale id→`NO_DEVICE`, unknown port→`PORT_NOT_OPEN`, concurrency | `HandleStoreTest.kt` |
| Bridge converts thrown errors to coded `reject` (Req 14.2) | `BridgeRejectTest.kt` |
| `UsbSerialImpl` orchestration vs. a mocked USB stack: permission gating (`NEEDS_PERMISSION`/`PERMISSION_DENIED`), `listDevices` shaping, read timeout→empty, `read`/`write` semantics, streaming exclusivity (`INVALID_STATE`), enum mapping at the port, control/flow lines, detach cleanup, disconnect→`DEVICE_DISCONNECTED` | `UsbSerialImplTest.kt` |

> The `UsbSerialImplTest` suite uses Robolectric + Mockito to drive the real impl logic
> against mocked `UsbManager`/`UsbSerialPort`/`UsbSerialDriver` — no hardware. It covers
> the orchestration the pure-helper tests don't. What it cannot cover (real serial bytes,
> `SerialInputOutputManager` threading against a live port, the system permission dialog)
> remains the manual hardware procedure below.

## Manual hardware procedure

**Hardware:** an Android device (USB‑C/OTG) plus at least one of:
- a **CH340** USB‑serial adapter (loopback TX↔RX, or wired to a device that echoes), and/or
- a **CDC/ACM** board (e.g. Arduino Uno) running a sketch that echoes bytes back.

Build a minimal app that imports `@leeskies/capacitor-usb-serial`, or use the verification snippet
in the README. Record PASS/FAIL for each step.

### 1. Discovery
- Plug in the adapter. Call `listDevices()`.
- **Expected:** one entry with the correct `driverType` (`Ch34xSerialDriver` /
  `CdcAcmSerialDriver`), correct `vendorId`/`productId`, `portCount ≥ 1`,
  `hasPermission:false` initially.

### 2. Permission — grant
- Call `requestPermission({ deviceId })`; approve the dialog.
- **Expected:** resolves `{ granted: true }`; `hasPermission()` now `true`.

### 3. Permission — deny
- On a fresh attach, call `requestPermission` and **deny**.
- **Expected:** `{ granted: false }`; `open()` rejects with code `PERMISSION_DENIED`.
- Also verify: calling `open()` **without** ever requesting → `NEEDS_PERMISSION`.

### 4. Open + parameters
- `open({ deviceId, portNum: 0 })` → `{ portId }`.
- `setParameters({ portId, baudRate: 115200, dataBits: 8, stopBits: 1, parity: 'none' })`.
- **Expected:** both resolve; `isOpen({ portId })` → `true`; `getPortInfo` returns matching
  `portNum`/`driverType`.

### 5. Write
- `write({ portId, data: btoa('hello\n') })`.
- **Expected:** resolves `{ bytesWritten: 6 }`.

### 6. Streamed read
- `addListener('data', cb)`, then `startReading({ portId })`.
- Send bytes from the device (or rely on loopback of step 5).
- **Expected:** `data` events arrive with base64 payloads decoding to the sent bytes.
- `getStreamState({ portId })` → `running`; `getStreamConfig` returns tuning values.
- Verify one‑shot `read()` while streaming rejects `INVALID_STATE`.
- `stopReading({ portId })` → `getStreamState` returns `stopped`.

### 7. Control lines
- `getSupportedControlLines({ portId })`, then `setRTS({ portId, value:true })` /
  `setDTR({ portId, value:true })`.
- **Expected:** `getControlLines()` reflects the toggles for supported lines; unsupported
  lines reject `UNSUPPORTED_OPERATION`.

### 8. Detach cleanup
- While open (and ideally streaming), physically unplug the adapter.
- **Expected:** a `detached` event fires; in‑flight `read`/`write` reject
  `DEVICE_DISCONNECTED`; the old `deviceId`/`portId` are invalidated (subsequent calls
  reject `NO_DEVICE`/`PORT_NOT_OPEN`); no lingering threads.

### 9. Concurrency (two ports)
- With two adapters (or one device exposing two ports), `open` both, `startReading` on both.
- **Expected:** `data` events carry the correct `portId` for each stream; the two streams
  do not interfere; closing one leaves the other streaming.

### 10. Cold-start auto-attach (optional)
- Configure the auto-attach intent-filter (README) and `device_filter.xml`.
- With the app **not running**, plug in a matching device and approve launch.
- **Expected:** the app starts; the device appears in `listDevices()`; an `attached` event
  is delivered to the first registered listener.
