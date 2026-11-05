# RetroSalon

**One launcher for every console on your Android TV, built for the Microsoft Xbox Nano controller.**

RetroSalon puts all your retro games in a single library on the TV, from the NES to the Switch. Pick a game with the controller and RetroSalon opens it in the right emulator. You no longer need to open RetroArch, then Dolphin, then PPSSPP, or remember which app runs which console.

- **24 consoles, one screen.** Nintendo, Sega, Sony, NEC, SNK, Atari and arcade games, sorted by console.
- **Made for the Xbox Nano controller.** The whole interface works with the D-pad and the A/B buttons, and RetroSalon tells you when the controller is connected.
- **Hybrid storage.** Games can be on the TV, on a USB drive, or on a NAS/PC at home. Network games are copied to the TV when you press play, then kept in a cache.
- **Uses the emulators you already have.** RetroSalon is a launcher; it includes no emulator, no ROM and no BIOS.

| | |
|---|---|
| Version | 0.1.0 |
| Minimum Android | **Android 8.0 (API 26)** |
| Target Android | **Android 17 (API 37)** |
| Devices | Android TV and Google TV (also works on phones and tablets in landscape) |
| Interface language | French |

---

## The Microsoft Xbox Nano controller

The Xbox Nano (Bluetooth, USB-C) connects to Android like any Xbox Wireless Controller. Android maps it to the standard gamepad buttons, so RetroArch, PPSSPP, Dolphin and most emulators set it up on their own.

**Pairing it with the TV:**

1. On the TV: **Settings → Remotes & Accessories → Pair accessory** (the name varies by brand).
2. Turn the controller on with the Xbox button, then hold the **pair** button until the logo blinks fast.
3. Pick **Xbox Wireless Controller** in the list.

RetroSalon shows the controller's name in the sidebar (*Manette : Xbox Wireless Controller*). If no controller is connected, it shows a reminder to pair the Xbox Nano. The *Réglages* screen repeats these steps.

**Keeping the controller up to date.** Firmware updates can fix Bluetooth problems such as lag or disconnections. The usual way is the *Xbox Accessories* app on Windows, or an Xbox console. Some sources say the **Xbox app on Android** can also update it. This isn't the main official route, and it can be slower:

1. Pair the controller with an Android phone or tablet over Bluetooth.
2. Open the *Xbox* app.
3. Go to the connected controller's settings. If an update is available, an option such as *Update firmware* appears; select it.

In the app, the **D-pad** moves between items, **A** opens a game or button, and **B** goes back.

---

## Two players

Pair a second controller the same way: a second Xbox Nano, or any Bluetooth or USB gamepad. Either controller can drive RetroSalon. Once the game starts, the **emulator** decides which controller is player 1 and which is player 2:

| Emulator (consoles) | Two players |
|---|---|
| RetroArch (NES, SNES, Mega Drive, Master System, PC Engine, PlayStation with SwanStation, Neo Geo, arcade, Atari 2600…) | **Automatic.** The first controller connected is player 1, the second is player 2. If they're swapped: *Settings → Input → Port 2 Controls → Device Index*. |
| Dolphin (GameCube, Wii), DuckStation (PlayStation), NetherSX2 / ARMSX2 (PlayStation 2) | **One-time setup.** In the emulator's controller settings, turn on port 2 and pick the second controller. |
| Flycast and Redream (Dreamcast), M64Plus FZ (Nintendo 64, *Player map*), Yaba Sanshiro 2 (Saturn), ePSXe (PlayStation), Eden (Switch), MAME4droid (Neo Geo, arcade) | Several controllers supported. Assign them in the emulator's controls settings. |
| PPSSPP (PSP), melonDS / DraStic (DS), Azahar (3DS) | No two players on one screen. These handhelds did multiplayer between several consoles. |

Tips:

- Connect both controllers **before** starting the game. Every emulator then sees them both.
- **Xbox 360 controllers** are not Bluetooth. A wired one works over USB; a wireless one needs Microsoft's USB wireless receiver.

> Two-controller play hasn't been tested on real hardware yet. The table describes how each emulator usually handles players.

---

## Consoles and emulators

Put your games in **one folder per console**. RetroSalon recognizes the folder names below. Case, spaces, hyphens and underscores are ignored, so `Mega Drive`, `mega-drive` and `MEGADRIVE` all work. Cartridge games up to the Nintendo DS can also be stored as `.zip` or `.7z` (not 3DS or Switch games).

