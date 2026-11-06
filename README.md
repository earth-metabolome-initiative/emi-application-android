# EMI Android

Container capacity comes from the assigned `Container_Models` row, not copied
fields on `Containers`. Positive `columns` and `rows` define finite capacity;
`-1` for both dimensions denotes an unlimited container. Model 7 has 9 × 9
places. Containers without a model or with invalid model dimensions are rejected
by the capacity checks. The app's Directus account needs read access to the
related model's `columns` and `rows` fields.

## Build and test on a headless Ubuntu server

Install JDK 17 and the [Android SDK command-line tools](https://developer.android.com/studio/command-line/sdkmanager).
Use `sdkmanager` to install `platform-tools`, `platforms;android-36`, and
`build-tools;35.0.0`. Set `ANDROID_HOME` to the SDK directory. Android Studio
and a local emulator are not required to build or deploy to a physical phone.

```sh
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/35.0.0:$PATH"
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

For wireless debugging, use an Android 11+ tablet/phone on a network that
can reach the server. On the device open **Developer options → Wireless
debugging**. The IP address and port on this main screen are for connecting.
Tap **Pair device with pairing code** (not QR code): its pop-up shows a
temporary *different* port and a six-digit code. Pair with that pop-up's
IP/port, enter the code when prompted, then check whether ADB connected
automatically:

```sh
adb pair PHONE_IP:PAIRING_POPUP_PORT
adb devices
```

If no device appears, close the pop-up and use the IP/port on the main
**Wireless debugging** screen:

```sh
adb connect PHONE_IP:MAIN_SCREEN_PORT
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.commons.emi.debug/com.commons.emi.LoginActivity
```

The debug build uses package `com.commons.emi.debug` and appears as **EMI
(Debug)**. It installs alongside `com.commons.emi` without modifying the
existing app or its data, even if that app has a different signing certificate.
To run the model-capacity tests on the connected device:

```sh
./gradlew :app:connectedDebugAndroidTest
```

The test runner may remove the debug app afterward; reinstall it with the
`adb install -r` command above before manual testing.

The server must be able to reach the device's pairing and connection ports;
Wi-Fi client isolation or firewall rules can block them. Do not expose ADB
ports to the internet. Sign in to Directus in **EMI (Debug)**, scan a container
assigned model 7 (e.g. container 14509) and verify the app reports remaining
places based on 81 minus occupied places. Also test a 1 × 1 sample container
and a model with `-1`/`-1` dimensions. The separately signed production build
uses `com.commons.emi.production`; it does not replace either existing icon.

## Bag-to-tube labels

In **Logistic → Bag to tube: print sticker**, select the destination tube's
single-sample container type and a Brady label size, scan the existing sample
bag's QR code, then tap **Attribute tube type and print sticker**. Connect an
M211 or M511 Brady printer from the navigation drawer first. Sizes refer to
Avery sticker dimensions, **not** Avery A4 sheets; the Big design can be
reflowed to fit continuous Brady tape:

| Size | Avery reference | Nominal design (length × width) |
| --- | --- | --- |
| Small | L4731 | 25.4 × 10 mm |
| Medium | L4732 | 35.6 × 16.9 mm |
| Big | L4736 | 45.7 × 21.2 mm |

Load continuous Brady tape with sufficient **printable** width, which is
slightly narrower than the physical tape. Big requires an M511 with at least
21.2 mm printable width; the M211 supports a maximum of 19.05 mm tape. The
app checks the installed tape before changing Directus data and will not
print these custom lengths on pre-cut stock. The QR code and readable
identifier are both included; verify the printed QR with the scanner before
applying it.

On continuous tape, the app turns the sticker clockwise when its design fits
across the printer's printable width, reflowing Big when necessary. The M511
reports 36.6 mm printable width on nominal 38.1 mm tape: Medium feeds
16.9 mm instead of 35.6 mm. Big's nominal 45.7 mm length cannot fit across
that tape, so it reflows to about 36.6 × 21.2 mm, retaining the large QR
and wrapping the identifier (for
example, `mnsl_` / `000036`). It feeds 21.2 mm instead of 45.7 mm; both
orientations add the printer's cut/feed allowance. Big only reflows when at
least 35.6 mm printable width is available; on narrower tape it keeps the
nominal dimensions and feeds the long side. Check a physical print for
legibility and cut clearance.

After changing label rendering, run
`./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.commons.emi.TubeLabelSizeTest`
on a connected tablet, then reinstall the debug APK (the test runner may
remove it). The QR and identifier should span the selected printed area, not
sit in a small corner of an otherwise blank label. Make a test print for each
size on suitable Brady stock before relabeling samples.

The same QR value is printed on a single
sticker; no new sample or identifier is created. The app changes the existing
sample container's `container_model` in Directus before printing, leaving its
other fields and parent relationship unchanged. Only sample models with a
positive volume and 1 × 1 capacity appear in the selector. An existing sample
container can have either a 1 × 1 model or no model yet; unassigned containers
must also have a matching `Field_Data.sample_id` (e.g. `mnsl_000068`). Containers
assigned to racks or other non-sample models cannot be relabeled here.

If printing fails after the model is saved, leave the scanned value on screen
and check the printer before retrying so you do not produce duplicate stickers.
Verify the printed QR by scanning the new tube and confirming it resolves to
the same sample. This changes a live Directus record: use a test sample first.

## Independent production release

The existing `com.commons.emi` installation was signed by an unavailable key.
The release build instead uses `com.commons.emi.production` and appears as
**EMI (Production)**. Debug remains `com.commons.emi.debug`. These are three
separate apps; the production app starts with its own local data and requires
a new Directus login. It uses the same remote Directus records.

Create a **permanent institutional signing key once** on the server. The
keystore and its passwords must be backed up securely outside Git; losing
them would prevent updates to the new production app as well. Run the
following interactively and choose a strong password when prompted (do not
put it in the command line or send it in chat):

```sh
umask 077
mkdir -p "$HOME/.local/share/emi-signing"
keytool -genkeypair -v \
  -keystore "$HOME/.local/share/emi-signing/emi-production.jks" \
  -alias emi-production -keyalg RSA -keysize 4096 -validity 10000
```

From this repository, with `ANDROID_HOME` and build-tools on `PATH` as above:

```sh
./gradlew :app:assembleRelease
zipalign -f 4 app/build/outputs/apk/release/app-release-unsigned.apk /tmp/emi-production-aligned.apk
apksigner sign --ks "$HOME/.local/share/emi-signing/emi-production.jks" \
  --ks-key-alias emi-production \
  --out /tmp/emi-production-v1.2.apk /tmp/emi-production-aligned.apk
apksigner verify --print-certs /tmp/emi-production-v1.2.apk
adb install -r /tmp/emi-production-v1.2.apk
adb shell am start -n com.commons.emi.production/com.commons.emi.LoginActivity
```

Verify one test sample and printer flow before switching from EMI (Debug).
Neither the original EMI nor EMI (Debug) needs uninstalling. For later
updates keep the same new keystore and package ID, increment `versionCode`,
rebuild, re-sign, and use `adb install -r`.
