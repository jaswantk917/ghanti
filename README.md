# Ghanti

A phone app for Android that keeps strangers quiet, except the delivery rider you're waiting for.

Calls from numbers that aren't in your contacts ring without a sound. They still show up, so nothing is blocked. But if an order or a ride is on its way (a Zomato, Swiggy, Blinkit, Zepto, Uber or Amazon notification, or an SMS saying "out for delivery"), Ghanti lets strangers ring normally and tells you who it probably is: *"Probably Bistro delivery"*.

Sister app to [Chitthi](https://github.com/jaswantk917/chitthi). Built with Kotlin and Jetpack Compose (Material 3 Expressive). Everything happens on the phone; nothing is sent anywhere.

## What it does

- **Silences strangers, lets deliveries through.** It reads notifications and recent SMS for words like "on the way" or "arriving today", and knows the common delivery, ride and courier apps. If the rider's number is in the message, the guess is confident.
- **Says who's calling.** The guess shows on the call screen, in the notification, in Recents and on the missed-call alert. When a call was silenced, Recents says so.
- **Second call rings.** Someone who calls again within 3 minutes usually means it, so the second call rings out loud.
- **Remembers who it was.** During or after a call, tap "Yes?" on the guess (or "Was it Ownly?" in Recents) and that call keeps the label. A later call from the same number shows "Ownly delivery last time", never a certainty, because delivery apps reuse masked numbers.
- **Call notes.** Write a note during a call (an order number, a gate, a time). It saves as you type and shows under that call in Recents.
- **A full phone app.** Call screen, keypad with T9 search, hold, mute, speaker, merge and swap, SIM choice, quick-reply SMS that knows when it's a delivery ("Please leave it at the door"), favourites, contacts and recents.
- **Made to feel good.** Morphing shapes, a breathing backdrop behind the caller's face, a header that tucks away as you scroll, blur under the bars, and light haptics.

## Small things worth knowing

| Do this | And |
|---|---|
| Tap a face in Recents or Contacts | Open the contact, or save a stranger's number |
| Tap a favourite with several numbers | Pick one, and optionally "always call this number" |
| Hold a favourite | All their numbers, or go back to asking every time |
| Hold a number on the call screen | Copy it (handy for masked delivery numbers) |
| Hold the typed number on the keypad | Paste a number from the clipboard |
| Tap call on an empty keypad | Bring back the last number you dialled |
| Hold backspace | Clear everything |
| Hold 0 | Type + |
| Tap speaker | Switch between speaker and phone |
| Hold speaker | Pick Bluetooth, headphones or a watch |
| Drag the scroll thumb in Contacts | Fly through the alphabet |
| Back on the Contacts tab | Return to Recents |

## Install

1. Download the APK from [Releases](https://github.com/jaswantk917/ghanti/releases) and install it.
2. Open Ghanti and make it your **phone app**.
3. Allow it as your **Caller ID & spam app**. Android asks that app which calls ring, so silencing only works with this.
4. Allow **notification access** so it can see deliveries on their way.

Your old phone app stays installed and can be switched back any time.

Needs Android 10 or newer on a 64-bit ARM phone.

## Permissions

| Permission | Why |
|---|---|
| Phone, call log | It's a phone app |
| Contacts | Names, photos, and knowing who's a stranger |
| SMS (read only) | Spotting "out for delivery" messages |
| Notification access | Spotting delivery and ride notifications |
| Notifications, full screen | Incoming calls over the lock screen |
| Audio settings, wake lock | Speaker and Bluetooth, screen off against your ear |

Ghanti has no internet permission.

## Build

```sh
./gradlew :app:assembleDebug        # debug build, installs next to a release one
./gradlew :app:testDebugUnitTest    # unit tests
```

Release builds sign with the keystore named by the `HEYRE_KEYSTORE`, `HEYRE_KEYSTORE_PASSWORD`, `HEYRE_KEY_ALIAS` and `HEYRE_KEY_PASSWORD` Gradle properties (for example in `~/.gradle/gradle.properties`). Without them the release APK is unsigned.

## Status

A personal project, built for daily use on a Pixel. No call recording and no spam database, on purpose. Issues and ideas are welcome.