| Console | Folder name | Also accepted | Emulators, in order of preference |
|---|---|---|---|
| NES / Famicom | `nes` | `famicom`, `fc` | RetroArch (Mesen, FCEUmm) |
| Super Nintendo | `snes` | `sfc`, `superfamicom`, `supernintendo` | RetroArch (Snes9x) |
| Nintendo 64 | `n64` | `nintendo64` | M64Plus FZ, RetroArch (Mupen64Plus-Next) |
| Game Boy | `gb` | `gameboy` | RetroArch (Gambatte, mGBA) |
| Game Boy Color | `gbc` | `gameboycolor` | RetroArch (Gambatte, mGBA) |
| Game Boy Advance | `gba` | `gameboyadvance` | RetroArch (mGBA) |
| Nintendo DS | `nds` | `ds` | melonDS, DraStic |
| Nintendo 3DS | `n3ds` | `3ds` | Azahar |
| GameCube | `gc` | `gamecube`, `ngc` | Dolphin |
| Wii | `wii` | | Dolphin |
| Nintendo Switch | `switch` | `nsw` | Eden |
| Master System | `mastersystem` | `sms` | RetroArch (Genesis Plus GX) |
| Mega Drive / Genesis | `megadrive` | `genesis`, `md` | RetroArch (Genesis Plus GX) |
| Game Gear | `gamegear` | `gg` | RetroArch (Genesis Plus GX) |
| Mega-CD / Sega CD | `segacd` | `megacd` | RetroArch (Genesis Plus GX) |
| Saturn | `saturn` | | Yaba Sanshiro 2 |
| Dreamcast | `dreamcast` | `dc` | Flycast, Redream, RetroArch (Flycast) |
| PlayStation | `psx` | `ps1`, `playstation` | DuckStation, RetroArch (SwanStation), ePSXe |
| PlayStation 2 | `ps2` | `playstation2` | NetherSX2, ARMSX2 |
| PSP | `psp` | | PPSSPP |
| PC Engine / TurboGrafx-16 | `pcengine` | `tg16`, `turbografx16`, `pce` | RetroArch (Beetle PCE Fast) |
| Neo Geo | `neogeo` | | RetroArch (FinalBurn Neo), MAME4droid |
| Arcade | `arcade` | `mame`, `fbneo`, `fba`, `cps1`, `cps2`, `cps3` | RetroArch (FinalBurn Neo), MAME4droid |
| Atari 2600 | `atari2600` | `2600` | RetroArch (Stella) |

By default RetroSalon uses the first installed emulator in each list. You can choose another one per console in **Réglages → Émulateurs**.

### Minimum Android version

RetroSalon itself needs **Android 8.0**. A console therefore needs whichever is higher: Android 8.0, or the emulator's own minimum.

| Emulator | Minimum Android | Notes |
|---|---|---|
| RetroArch | 4.1 (F-Droid build) | Play Store build needs a newer Android. The SwanStation core is 64-bit (arm64) only. |
| PPSSPP | 2.3 (`minSdk 9`, build 1.19.3) | Checked on the installed 1.19.3 F-Droid build. A third-party mirror lists Android 10 for 1.20.x (not verified). |
| Dolphin | 7.0 (API 24) | 64-bit devices only, OpenGL ES 3.0 or Vulkan. |
| melonDS | 7.0 (API 24) | OpenGL ES 3.0. |
| Azahar | **10 (API 29)** | Android 9 users must stay on Azahar 2123.4.1 or older. |
| Eden | **10 (API 29)**, "legacy" APK | Main APK needs Android 13 (API 33). Vulkan 1.1 required. |
| NetherSX2 | 8.0 | 64-bit (arm64) only. |
| DuckStation | not verified | Historically Android 7+, arm64. |
| M64Plus FZ, DraStic, ePSXe, Flycast, Redream, Yaba Sanshiro 2, ARMSX2, MAME4droid | not verified | No reliable figure found. |

When an emulator needs a newer Android than the TV has, *Réglages → Émulateurs* shows **"Demande Android X"** on it.

---

## Where your games can live

RetroSalon reads from two kinds of sources. You can mix them, and add as many as you like.

### 1. A folder on the TV or a USB drive

