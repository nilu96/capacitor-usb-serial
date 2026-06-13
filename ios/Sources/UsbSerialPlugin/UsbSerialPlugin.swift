import Foundation
import Capacitor

/**
 * iOS stub. The underlying mik3y/usb-serial-for-android library and the Android USB
 * Host API have no iOS counterpart (USB serial access on iOS is limited to MFi /
 * ExternalAccessory, a different domain), so every bridged method rejects with the
 * stable `UNSUPPORTED_PLATFORM` code — mirroring the web stub. This exists only so the
 * package installs and `pod install` / SPM resolve cleanly on iOS targets.
 */
@objc(UsbSerialPlugin)
public class UsbSerialPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "UsbSerialPlugin"
    public let jsName = "UsbSerial"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "listDevices", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "registerDriver", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "requestPermission", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "hasPermission", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "open", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "close", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "isOpen", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getPortInfo", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "setParameters", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "read", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "write", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "writeAsync", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "startReading", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "stopReading", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getStreamState", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getStreamConfig", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getControlLines", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getSupportedControlLines", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getCD", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getCTS", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getDSR", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getDTR", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getRI", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getRTS", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "setDTR", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "setRTS", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "setFlowControl", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getFlowControl", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getSupportedFlowControl", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getXON", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "purgeHwBuffers", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "setBreak", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "setReadQueue", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getReadQueueConfig", returnType: CAPPluginReturnPromise)
    ]

    private func unsupported(_ call: CAPPluginCall) {
        call.reject("USB serial is only available on Android.", "UNSUPPORTED_PLATFORM")
    }

    @objc func listDevices(_ call: CAPPluginCall) { unsupported(call) }
    @objc func registerDriver(_ call: CAPPluginCall) { unsupported(call) }
    @objc func requestPermission(_ call: CAPPluginCall) { unsupported(call) }
    @objc func hasPermission(_ call: CAPPluginCall) { unsupported(call) }
    @objc func open(_ call: CAPPluginCall) { unsupported(call) }
    @objc func close(_ call: CAPPluginCall) { unsupported(call) }
    @objc func isOpen(_ call: CAPPluginCall) { unsupported(call) }
    @objc func getPortInfo(_ call: CAPPluginCall) { unsupported(call) }
    @objc func setParameters(_ call: CAPPluginCall) { unsupported(call) }
    @objc func read(_ call: CAPPluginCall) { unsupported(call) }
    @objc func write(_ call: CAPPluginCall) { unsupported(call) }
    @objc func writeAsync(_ call: CAPPluginCall) { unsupported(call) }
    @objc func startReading(_ call: CAPPluginCall) { unsupported(call) }
    @objc func stopReading(_ call: CAPPluginCall) { unsupported(call) }
    @objc func getStreamState(_ call: CAPPluginCall) { unsupported(call) }
    @objc func getStreamConfig(_ call: CAPPluginCall) { unsupported(call) }
    @objc func getControlLines(_ call: CAPPluginCall) { unsupported(call) }
    @objc func getSupportedControlLines(_ call: CAPPluginCall) { unsupported(call) }
    @objc func getCD(_ call: CAPPluginCall) { unsupported(call) }
    @objc func getCTS(_ call: CAPPluginCall) { unsupported(call) }
    @objc func getDSR(_ call: CAPPluginCall) { unsupported(call) }
    @objc func getDTR(_ call: CAPPluginCall) { unsupported(call) }
    @objc func getRI(_ call: CAPPluginCall) { unsupported(call) }
    @objc func getRTS(_ call: CAPPluginCall) { unsupported(call) }
    @objc func setDTR(_ call: CAPPluginCall) { unsupported(call) }
    @objc func setRTS(_ call: CAPPluginCall) { unsupported(call) }
    @objc func setFlowControl(_ call: CAPPluginCall) { unsupported(call) }
    @objc func getFlowControl(_ call: CAPPluginCall) { unsupported(call) }
    @objc func getSupportedFlowControl(_ call: CAPPluginCall) { unsupported(call) }
    @objc func getXON(_ call: CAPPluginCall) { unsupported(call) }
    @objc func purgeHwBuffers(_ call: CAPPluginCall) { unsupported(call) }
    @objc func setBreak(_ call: CAPPluginCall) { unsupported(call) }
    @objc func setReadQueue(_ call: CAPPluginCall) { unsupported(call) }
    @objc func getReadQueueConfig(_ call: CAPPluginCall) { unsupported(call) }
}
