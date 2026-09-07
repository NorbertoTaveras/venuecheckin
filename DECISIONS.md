# Decisions

## Already Inside
The app handles already-inside in two ways:
- Geofences are registered with `INITIAL_TRIGGER_ENTER`.
- After registration succeeds, the app checks the current fused location and compares it against the venue radii.

The explicit current-location check makes the behavior easier to reason about during emulator testing, where geofence transition delivery can be delayed.

## Geofence Transitions
The app registers only `ENTER` and `EXIT` transitions. It does not use dwell or loitering logic.
Transitions are delivered to a `BroadcastReceiver` through a `PendingIntent`. On Android S and newer, the geofence `PendingIntent` is mutable because Play Services geofencing requires it.

## BLE Scanning
BLE scanning starts only after a confirmed venue enter and stops on exit. The scanner is not intended to run while the user is outside every venue.
The app uses the platform `BluetoothLeScanner` API directly and parses iBeacon manufacturer data without a beacon library.

## Foreground Service
I used a foreground service for BLE scanning because a geofence transition can arrive when the app UI is not active. The foreground service makes the active scan visible to the user and gives Android a clearer reason to keep the short-lived scan running while inside a venue.
If Bluetooth permission is missing or the service cannot start, the app stays in the geographic `Inside` state and skips BLE ranging instead of crashing.

## RSSI Smoothing
RSSI is smoothed with a simple running mean over the last five readings. This keeps the implementation small while avoiding raw per-packet RSSI jitter in the UI.

Proximity buckets:
- Immediate: RSSI >= -55
- Near: RSSI >= -70
- Far: RSSI >= -90
- Unknown: weaker than -90

## Beacon Lost
The app treats the beacon as lost if no matching advertisement is received for 5 seconds. At that point it falls back from `InRange` to `Inside`.
Five seconds is short enough to make the UI responsive during testing, but long enough to avoid flickering from a missed advertisement or two.

## Battery Tradeoffs
The main battery tradeoff is BLE scanning. To reduce unnecessary work, the scanner is gated by geofence state and stops on exit. The app also scans only for the active venue's beacon identity.

## Known Limitation
I could not test BLE on hardware because my available Android device was too old.

## With More Time
I would add a unit test for iBeacon payload parsing, verify the full BLE flow on a newer Android device with an iPhone advertiser, and record the complete `Outside -> Inside -> InRange -> Exit` flow.
