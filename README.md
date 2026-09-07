# Venue Check-In
Small Android take-home app for geofence-based venue check-in and BLE beacon ranging.

## Requirements
- Android Studio
- Android SDK with a recent emulator or Android device
- Kotlin / Jetpack Compose project
- `minSdk 28`

## Build And Test

```sh
./gradlew testDebugUnitTest assembleDebug --no-configuration-cache --max-workers=2
```

## Venue Data
The app ships three hardcoded venues:
- Kaseya Center
- Bayfront Park
- CityPlace Doral

The main test venue used during development was CityPlace Doral:

```text
Latitude: 25.8069771
Longitude: -80.3311359
Radius: 150 meters
```

## Beacon Configuration
The app scans for iBeacon advertisements.

Use this configuration for CityPlace Doral:

```text
Beacon type: iBeacon
UUID: FDA50693-A4E2-4FB1-AFCF-C6EB07647825
Major: 100
Minor: 3
```

Recommended advertiser apps from the challenge:
- Beacon Simulator
- Beacon Toy
- nRF Connect

## Permissions
Grant these permissions in the app:
- Precise location
- Background location
- Nearby devices / Bluetooth scan

Background location is requested separately from foreground location.

## Location Testing
Emulator location can be tested with Android Studio Extended Controls, or with adb:

```sh
adb emu geo fix <longitude> <latitude>
```

Example outside location:

```text
Latitude: 25.750000
Longitude: -80.400000
```

Example inside CityPlace Doral:

```text
Latitude: 25.8069771
Longitude: -80.3311359
```

## Reproduce The Flow

1. Start outside the venue.
2. Open the app.
3. Grant location and Bluetooth permissions.
4. Tap `Start Monitoring`.
5. Move the mocked location inside CityPlace Doral.
6. The app should show `Inside CityPlace Doral`.
7. Start the iBeacon advertiser with the UUID, major, and minor listed above.
8. The app should show beacon proximity and smoothed RSSI.
9. Stop the advertiser and wait about 5 seconds.
10. The app should return to `Inside` with `Beacon not detected`.
11. Move the mocked location outside the venue.
12. The app should show `Outside` and stop BLE scanning.

## Verification Note
I verified geofence registration, already-inside behavior, enter, exit, and scan stopping with emulator location testing. I could not test BLE on hardware because my available Android device was too old.