Games are launched **in place**, with no copy. Choose the folder that contains your console folders:

```
USB drive/
└── Roms/                 ← add this folder as a source
    ├── snes/
    │   └── Super Mario World (USA).sfc
    ├── psx/
    │   ├── Final Fantasy VII (Disc 1).cue
    │   ├── Final Fantasy VII (Disc 1).bin
    │   └── Final Fantasy VII.m3u
    ├── gc/
    │   └── Wind Waker (Europe).rvz
    └── psp/
        └── Patapon (Europe).iso
```

A console folder itself can also be added as a source (for example `Roms/psx`).

### 2. A home server (NAS, PC, Samba): hybrid storage

Add an **SMB share** (Synology, QNAP, a shared folder on a Windows PC, Samba…) with its address, share name, optional sub-folder and an optional user name and password. Leave the user name empty for guest access. The **Tester la connexion** button checks the connection and lists the consoles it finds.

When you start a network game:

1. RetroSalon copies it to the TV, with a progress bar. *Annuler* stops the copy.
2. The game is kept in the **cache** (`/storage/emulated/0/RetroSalon/jeux/<console>/` by default), so the next launch is instant.
3. When the cache goes over its limit (32 GB by default, adjustable from 2 GB to 2,000 GB), the games you played least recently are deleted from the TV. They stay on the server.

You can move the cache to a USB drive, change the limit, or empty it in *Réglages*. Moving the cache only changes where new games are copied; empty it first to free the old location. Emulator save files are kept by the emulators, never in the cache.

File names sent by the server that would point outside the console's cache folder (an absolute path, or `..`) are refused.

**IPv6 first, IPv4 fallback.** RetroSalon tries every address of the server, IPv6 first, waiting up to 3 seconds for each. If no IPv6 address answers, it uses IPv4. You can type a name (`nas.local`), an IPv4 address (`192.168.1.20`), an IPv6 address (`fd00::20` or `[fd00::20]:445`), or an `smb://nas.local` or `\\NAS` address. If you paste a full address such as `smb://nas.local/games`, only the server part is used; put the share name in the *Partage* field.

### What RetroSalon understands in your folders

- **Multi-file discs.** `.cue`, `.gdi` and `.ccd` files are listed, and the track files in the same folder (`.bin`, `.img`, `.sub`, `.raw`) are hidden. When copying from a server, the tracks are copied with them. Keep stand-alone `.img` games in a folder with no `.cue` or `.ccd` file, or they will be hidden too.
- **Multi-disc games.** Put the discs and an `.m3u` playlist with the same title side by side, for example `Final Fantasy VII.m3u` next to `Final Fantasy VII (Disc 1).cue`. Only the playlist is listed, and every disc it names is copied.
- **Clean titles.** `Final Fantasy VII (Europe) (Disc 1) [!]` is shown as *Final Fantasy VII*, with the region and tags in small print.
- Folders named `media`, `images`, `bios` and similar are skipped. Sub-folders are scanned up to 3 levels deep.

---

## First launch and permissions

On first launch, RetroSalon shows a welcome screen that explains what it needs, then asks for **all files access**:

- **Android 11 and newer:** the system screen *All files access* opens. Turn RetroSalon on, then press Back.
- **Android 8 to 10:** Android shows its usual *Allow* dialog.

Without this access, RetroSalon can't read games on the TV or a USB drive, or write the cache. *Plus tard* skips the step; a reminder then stays on the home screen and in *Réglages*.

Some Android TV models don't have the *All files access* screen. In that case, grant it from a computer with `adb` (USB or network debugging enabled on the TV):

```bash
adb shell appops set --uid fr.retrosalon MANAGE_EXTERNAL_STORAGE allow
```

---

## Setting up the emulators

RetroSalon passes the game to each emulator the way that emulator expects. The *Réglages → Émulateurs* screen shows what is left to do for each console.

| How the game is passed | Emulators | What you need to do |
|---|---|---|
| **File path** | RetroArch, ePSXe | **RetroArch:** download the core once: *Main Menu → Online Updater → Core Downloader*, then pick the core named in the table above. |
| **Folder access (Android storage framework)** | PPSSPP, Dolphin, DuckStation, Flycast, Redream, M64Plus FZ, melonDS, DraStic, Azahar, Yaba Sanshiro 2, NetherSX2, ARMSX2 | Give the emulator access to **each console folder** once, in its own settings. Add the console folder of your local source, and the cache folder shown in RetroSalon for server games (for example `RetroSalon/jeux/psp`). In **PPSSPP**, use the *Browse* button when you add the folder. |
| **Shared file** | MAME4droid, Eden | Nothing to do: RetroSalon grants the emulator access to the game when it launches it. |

