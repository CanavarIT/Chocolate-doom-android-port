# Chocolate Doom 3.1.1 — Android Port

A port of the classic [Chocolate Doom](https://www.chocolate-doom.org/) 3.1.1 to Android, with a virtual gamepad and full touch controls. Built entirely by hand: SDL2, SDL2_mixer, and the Doom engine itself were all compiled on-device through Android NDK 29 for arm64.

---

## What this is

**Chocolate Doom** is a faithful recreation of the original Doom from 1993. No visual enhancements, no 3D, no new renderers. Just Vanilla Doom, the way it was on DOS.

This repository is the **Android port**, which:

- Runs the original Chocolate Doom 3.1.1 engine (not GZDoom, not PrBoom+)
- Targets **arm64** (modern Android devices)
- Uses **SDL2** for graphics/input and **SDL2_mixer** for sound and MIDI
- Ships with a **virtual gamepad**: analog joystick, FIRE, USE, RUN, ESC, MAP, and weapon switching
- **Bundles the WAD inside the APK** — the Shareware `doom1.wad` lives in `assets/` and is extracted on first launch
- **Locks landscape orientation** from the very start (no portrait flash)
- Works **without root**, without Termux, without any third-party app

---

## How it was built

The entire project was built **on the phone itself** through the AndroidIDE terminal. No PC, no Android Studio, no x86_64 cross-compilation. Everything ran on an arm64 device.

### Why NDK 29 instead of 27

The project started with NDK 27.3.13750724. The very first attempt to build SDL2 failed with:

```
error: executable's TLS segment is underaligned:
alignment is 8 for ARM64 Bionic, needs to be at least 64
```

This is a known issue: NDK 27 switched to 64-byte TLS alignment, but the old Bionic in Android API < 29 does not support it. The fix is to use NDK 29 with `APP_PLATFORM := android-29`. That's what we did.

### Building the components

Build order:

1. **SDL2 2.30.9** — foundation for graphics, input, events

   ```
   ndk-build NDK_PROJECT_PATH=. APP_BUILD_SCRIPT=./Android.mk \
             NDK_APPLICATION_MK=./Application.mk -j2
   ```

   `Application.mk`:
   ```
   APP_ABI := arm64-v8a
   APP_PLATFORM := android-29
   APP_STL := c++_shared
   APP_OPTIM := release
   ```

2. **SDL2_mixer 2.8.1** — sound and MIDI
   - TiMidity is disabled by default (`SUPPORT_MID_TIMIDITY ?= false`)
   - We enabled it — Doom uses MIDI for music
   - Disabled WavPack and GME (not needed, they pull in external libraries)
   - Needed a `PREBUILT_SHARED_LIBRARY` block at the top to link against SDL2
   - Had to expose **both** include paths — `prebuilt/include` and `prebuilt/include/SDL2` — because `SDL_mixer.h` does `#include "SDL_stdinc.h"` without the `SDL2/` prefix

3. **Chocolate Doom 3.1.1** — the engine itself
   - Sources from GitHub: `chocolate-doom/chocolate-doom` (tag 3.1.1)
   - Wrote a custom `Android.mk` — the project has no native Android build system
   - Excluded files incompatible with Android: `d_dedicated.c`, `net_dedicated.c`, `i_winmusic.c`, `i_videohr.c`, `i_cdmus.c`, `w_file_win32.c`
   - Hit duplicate-symbol issues: `z_native.c` vs `z_zone.c` (both define `Z_Malloc`), and `net_sdl_module` was defined twice — solved with `filter-out` in `Android.mk`
   - `net_sdl.c` was replaced with a stub (`net_sdl_stub.c`) because SDL_net is not built yet
   - F-keys and other special keys are not recognized by Doom's key table — only printable ASCII and DOS scancodes work correctly

Result: **4 native libraries** in `jniLibs/arm64-v8a/`:

| File | Size | Purpose |
|---|---|---|
| `libSDL2.so` | ~1.4 MB | Core SDL2 |
| `libSDL2_mixer.so` | ~303 KB | Sound and MIDI |
| `libchocdoom.so` | ~956 KB | Doom engine |
| `libc++_shared.so` | ~8.9 MB | C++ STL (pulled in as dependency) |

---

## Virtual gamepad

The mobile controls are written from scratch in Java. The `GamepadOverlay` layer is drawn on top of `SDLSurface` and renders all buttons.

### Classes

- **`JoystickView.java`** — analog joystick (bottom-left). Sends `KEYCODE_DPAD_UP/DOWN/LEFT/RIGHT` via `SDLActivity.onNativeKeyDown/Up`.
- **`GamepadButton.java`** — universal button. Supports three modes:
  - normal (hold key down)
  - toggle (for RUN — tap to toggle)
  - cyclic (for weapon switching — each tap sends the next keycode)
- **`FireButton.java`** — early prototype of the FIRE button (kept for reference, not used)
- **`GamepadOverlay.java`** — container that assembles all buttons and lays them out on screen

### Buttons

| Button | Action | Android keycode |
|---|---|---|
| Joystick | Walk/turn | `DPAD_*` |
| FIRE | Shoot | `KEYCODE_CTRL_LEFT (113)` |
| USE | Open doors | `KEYCODE_SPACE (62)` |
| RUN | Run (toggle) | `KEYCODE_SHIFT_LEFT (59)` |
| ESC | Doom menu | `KEYCODE_ESCAPE (111)` |
| MAP | Automap | `KEYCODE_TAB (61)` |
| W+ | Next weapon | `KEYCODE_HOME (122)` |
| W- | Previous weapon | `KEYCODE_END (123)` |

Buttons are **semi-transparent**; they become brighter when pressed. Size and position can be tweaked in `GamepadOverlay.init()`.

---

## WAD and configs in assets

Problem: Chocolate Doom on Android **does not have access to external storage** (no permissions, no path), and looks for files in `configdir`, which SDL2 returns as:

```
/data/user/0/com.chocdoom.android/files/
```

Solution — **bundle the WAD and configs inside the APK** and extract them at first launch into `getFilesDir()`.

### Extraction logic (in `SDLActivity.java`)

```java
private String copyAssetIfNeeded(String assetName, boolean overwrite) {
    java.io.File target = new java.io.File(getFilesDir(), assetName);
    if (target.exists() && !overwrite) {
        return target.getAbsolutePath();
    }
    try {
        InputStream in = getAssets().open(assetName);
        FileOutputStream out = new FileOutputStream(target);
        byte[] buf = new byte[16384];
        int len;
        while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        out.flush(); out.close(); in.close();
    } catch (IOException e) { /* log */ }
    return target.getAbsolutePath();
}
```

Called from `getArguments()`:

```java
protected String[] getArguments() {
    String iwadPath = copyAssetIfNeeded("doom1.wad", false);
    String cfgPath = copyAssetIfNeeded("default.cfg", true);
    String extraCfgPath = copyAssetIfNeeded("chocolate-doom.cfg", true);
    return new String[] {
        "-iwad", iwadPath,
        "-config", cfgPath,
        "-extraconfig", extraCfgPath
    };
}
```

### Why two config files

Chocolate Doom reads settings from **two** collections:

- `default.cfg` — the main config (Vanilla Doom settings)
- `chocolate-doom.cfg` — extra settings (Chocolate-specific: weapon cycling, and many others)

Weapon keys (`key_weapon1..7`, `key_nextweapon`, `key_prevweapon`) are registered in the **extra** collection, so they only work from `chocolate-doom.cfg`.

### The `scantokey[]` gotcha

The `key_*` settings are of type `DEFAULT_KEY`, not `DEFAULT_INT`. When the config is read, the value goes through the `scantokey[]` translation table (DOS scancode → Doom keycode).

This means you **cannot** write a Doom keycode directly. For example, to map `key_nextweapon` to `KEY_HOME (199)`, you need to write the DOS scancode **71** in the config — `scantokey[71] == KEY_HOME`.

Our working `chocolate-doom.cfg`:

```
key_weapon1 2
key_weapon2 3
key_weapon3 4
key_weapon4 5
key_weapon5 6
key_weapon6 7
key_weapon7 8
key_nextweapon 71
key_prevweapon 79
```

Where:
- `2..8` — DOS scancodes for keys `1..7`
- `71` → `KEY_HOME (199)` → weapon "next"
- `79` → `KEY_END (207)` → weapon "previous"

### Why "fast down-up" is a problem

Doom's input system reads `keydown` state once per game tic (~28.5 ms). If we send `keyDown` and `keyUp` in the same frame, Doom never sees the key as pressed.

Solution: `GamepadButton` sends `keyDown` immediately, but delays `keyUp` by **100 ms** via `Handler.postDelayed()`. This guarantees the key is registered before being released.

---

## Orientation lock

SDL2 by default calls `setOrientation()` only after the window is created, which causes a brief portrait flash. Fixes applied:

1. `AndroidManifest.xml`:
   ```xml
   android:screenOrientation="sensorLandscape"
   android:configChanges="orientation|keyboardHidden|screenSize|..."
   ```

2. `SDLActivity.setOrientationBis()` — patched so that when no hint is given and `resizable == true`, it forces `SCREEN_ORIENTATION_SENSOR_LANDSCAPE` instead of `FULL_USER`.

---

## Requirements

- **Android 10 (API 29)** or higher
- **arm64-v8a** device (essentially all modern phones)
- ~15 MB free space for the APK
- ~50 MB for the extracted WAD and runtime

---

## Building from this repo

The `.so` files are **already committed**, so you can clone and build the APK directly. No need to rebuild the native stack.

```bash
git clone https://github.com/CanavarIT/Chocolate-doom-android-port.git
cd Chocolate-doom-android-port
./gradlew assembleDebug
```

The APK will appear in `app/build/outputs/apk/debug/`.

If you want to rebuild the native libraries yourself — you need:
- **Android NDK 29.0.14033849**
- Sources of SDL2 2.30.9, SDL2_mixer 2.8.1, Chocolate Doom 3.1.1
- Custom `Android.mk` files (see the "How it was built" section)

---

## Project structure

```
ChocDoom/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/
│       │   ├── doom1.wad              # Shareware Doom WAD
│       │   ├── default.cfg            # Main config (empty — weapon keys live in extra config)
│       │   └── chocolate-doom.cfg     # Extra config (weapon keys)
│       ├── java/
│       │   ├── com/chocdoom/android/gamepad/
│       │   │   ├── GamepadOverlay.java    # Virtual gamepad container
│       │   │   ├── GamepadButton.java     # Universal button
│       │   │   ├── JoystickView.java      # Analog joystick
│       │   │   └── FireButton.java        # Early prototype (unused)
│       │   └── org/libsdl/app/            # SDL2 Java bridge (9 classes)
│       ├── jniLibs/arm64-v8a/
│       │   ├── libSDL2.so
│       │   ├── libSDL2_mixer.so
│       │   ├── libchocdoom.so
│       │   └── libc++_shared.so
│       └── res/                           # Layouts, icons, themes
├── gradle/
├── build.gradle
├── settings.gradle
├── gradlew
├── gradlew.bat
├── .gitignore
└── README.md
```

---

## Known limitations

- **Multiplayer is not working.** SDL_net is not built yet, and `net_sdl.c` is replaced with a stub. Single-player only.
- **Only `arm64-v8a` is built.** 32-bit ARM, x86, and x86_64 devices are not supported.
- **The WAD is fixed at `doom1.wad` (Shareware).** To use a full version of Doom, you'd need to replace the file in `assets/` or add a file picker UI.
- **No mouse look / grab.** Touch input is mapped to mouse by SDL2 (tap = click, swipe = move), but there's no "mouse lock" mode for free rotation, because on Android SDL2 always runs fullscreen and `grabmouse` is a no-op.
- **Diagnostic logs remain in the code.** Look for `CHOCDOOM_CFG2`, `CHOCDOOM_CFG3`, `CHOCDOOM_GR`, `CHOCDOOM_SDL`, `CHOCDOOM_DEBUG` tags in LogWire. These were left in for debugging and add slight overhead. They will be removed in a future cleanup.
- **No save game UI.** Doom's internal save system works (via F-keys not bound to anything), but there's no mobile-friendly way to trigger it yet.

---

## Roadmap

- [ ] Remove diagnostic logging
- [ ] Build SDL_net and restore full multiplayer support
- [ ] Add mouse look (swipe-to-turn) mode
- [ ] Add in-game settings menu (WAD picker, control layout, audio volume)
- [ ] Build `armeabi-v7a` for older devices
- [ ] Add save game buttons (Quick Save / Quick Load)

---

## License

This project is licensed under the **GNU General Public License v2.0**, the same license as the original Chocolate Doom.

Original Chocolate Doom:
- Copyright (C) 2005-2024 Simon Howard and contributors
- https://www.chocolate-doom.org/

SDL2 and SDL2_mixer:
- Copyright (C) 1997-2024 Sam Lantinga and contributors
- Licensed under the zlib license

Doom is a trademark of id Software. The Shareware WAD (`doom1.wad`) is freely redistributable under id Software's original shareware terms.

---

## Credits

- **Chocolate Doom** — Simon Howard and contributors
- **SDL2 / SDL2_mixer** — Sam Lantinga and contributors
- **Android port** — CanavarIT, 2026

Build it, play it, share it.
