# Embedded Linux windows — experimental launcher patch

This fork adds the generic OWL 13 `LinuxApp` node. It embeds one foreground
Wayland program directly inside a face or widget, using the existing terminal
window renderer. There is no emulator, ROM scanner, game controller or emulator
package installation in this version.

## API

```owl
face "my linux app" {
  meta { owl: 13 }
  page: -1
  padding: 0
  LinuxApp {
    command: "my-wayland-program"
    fill: true
  }
}
```

`command` is a shell command executed with `bash -c` in the launcher's Linux
installation. As with `Terminal` and `run`, quote values from OWL with `sh(value)`.
It must run the client in the foreground, not daemonize or forward the request
to an already running instance. Its command is included in OWL's review output.

Each instance gets a unique Wayland app ID. The launcher exports it as
`OMALAUNCHER_APP_ID`, `SDL_VIDEO_WAYLAND_WMCLASS` (SDL2) and `SDL_APP_ID` (SDL3).
SDL clients that honour these settings need only `command`. For another toolkit,
pass `$OMALAUNCHER_APP_ID` to its application-ID option, or set the optional
`appId` property to the exact ID the program publishes. A fixed `appId` must be
unique among running embedded sessions. It is an ID, not a window title or a
.desktop filename; a mismatch leaves the node waiting and the unmatched window
opens as a normal Linux Android task. The waiting node displays the expected ID.

For example, foot is already installed, so the generic path can also be checked
without installing a demo dependency:

```owl
face "window smoke test" {
  meta { owl: 13 }
  page: -1
  LinuxApp {
    command: "foot --app-id=\"$OMALAUNCHER_APP_ID\" -e bash -c 'printf \"Embedded Wayland window\\n\"; exec bash'"
    fill: true
  }
}
```

## Interaction and lifetime

- A graphical program starts only after tapping its node. Neighbour-page preload
  does not start it. Tap again after its window appears to give it input.
- Graphical clients use the same pointer/touch bridge as other Linux windows.
  Activation does not automatically open the soft keyboard. The keyboard button
  opens it when needed. Back, a tap outside, leaving the page, or Android focus
  loss releases input. The surrounding face remains normal OWL UI.
- Leaving the page keeps the program alive, just like an embedded terminal. There
  is no assumed application-specific pause shortcut. The × button stops the
  owned proot process, even if no window has appeared yet; tap to start it again.
- Removing the widget, changing its command/ID or stopping its instance closes
  its window and cancels its owned process. Late windows during cancellation are
  closed instead of opening an independent task. Process errors show the last
  few output lines in the node, and offer retry by tapping.
- One toplevel is embedded per node. Additional toplevel windows open normally;
  existing Wayland popups continue to use the renderer's popup support.

This does not add X11/XWayland, Android app embedding, a WebView or a new GPU
backend. The client must be compatible with the existing Wayland compositor.
The first device test uses software rendering / shared-memory buffers.

## Demo and build

`linux-app.owl` is a self-contained face with a small Python/ctypes SDL2 client.
It draws coloured bars, changes colours on input and resizes with the node.
`linux-app-demo.py` is the same script, kept separately for inspection. There are
no Python package dependencies: the face declares Arch packages `python` and
`sdl2`, installed through the launcher's normal `requires` mechanism.

The separate **Linux Face Prototype** APK uses application ID
`dev.omalauncher.embedded` and its own Linux installation. It can coexist with
both the stock launcher and the earlier GBA prototype. Existing files and
packages are not shared between these apps. Its demo is seeded automatically;
normal debug/release builds expose `LinuxApp` but do not seed the test face.

On a supported Android development machine (JDK 25, SDK 37, pinned NDK):

```sh
./scripts/fetch-proot.sh
./gradlew :owl:test :app:testDebugUnitTest :owl:owlJar :app:assembleEmbedded :app:lintDebug
java -jar owl/build/cli/owl.jar check prototype/linux-app.owl
```

The **Linux face prototype APK** GitHub Actions workflow builds the isolated APK
with the Linux runtime. It runs automatically on relevant changes to `main` and
can also be started manually. Download the `linux-face-prototype` artifact and
install `app-embedded.apk`. Do not use the normal `app-debug.apk` for this test.
Fresh CI debug signing keys may differ between builds; no persistent signing key
is included in this repository.

Without an Android SDK, `python prototype/check-owl.py` compiles/tests OWL plus
command quoting and process cancellation, and checks the demo face. It does not
compile Compose or prove phone rendering. `python prototype/test-demo.py` checks
that the self-contained face contains the inspected demo script and, when SDL2
is available, exercises its rendering and input using SDL's dummy video driver.

## Required phone checks before an upstream PR

1. Start with the seeded SDL face: install its declared packages, tap to start,
   and confirm the picture is embedded without opening a separate task.
2. Activate the window, tap it to change colours, use the soft/hardware keyboard,
   and confirm the surrounding page can be reached again with Back/outside tap.
3. Change orientation, leave/return to the page, lock/unlock and switch apps.
   Confirm sizing, rendering and input ownership remain correct.
4. Stop/restart, remove/re-add, and edit the command while starting. No stray
   task or process should remain. Test a missing executable and retry.
5. Run two auto-ID nodes and confirm their input stays separate; test duplicate
   explicit IDs and an ID mismatch. Check ordinary Terminal nodes still work.

Compilation and host tests alone do not establish these device behaviours.
