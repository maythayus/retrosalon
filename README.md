# RetroSalon

**One launcher for every console on your Android TV, built for the Microsoft Xbox Nano controller.**

> **Branch `retrosalon/retroarch-integre`: RetroSalon with RetroArch built in.** This build contains RetroArch 1.22.2 and 16 libretro cores, so most consoles play without installing anything else. It comes as two APKs, for 32-bit and 64-bit ARM TVs, which you build yourself. The standard RetroSalon, which contains no emulator and runs on every TV, is on the [`main`](https://github.com/maythayus/retrosalon/tree/main) branch and on the [releases page](https://github.com/maythayus/retrosalon/releases).

RetroSalon puts all your retro games in a single library on the TV, from the NES to the Switch. Pick a game with the controller and RetroSalon opens it in the right emulator. You no longer need to open RetroArch, then Dolphin, then PPSSPP, or remember which app runs which console.

- **24 consoles, one screen.** Nintendo, Sega, Sony, NEC, SNK, Atari and arcade games, sorted by console.
- **Made for the Xbox Nano controller.** The whole interface works with the D-pad and the A/B buttons, and RetroSalon shows each connected controller, with its battery level when the controller reports it.
- **Hybrid storage.** Games can be on the TV, on a USB drive, or on a NAS/PC at home. Network games are copied to the TV when you press play, then kept in a cache.
- **RetroArch built in.** The consoles that RetroArch runs (NES, Super Nintendo, Game Boy, Mega Drive, PlayStation, PC Engine, arcade…) start in the RetroArch inside RetroSalon, with its cores: nothing to install or download, unless you install an emulator that comes before it in that console's list (see [Consoles and emulators](#consoles-and-emulators)). For the other consoles, RetroSalon opens the emulators you install yourself. It includes no ROM and no BIOS.

| | |
|---|---|
| Version | 0.2.3-perso2 (RetroArch 1.22.2 built in) |
| Minimum Android | **Android 8.0 (API 26)** |
| Target Android | **Android 17 (API 37)** |
| Devices | Android TV and Google TV with an ARM processor, 32-bit or 64-bit (also works on phones and tablets in landscape). No x86 build. |
| Interface language | English and French, following the system language |
| Licence | Source code: [MIT](LICENSE). Each APK of this build: RetroSalon with RetroArch under GPL-3.0; the cores and some files keep their own licences, some of them non-commercial. See [License](#license). |

**Language.** RetroSalon follows the TV's language: French when the system is set to French, English for any other language. On Android 13 and newer, you can also give RetroSalon its own language, where the system settings offer it: *Apps → RetroSalon → Language*. Console and emulator names stay as they are. The screen names below are the English ones.

---

## The Microsoft Xbox Nano controller

The Xbox Nano (Bluetooth, USB-C) connects to Android like any Xbox Wireless Controller. Android maps it to the standard gamepad buttons, so RetroArch, PPSSPP, Dolphin and most emulators set it up on their own.

**Pairing it with the TV:**

1. On the TV: **Settings → Remotes & Accessories → Pair accessory** (the name varies by brand).
2. Turn the controller on with the Xbox button, then hold the **pair** button until the logo blinks fast.
3. Pick **Xbox Wireless Controller** in the list.

RetroSalon shows each connected controller in the sidebar, with its battery level when the controller reports it (*Controller: Xbox Wireless Controller · 80%*). The badge turns orange at 20% or less, unless Android reports that the controller is charging or plugged in. If no controller is connected, it shows a reminder to pair the Xbox Nano. RetroSalon's *Settings* screen repeats these steps.

**Battery level.** RetroSalon reads it in one of two ways:

- **From Android**, on Android 12 and newer, for controllers whose driver reports a battery, such as the PlayStation DualShock 4 and DualSense. Nothing needs to be granted, and RetroSalon also shows when the controller is charging.
- **From the controller's Bluetooth battery service**, for Bluetooth LE controllers such as the Xbox Nano and Xbox Wireless Controllers with recent firmware, which don't report their battery to Android apps. This gives the level, but not whether the controller is charging, so the badge can turn orange even on the charger. Older Xbox firmware uses classic Bluetooth and shows no level; a firmware update (below) can fix this.
  - On Android 12 and newer, it needs the *Nearby devices* permission: with the controller connected, press *Show the controller battery* in RetroSalon's *Settings*. The button appears while a connected controller shows no level. RetroSalon uses the permission only to read the battery level.
  - On Android 8 to 11, nothing needs to be granted.

RetroSalon refreshes the level every minute or two while it is on screen, and stops while you play. A controller that reports no battery, for example an Xbox controller connected with a USB cable, shows no level.

> The Bluetooth battery reading hasn't been tested with a real Xbox controller yet.

**Keeping the controller up to date.** Firmware updates can fix Bluetooth problems such as lag or disconnections. The usual way is the *Xbox Accessories* app on Windows, or an Xbox console. Some sources say the **Xbox app on Android** can also update it. This isn't the main official route, and it can be slower:

1. Pair the controller with an Android phone or tablet over Bluetooth.
2. Open the *Xbox* app.
3. Go to the connected controller's settings. If an update is available, an option such as *Update firmware* appears; select it.

In the app, the **D-pad** moves between items, **A** opens a game or button, and **B** goes back.

---

## Two players

Pair a second controller the same way: a second Xbox Nano, or any Bluetooth or USB gamepad. Either controller can drive RetroSalon. For the built-in RetroArch, RetroSalon assigns the players before the game starts; with the other emulators, the **emulator** decides which controller is player 1 and which is player 2:

| Emulator (consoles) | Two players |
|---|---|
| Built-in RetroArch (NES, SNES, Mega Drive, Master System, PC Engine, PlayStation with PCSX ReARMed or SwanStation, Neo Geo, arcade, Atari 2600…) | **Automatic.** Before each game, RetroSalon gives player 1 to the first controller connected, player 2 to the second, and so on up to 4 players, so the TV remote never takes a controller's place. See [Playing with the built-in RetroArch](#playing-with-the-built-in-retroarch). |
| Dolphin (GameCube, Wii), DuckStation (PlayStation), NetherSX2 / ARMSX2 (PlayStation 2) | **One-time setup.** In the emulator's controller settings, turn on port 2 and pick the second controller. |
| Flycast and Redream (Dreamcast), M64Plus FZ (Nintendo 64, *Player map*), Yaba Sanshiro 2 (Saturn), ePSXe (PlayStation), Eden (Switch), MAME4droid (Neo Geo, arcade) | Several controllers supported. Assign them in the emulator's controls settings. |
| PPSSPP (PSP), melonDS / DraStic (DS), Azahar (3DS) | No two players on one screen. These handhelds did multiplayer between several consoles. |

Tips:

- Connect both controllers **before** starting the game. Every emulator then sees them both.
- **Xbox 360 controllers** are not Bluetooth. A wired one works over USB; a wireless one needs Microsoft's USB wireless receiver.

> Two-controller play hasn't been tested on real hardware yet. The table describes how each emulator usually handles players.

---

## Playing with the built-in RetroArch

Every console marked *RetroArch* in the [table below](#consoles-and-emulators) can start in the RetroArch built into RetroSalon: it is used when no emulator listed before it is installed, or when you choose it in **Settings → Choose emulators**. You don't need the RetroArch app, and there are no cores to download. These 16 cores are built in: Mesen and FCEUmm (NES), Snes9x (Super Nintendo), Mupen64Plus-Next (Nintendo 64), Gambatte and mGBA (Game Boy, Game Boy Color, Game Boy Advance), melonDS DS (Nintendo DS), Genesis Plus GX (Master System, Mega Drive, Game Gear, Mega-CD), YabaSanshiro (Saturn), Flycast (Dreamcast), PCSX ReARMed and SwanStation (PlayStation), PPSSPP (PSP), Beetle PCE Fast (PC Engine), FinalBurn Neo (Neo Geo, arcade) and Stella (Atari 2600).

**During a game:**

- **Back** on the remote, or **Menu + View** (Start + Select) on the controller, opens RetroArch's menu. **A** confirms and **B** goes back. Every connected controller can use the menu, not only player 1's.
- In the menu, *Quick Menu → Save State* and *Load State* save and restore the game at any point. *Close Content* quits RetroArch and returns to RetroSalon.
- **Home** on the remote also closes RetroArch, so the next game always starts in a fresh RetroArch. RetroArch writes the game's own saves every 10 seconds (*SaveRAM Autosave Interval*), so up to 10 seconds of progress can be lost: save in the game and wait a moment, or use *Close Content*, before pressing Home.
- In games that don't use the analog stick (NES, Mega Drive…), the **left stick** also works as the D-pad. Games that use the stick (Nintendo 64, PSP…) keep it as a stick.

**Players.** Before each game, RetroSalon assigns the connected controllers to players 1 to 4, in the order they were connected, controllers with analog sticks first (a TV remote has none), with RetroArch's *Device Reservation Type* set to *Preferred*. Without this, RetroArch gives player 1 to the first device that sends a button, which can be the TV remote. If you choose another controller for a player in RetroArch (*Settings → Input → Port 1 Controls → Device Reservation Type* and *Reserved Device for This Player*) and RetroArch saves it (see below), RetroSalon leaves that player alone from then on.

**Freebox Player POP and other TVs with "Play" in their model name.** RetroArch 1.22.2 reads the device's model name, and when it contains "Play" it assumes a Sony XPERIA Play phone: the controllers' A button does nothing, and every controller drives player 1. The built-in RetroArch sees the model name with "play" in lower case (for example *Freebox player POP*), so controllers work normally. The installed RetroArch app doesn't have this fix.

**RetroArch's settings.** RetroSalon sets a few TV defaults once: the Ozone menu, *Close Content* returning to RetroSalon, the menu buttons above, the left stick as D-pad and the menu for every controller. What you change in RetroArch afterwards is kept, even after a RetroSalon update, as long as RetroArch saves it. RetroArch saves its settings when it quits from its own menu (*Close Content* during a game, or *Main Menu → Quit*), or with *Main Menu → Configuration File → Save Current Configuration*. **Home closes RetroArch without saving them.** The *Online Updater → Core Downloader* entry is hidden: the cores come with the APK, and RetroSalon rebuilds RetroArch's core folder before each game, so a downloaded core would not be kept. *Settings → Emulators* shows *RetroArch 1.22.2 built in · 16 cores*, and its **Open RetroArch** button opens RetroArch's menu without a game (settings, controllers…). The built-in RetroArch keeps its own settings, separate from an installed RetroArch app's.

**BIOS files.** Put the BIOS files of your own consoles in `/storage/emulated/0/RetroArch/system`, the same folder as the RetroArch app's. The libretro documentation lists the files each core needs and their names. The PlayStation doesn't need one with PCSX ReARMed, which has its own replacement BIOS, though some games run better with a real one. Before the first game, RetroSalon also copies PPSSPP's system files (the PSP fonts, translations and shaders) into `RetroArch/system/PPSSPP`, once RetroSalon has *All files access*.

**An installed RetroArch app.** RetroSalon always uses the built-in RetroArch for the consoles above, even when the RetroArch app is installed.

> Tested on a Freebox Player POP (32-bit) with an Xbox Nano controller, and on an Android emulator. Not every core and console has been tried.

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
| Nintendo DS | `nds` | `ds` | melonDS, DraStic, RetroArch (melonDS DS) |
| Nintendo 3DS | `n3ds` | `3ds` | Azahar |
| GameCube | `gc` | `gamecube`, `ngc` | Dolphin |
| Wii | `wii` | | Dolphin |
| Nintendo Switch | `switch` | `nsw` | Eden |
| Master System | `mastersystem` | `sms` | RetroArch (Genesis Plus GX) |
| Mega Drive / Genesis | `megadrive` | `genesis`, `md` | RetroArch (Genesis Plus GX) |
| Game Gear | `gamegear` | `gg` | RetroArch (Genesis Plus GX) |
| Mega-CD / Sega CD | `segacd` | `megacd` | RetroArch (Genesis Plus GX) |
| Saturn | `saturn` | | Yaba Sanshiro 2, RetroArch (YabaSanshiro) |
| Dreamcast | `dreamcast` | `dc` | Flycast, Redream, RetroArch (Flycast) |
| PlayStation | `psx` | `ps1`, `playstation` | DuckStation, RetroArch (PCSX ReARMed), RetroArch (SwanStation), ePSXe |
| PlayStation 2 | `ps2` | `playstation2` | NetherSX2, ARMSX2 |
| PSP | `psp` | | PPSSPP, RetroArch (PPSSPP) |
| PC Engine / TurboGrafx-16 | `pcengine` | `tg16`, `turbografx16`, `pce` | RetroArch (Beetle PCE Fast) |
| Neo Geo | `neogeo` | | RetroArch (FinalBurn Neo), MAME4droid |
| Arcade | `arcade` | `mame`, `fbneo`, `fba`, `cps1`, `cps2`, `cps3` | RetroArch (FinalBurn Neo), MAME4droid |
| Atari 2600 | `atari2600` | `2600` | RetroArch (Stella) |

Every RetroArch core in this table is built into RetroSalon. By default RetroSalon uses the first installed emulator in each list, and the built-in RetroArch always counts as installed: for the Nintendo 64, for example, RetroSalon uses M64Plus FZ if it is installed, and the built-in RetroArch otherwise. You can choose another one per console in **Settings → Choose emulators**.

### Minimum Android version

RetroSalon itself needs **Android 8.0**. A console therefore needs whichever is higher: Android 8.0, or the emulator's own minimum.

| Emulator | Minimum Android | Notes |
|---|---|---|
| RetroArch | 8.0, built in | Same as RetroSalon. The built-in RetroArch is 1.22.2. |
| PPSSPP | 2.3 (`minSdk 9`, build 1.19.3) | Checked on the installed 1.19.3 F-Droid build. A third-party mirror lists Android 10 for 1.20.x (not verified). |
| Dolphin | 7.0 (API 24) | 64-bit devices only, OpenGL ES 3.0 or Vulkan. |
| melonDS | 7.0 (API 24) | OpenGL ES 3.0. |
| Azahar | **10 (API 29)** | Android 9 users must stay on Azahar 2123.4.1 or older. |
| Eden | **10 (API 29)**, "legacy" APK | Main APK needs Android 13 (API 33). Vulkan 1.1 required. |
| NetherSX2 | 8.0 | 64-bit (arm64) only. |
| DuckStation | not verified | Play Store build supports 32-bit (armv7) and 64-bit ARM. |
| M64Plus FZ, DraStic, ePSXe, Flycast, Redream, Yaba Sanshiro 2, ARMSX2, MAME4droid | not verified | No reliable figure found. |

When an emulator needs a newer Android than the TV has, *Settings → Choose emulators* shows **"Requires Android X"** on it.

### 32-bit and 64-bit TVs

This build comes as **two APKs**: one for 32-bit ARM Android and one for 64-bit ARM Android, each with the RetroArch engine and cores for that Android (see [Installing](#installing)). There is no x86 or x86_64 APK: on an x86 TV, use the standard RetroSalon from the [releases page](https://github.com/maythayus/retrosalon/releases), which runs on every TV. Some TV boxes run a 32-bit Android even with a 64-bit processor, for example the Freebox Player POP: they need the 32-bit APK.

Some emulators are **64-bit only** and won't install on a 32-bit TV: Dolphin, Azahar, NetherSX2, ARMSX2, Eden and the current MAME4droid (2024 version). On a 32-bit TV, use the other emulators in each console's list: for example the built-in RetroArch (with PCSX ReARMed or SwanStation for the PlayStation), DuckStation or ePSXe, and the older MAME4droid 0.139u1 for arcade games, which needs games in the MAME 0.139 format.

RetroSalon tells you which one your TV runs on first launch and in *Settings → Emulators*. From a computer, you can also run `adb shell getprop ro.product.cpu.abilist`: if the list contains `arm64-v8a` or `x86_64`, the TV runs a 64-bit Android.

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

Add an **SMB share** (Synology, QNAP, a shared folder on a Windows PC, Samba…) with its address, share name, optional sub-folder and an optional user name and password. Leave the user name empty for guest access. The **Test connection** button checks the connection and lists the consoles it finds.

When you start a network game:

1. RetroSalon copies it to the TV, with a progress bar. *Cancel* stops the copy. Files that finished copying are kept for the next try, and a stopped copy never mixes two versions of a game.
2. The game is kept in the **cache** (`/storage/emulated/0/RetroSalon/jeux/<console>/` by default). The next launch is instant and doesn't contact the server, so a game already in the cache plays even when the NAS or PC is off. When RetroSalon can't reach the server, the server's games stay in the list.
3. When the cache goes over its limit (32 GB by default, adjustable from 2 GB to 2,000 GB), the games you played least recently are deleted from the TV. They stay on the server.

You can move the cache to a USB drive, change the limit, or empty it in RetroSalon's *Settings*. Moving the cache only changes where new games are copied; empty it first to free the old location. Emulator save files are kept by the emulators, never in the cache.

A cached game is checked against the server's game list by the size of its main file. If you replace the tracks or discs of a game on the server but keep its `.cue`, `.gdi`, `.ccd` or `.m3u` file as it is, use *Empty cache* so the new files are copied.

File names sent by the server that would point outside the console's cache folder (an absolute path, or `..`) are refused.

**IPv6 first, IPv4 fallback.** RetroSalon tries every address of the server, IPv6 first, waiting up to 3 seconds for each. If no IPv6 address answers, it uses IPv4. You can type a name (`nas.local`), an IPv4 address (`192.168.1.20`), an IPv6 address (`fd00::20` or `[fd00::20]:445`), or an `smb://nas.local` or `\\NAS` address. If you paste a full address such as `smb://nas.local/games`, only the server part is used; put the share name in the *Share* field.

#### Games on a disk plugged into a Freebox (Freebox Pop and others)

The Freebox can share its disk on your home network. Its settings are in Freebox OS: open `http://mafreebox.freebox.fr` in a browser on your home network, click **Paramètres de la Freebox** (Freebox settings), then the **Mode avancé** (advanced mode) tab. The labels below are the French ones shown by Freebox OS.

1. **Share the disk.** Under *Partage de fichiers* (file sharing), click **Partages Windows** (Windows shares). Tick **Activer le partage de fichiers** (enable file sharing) and the SMBv2/SMBv3 option. Tick **Accès authentifié** (authenticated access), choose a user name and a password, then click **OK**. Users report sign-in problems with the user name `freebox`, so choose another one. The same user name and password give access to every disk plugged into the Freebox.
2. **Turn off disk standby.** By default the Freebox puts its disk to sleep after 10 minutes without use. Waking it up can take longer than RetroSalon waits, so the first game launched after a pause can fail with a *timed out* error. Under *Partage de fichiers*, click **Disques** (disks), then **Paramètres** (settings) at the bottom left of the window. Under *Gestion de l'alimentation SATA externe* (external SATA power management), untick **Activer la mise en veille des périphériques SATA** (enable standby for SATA devices), then click **OK**.

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

Without this access, RetroSalon can't read games on the TV or a USB drive, or write the cache. *Later* skips the step; a reminder then stays on the home screen and in *Settings*.

RetroSalon then shows **Your TV** once: whether the TV runs a 32-bit or 64-bit Android, on an ARM or x86 processor, and which emulators can't be installed on it (see [32-bit and 64-bit TVs](#32-bit-and-64-bit-tvs)). It also appears once after updating from RetroSalon 0.2.2 or older. The same information stays in *Settings → Emulators*.

**Nearby devices** (optional, Android 12 and newer) is asked only when you press *Show the controller battery* in *Settings*; the button appears while a connected controller shows no battery level. RetroSalon uses it to read the battery level of Bluetooth controllers such as the Xbox Nano. If you refuse, everything else works the same, without the level. The button then becomes *Open in the TV settings*, where you can allow it later: *Apps → RetroSalon → Permissions*.

Some Android TV models don't have the *All files access* screen. In that case, grant it from a computer with `adb` (USB or network debugging enabled on the TV):

```bash
adb shell appops set --uid fr.retrosalon MANAGE_EXTERNAL_STORAGE allow
```

---

## Setting up the emulators

RetroSalon passes the game to each emulator the way that emulator expects. The *Settings → Choose emulators* screen shows what is left to do for each console.

| How the game is passed | Emulators | What you need to do |
|---|---|---|
| **File path** | Built-in RetroArch, ePSXe | Nothing to do. The RetroArch cores are built into RetroSalon: see [Playing with the built-in RetroArch](#playing-with-the-built-in-retroarch). |
| **Folder access (Android storage framework)** | PPSSPP, Dolphin, DuckStation, Flycast, Redream, M64Plus FZ, melonDS, DraStic, Azahar, Yaba Sanshiro 2, NetherSX2, ARMSX2 | Give the emulator access to **each console folder** once, in its own settings. Add the console folder of your local source, and the cache folder shown in RetroSalon for server games (for example `RetroSalon/jeux/psp`). In **PPSSPP**, use the *Browse* button when you add the folder. |
| **Shared file** | MAME4droid, Eden | Nothing to do: RetroSalon grants the emulator access to the game when it launches it. |

> **Why folder access?** Since Android 11, an emulator can only open a game it was given access to; a file path isn't enough. In testing, PPSSPP received the game from RetroSalon correctly, but Android refused to let it read the file until the *psp* folder had been allowed in PPSSPP.

---

## Installing

The APKs of this build are not published: build them yourself (see [Building from source](#building-from-source)). The releases page only has the standard RetroSalon, with no emulator inside.

1. Pick the APK that matches the TV's Android, from `app/build/outputs/apk/release/`. Each is about 80 MB.
   - `app-armeabi-v7a-release.apk` for a **32-bit** Android, such as the Freebox Player POP;
   - `app-arm64-v8a-release.apk` for a **64-bit** Android.

   To find out which one the TV runs, run `adb shell getprop ro.product.cpu.abilist`: if the list contains `arm64-v8a`, take the 64-bit APK; if it only has `armeabi-v7a` and `armeabi`, take the 32-bit one. If it only has `x86` or `x86_64`, this build can't run on the TV.
2. Install it on the TV. Choose one:
   - over the network with `adb install app-armeabi-v7a-release.apk` (or the 64-bit file);
   - with an app such as *Downloader*;
   - from a USB stick with a file manager.

   Android asks you to allow installs from that source.
3. Open **RetroSalon** from the TV's app row. It has its own TV banner.
4. Install the other emulators you need from Google Play, F-Droid or their official pages.

**Updating from RetroSalon 0.2.3 or older.** Android only installs this build over an existing RetroSalon, keeping your sources, settings and cached games, when both APKs are signed with the same key. The APKs on the releases page are signed with the maintainer's key, so a build you make yourself can't replace them: write down your sources and settings, then uninstall RetroSalon first, as they are lost. Games already in the cache stay on the TV. A build made on the same computer as the RetroSalon already installed is signed with the same local Android debug key, and installs over it.

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
| Python | 3.8 or newer, to download RetroArch (`retroarch/fetch.py`) |
| RetroArch | 1.22.2, downloaded by `retroarch/fetch.py` |

**First, download RetroArch and its cores.** Nothing from RetroArch is stored in this repository. `retroarch/fetch.py` downloads, from libretro's official sources only:

- RetroArch's Android Java code, from the `v1.22.2` tag on GitHub;
- the RetroArch engine and its files (menus, fonts, controller profiles, core information), from the official 1.22.2 APKs on buildbot.libretro.com/stable;
- the 16 cores, for 32-bit and 64-bit ARM, from buildbot.libretro.com/nightly (the same files as RetroArch's *Core Downloader*);
- PPSSPP's system files, from buildbot.libretro.com/assets/system;
- the licence texts of the Snes9x, Genesis Plus GX and FinalBurn Neo cores, from their repositories on GitHub.

That's about 450 MB, kept in `retroarch/vendor/cache` for the next run. `retroarch/vendor/` is ignored by git. `retroarch/vendor/VERSIONS.txt`, also shipped in the APK, lists every downloaded file with its SHA-256 and, for each core, its build date. The cores are libretro's latest nightly builds at the time of the first download. Running `fetch.py` again reuses the files in `retroarch/vendor/cache` and gives the same cores; to get newer ones, delete `retroarch/vendor/cache` first. Two fresh downloads on different days can give different cores.

```bash
python -I retroarch/fetch.py
```

The Gradle build stops with *RetroArch n'est pas encore récupéré* (RetroArch hasn't been downloaded yet) until this has run.

Then set `sdk.dir` in `local.properties`, or open the project in Android Studio, and run:

```bash
./gradlew assembleDebug
```

```bash
./gradlew testDebugUnitTest
```

```bash
./gradlew assembleRelease
```

The APKs are written to `app/build/outputs/apk/`, one per ABI: `release/app-armeabi-v7a-release.apk` (32-bit ARM) and `release/app-arm64-v8a-release.apk` (64-bit ARM), and the same names with `debug` for the debug build. The release build is signed with the local Android debug key so it can be sideloaded. If you share your own builds, sign them with your own keystore, and read [License](#license) first: an APK of this build comes with obligations.

**Network tip:** if Gradle fails to download with `Cannot assign requested address` on a network where only IPv6 works, make the JVM prefer IPv6:

```bash
JAVA_TOOL_OPTIONS="-Djava.net.preferIPv6Addresses=true" ./gradlew --no-configuration-cache assembleDebug
```

### Project layout

```
app/src/main/java/fr/retrosalon/
├── MainActivity.kt        screens and navigation, first-launch flow
├── RetroSalonApp.kt       sets up the built-in RetroArch's process (":retroarch")
├── AppModel.kt            library, scanning, launch and cache state
├── StorageAccess.kt       all-files access request
├── Gamepads.kt            controllers and battery rules
├── PadMonitor.kt          follows connected controllers and reads their battery
├── data/
│   ├── Catalog.kt         consoles, folder names, emulators and launch intents
│   ├── Scanner.kt         finds games in console folders
│   ├── Storage.kt         local volumes, storage-framework IDs
│   ├── SmbFs.kt           SMB client, IPv6 → IPv4 fallback
│   ├── GameCache.kt       copy from the server, LRU cache
│   ├── Launch.kt          builds and starts the emulator intent
│   ├── EmbeddedRetroArch.kt  built-in RetroArch: files, cores, settings, players
│   └── Models.kt          sources, games, settings
└── ui/                    Compose for TV screens
app/src/main/jniLibs/<abi>/libretrosalon-getprop.so
                           shell script: the device model name the built-in RetroArch sees
retroarch/
├── fetch.py               downloads RetroArch 1.22.2 and the cores into vendor/
├── build.gradle.kts       Gradle module that builds RetroArch's Java code and packs vendor/
├── src/main/AndroidManifest.xml  RetroArch's activities, in their own ":retroarch" process
└── vendor/                downloaded files (ignored by git)
legal/
├── THIRD_PARTY_NOTICES.md third-party notices, shipped in the APK
└── licenses/              GPL-3.0, GPL-2.0, MPL-2.0 and OFL-1.1 texts, shipped in the APK
```

---

## Legal

This build of RetroSalon contains emulators: RetroArch and 16 libretro cores, which `retroarch/fetch.py` downloads from libretro's official sources (buildbot.libretro.com and libretro's repositories on GitHub) before the APK is built. It includes no game and no BIOS, and downloads none. The only console system files it contains are the PSP system fonts that the PPSSPP project redistributes with its system files. Use only games and BIOS files you own and dumped yourself, and follow the laws of your country. Install the other emulators from their official sources.

**Non-commercial components.** The Snes9x, Genesis Plus GX and FinalBurn Neo cores, and RetroArch's menu sounds, may only be redistributed for non-commercial purposes. An APK of this build must be given away free: never sold, bundled with a paid product or device, offered behind a paywall or in exchange for a donation, or offered from a page or app with advertising.

RetroArch and libretro are trademarks of the libretro team. Xbox and Xbox Nano are trademarks of Microsoft. Console names are trademarks of their owners. RetroSalon is not affiliated with or endorsed by the libretro team, the authors of any emulator or core, Microsoft, or any console maker.

## Credits

- [RetroArch](https://www.retroarch.com) 1.22.2, by the libretro team, is built in with its Android code unchanged. The 16 cores are by their own authors, listed in [legal/THIRD_PARTY_NOTICES.md](legal/THIRD_PARTY_NOTICES.md#retroarch-and-libretro-cores-built-in-retroarch-build).
- The launch intents (activity names, extras and how each emulator expects the game) are adapted from the Android configuration of [ES-DE](https://gitlab.com/es-de/emulationstation-de) (MIT licence).
- SMB support uses [smbj](https://github.com/hierynomus/smbj) (Apache 2.0).
- Thanks to the authors of every emulator listed above.

## License

RetroSalon's own source code is released under the [MIT License](LICENSE). Third-party components keep their own licences.

Each APK of this build also contains RetroArch's Android code, compiled together with RetroSalon, so RetroSalon and RetroArch together are distributed under the [GNU General Public License version 3](legal/licenses/GPL-3.0.txt). The cores are separate programs, not covered by it, under their own licences: GPL-2.0, GPL-3.0, MPL-2.0 (mGBA), and the non-commercial licences of Snes9x, Genesis Plus GX and FinalBurn Neo. Some files keep their own licences too: the menu sounds (CC BY-NC 3.0) and fonts (OFL-1.1 and others).

This project does not publish APKs of this build. If you give one to someone else, you must also offer them the complete source code of RetroArch and of every GPL or MPL core in it, at the revisions the APK was built from, and keep the non-commercial conditions above. Share only APKs built from a committed tree, so the source of RetroSalon itself can be matched too. [legal/THIRD_PARTY_NOTICES.md](legal/THIRD_PARTY_NOTICES.md#source-code) says where each one is.

The copyright notices are in [legal/THIRD_PARTY_NOTICES.md](legal/THIRD_PARTY_NOTICES.md), and the GPL-3.0, GPL-2.0, MPL-2.0 and OFL-1.1 texts in [legal/licenses/](legal/licenses). That covers the ES-DE material, every library bundled in the APK, RetroArch and the 16 cores. The licence texts of Snes9x, Genesis Plus GX and FinalBurn Neo are downloaded by `retroarch/fetch.py` from their repositories and are not stored here. All of them ship inside the APK (`assets/THIRD_PARTY_NOTICES.md`, `assets/licenses/`, `assets/retroarch/licenses/`).
