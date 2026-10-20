# Pocket GBA — experimental launcher patch

This is source code for a modified Omalauncher, **not a face that works on the stock APK**.
The experimental `Gba { rom: absolutePath }` node requires this fork's OWL level 12.
No commercial ROMs or BIOS files are included.

Based on upstream commit `542c021bf4c1200162e4cf99bad34cd0264dcc22`.

## What the patch does

- Starts `mgba` from Arch's `mgba-sdl`, with a unique Wayland app ID.
- Reuses the launcher's embedded terminal window renderer, without launching an Android task.
- Draws touch controls next to the game: D-pad, A/B, L/R, Select and Start.
- Sends separate evdev press/release events; different buttons can be held together.
- Releases held keys and requests pause (mGBA SDL's Ctrl+P) when leaving the page,
  losing Android window focus, or returning to the library. Resume is manual.
- Starts the emulator only after a tap, not while preloading the neighbouring face.
- Uses a dedicated mGBA configuration directory (`~/.config/omalauncher/gba`)
  so existing personal key bindings do not change the expected defaults.
- Lists up to 200 uncompressed `.gba` files in a selectable Linux directory.
  The provided face is assigned `page: -1`.

## Build

Use a supported Android development machine with JDK 25, Android SDK platform 37,
CMake 3.22.1 and the NDK version specified in `scripts/fetch-proot.sh`.
The `:owl` module needs a Java 17 toolchain, which Gradle can provision.

```sh
./scripts/fetch-proot.sh
./gradlew :owl:test :app:testDebugUnitTest :owl:owlJar :app:assembleDebug
java -jar owl/build/cli/owl.jar check prototype/pocket-gba.owl
```

The manual GitHub Actions workflow **GBA prototype APK** builds the APK including
proot, runs tests and uploads an artifact. It must first exist in your own fork;
this working copy has not been published or uploaded anywhere.

On ARM Linux without Android SDK, run `python prototype/check-owl.py` to compile
the language module, run its JVM tests and statically check the face. It downloads
Kotlin/JUnit jars into the ignored `prototype/build/` directory. This does **not**
compile the Android/Compose changes or test emulation.

## Installation safety

The debug APK uses `dev.omalauncher`, like the installed launcher, but has a
different signing key from Play releases. Android will normally reject an
in-place update. **Do not uninstall your launcher just to try this prototype:**
that can delete its private Linux installation and settings. Back up first, and
prefer testing on a spare device or a separate Android profile where available.
Changing the application ID alone is not sufficient to promise isolation: some
existing integration paths and authorities may also need adjustment.

Once the modified launcher is installed in a safe test environment:

```sh
pacman -S --needed mgba-sdl lua python bash coreutils
mkdir -p ~/Games/GBA
# Copy your own .gba files into ~/Games/GBA.
cp /path/to/prototype/pocket-gba.owl ~/.config/omalauncher/widgets/
owl-check ~/.config/omalauncher/widgets/pocket-gba.owl
```

Open the preview PNG printed by `owl-check` and inspect the library layout.
Live emulation, audio and multi-touch must also be checked on the phone.
The unmodified launcher cannot validate or preview the new Gba node.

## Known limits / device test checklist

This is an unverified integration prototype until an APK has been compiled and
tested on-device. In particular, verify:

1. SDL honours `SDL_VIDEO_WAYLAND_WMCLASS` / `SDL_APP_ID`, so the window is claimed
   by the face instead of opening as a normal Linux window.
2. mGBA renders and scales correctly through the existing Wayland shm compositor.
   `SDL_RENDER_DRIVER=software` is a hint, not a guarantee that mGBA won't use GL.
3. Audio starts and pause/resume stays in sync. Pause is a keyboard toggle, not
   an acknowledged emulator API; startup and focus races still need testing.
4. Holding two or three buttons works; dragging away, navigating home, locking
   the phone and changing ROMs do not leave keys down.
5. A normally exiting mGBA writes its standard battery save alongside the ROM.
   No quick-save/quick-load UI, gamepad mapping, zip support or ROM thumbnails yet.
6. The first layout is portrait-first with scrolling on short screens; landscape
   needs layout refinement. Swipe outside the buttons to turn the page.

The patch does not add a native emulator library, generic WebView or arbitrary
third-party Android app embedding. Existing OWL files remain unchanged.
