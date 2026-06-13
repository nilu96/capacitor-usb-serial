// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapacitorUsbSerial",
    platforms: [.iOS(.v13)],
    products: [
        .library(
            name: "CapacitorUsbSerial",
            targets: ["UsbSerialPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", branch: "main")
    ],
    targets: [
        .target(
            name: "UsbSerialPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/UsbSerialPlugin")
    ]
)