> **Why folder access?** Since Android 11, an emulator can only open a game it was given access to; a file path isn't enough. In testing, PPSSPP received the game from RetroSalon correctly, but Android refused to let it read the file until the *psp* folder had been allowed in PPSSPP.

---

## Installing

1. Download `RetroSalon-0.1.0.apk` from the release.
2. Install it on the TV. Choose one:
   - over the network with `adb install RetroSalon-0.1.0.apk`;
   - with an app such as *Downloader*;
   - from a USB stick with a file manager.

   Android asks you to allow installs from that source.
3. Open **RetroSalon** from the TV's app row. It has its own TV banner.
4. Install the emulators you need from Google Play, F-Droid or their official pages.

---

## Building from source

| Tool | Version |
|---|---|
| Gradle (wrapper) | 9.8.2 |
| Android Gradle Plugin | 9.5.0 (built-in Kotlin support) |
| Kotlin / Compose compiler plugin | 2.4.21 |
| Android SDK | Platform 37, minor API level 2 (`compileSdk 37.2`) |
| JDK | Gradle runs on JDK 17 or newer (Android Studio's JBR 21 works). The code is compiled with a JDK 17 toolchain, downloaded automatically if it isn't installed. |
| Libraries | Compose BOM 2026.09.00, `androidx.tv:tv-material` 1.1.0, Activity Compose 1.13.0, Lifecycle 2.11.0, Core KTX 1.19.1, Coroutines 1.10.2, smbj 0.15.0 |

Set `sdk.dir` in `local.properties`, or open the project in Android Studio. Then run:

```bash
./gradlew assembleDebug
```

```bash
./gradlew testDebugUnitTest
```

```bash
./gradlew assembleRelease
```

The APKs are written to `app/build/outputs/apk/`. The release build is signed with the local Android debug key so it can be sideloaded. If you publish your own builds, sign them with your own keystore.

**Network tip:** if Gradle fails to download with `Cannot assign requested address` on a network where only IPv6 works, make the JVM prefer IPv6:

```bash
JAVA_TOOL_OPTIONS="-Djava.net.preferIPv6Addresses=true" ./gradlew --no-configuration-cache assembleDebug
```

### Project layout

```
app/src/main/java/fr/retrosalon/
├── MainActivity.kt        screens and navigation, first-launch flow
├── AppModel.kt            library, scanning, launch and cache state
├── StorageAccess.kt       all-files access request
├── Gamepads.kt            connected controller detection
├── data/
│   ├── Catalog.kt         consoles, folder names, emulators and launch intents
│   ├── Scanner.kt         finds games in console folders
│   ├── Storage.kt         local volumes, storage-framework IDs
│   ├── SmbFs.kt           SMB client, IPv6 → IPv4 fallback
│   ├── GameCache.kt       copy from the server, LRU cache
│   ├── Launch.kt          builds and starts the emulator intent
│   └── Models.kt          sources, games, settings
└── ui/                    Compose for TV screens
```

---

## Legal

RetroSalon is a launcher. It does not include or download any game, BIOS, firmware or emulator. Use only games and BIOS files you own and dumped yourself, and follow the laws of your country. Install emulators from their official sources.

Xbox and Xbox Nano are trademarks of Microsoft. Console names are trademarks of their owners. RetroSalon is not affiliated with Microsoft or any console maker.

## Credits

- The launch intents (activity names, extras and how each emulator expects the game) are adapted from the Android configuration of [ES-DE](https://gitlab.com/es-de/emulationstation-de) (MIT licence).
- SMB support uses [smbj](https://github.com/hierynomus/smbj) (Apache 2.0).
- Thanks to the authors of every emulator listed above.

The full copyright notices and licence texts are in [legal/THIRD_PARTY_NOTICES.md](legal/THIRD_PARTY_NOTICES.md). That covers the ES-DE material and every library bundled in the APK, and the same file ships inside the APK.
