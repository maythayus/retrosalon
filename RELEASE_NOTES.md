# Release notes

## RetroSalon 0.3.0 (2026-11-13)

**RetroArch is now built into RetroSalon.** RetroArch 1.22.2 and 16 libretro cores are inside the APK, so the consoles that RetroArch runs play without installing anything else.

- **Two APKs, one per processor.** `RetroSalon-0.3.0-32bit.apk` is for 32-bit ARM Android, such as the Freebox Player POP, and `RetroSalon-0.3.0-64bit.apk` for 64-bit ARM Android. Each has the RetroArch engine and cores for that Android. There is no x86 APK: on an x86 TV, stay on RetroSalon 0.2.3.
- **The consoles that use RetroArch start in the built-in RetroArch**, unless you install an emulator that comes before it in that console's list. No RetroArch app and no core downloads are needed. The 16 cores are Mesen, FCEUmm, Snes9x, Mupen64Plus-Next, Gambatte, mGBA, melonDS DS, Genesis Plus GX, YabaSanshiro, Flycast, PCSX ReARMed, SwanStation, PPSSPP, Beetle PCE Fast, FinalBurn Neo and Stella. melonDS DS (Nintendo DS), YabaSanshiro (Saturn), PCSX ReARMed (PlayStation) and the PPSSPP core (PSP) are new in the emulator lists.
- **Controllers.** Before each game, RetroSalon assigns the connected controllers to players 1 to 4 in the order they were connected, so the TV remote no longer takes player 1. The left stick works as the D-pad in games that don't use it, every controller can use RetroArch's menu, and Back or Start + Select opens it.
- **Freebox Player POP.** The built-in RetroArch no longer mistakes the TV for a Sony XPERIA Play, where the controllers' A button did nothing.
- **Home closes RetroArch,** so the next game always starts in a fresh RetroArch.
- **Settings → Emulators** shows the built-in RetroArch, with an **Open RetroArch** button, the BIOS folder and the legal notices.
- **Network games.** When the server stops answering in time (a disk going to sleep, weak Wi-Fi) or the connection drops, RetroSalon tries the copy up to 3 times and resumes where it stopped, unless the file changed on the server in between. It shows *Connection problem with …, trying again (2 of 3)…* meanwhile. A password refused after a drop is not retried. A drop while scanning a source keeps that source's games from the previous scan. The README explains how to share a Freebox's disk and turn off its sleep.

**Licences and source code.** RetroSalon's own code stays under the MIT licence. Each APK, RetroSalon with RetroArch, is distributed under the GPL-3.0, and the cores keep their own licences. Snes9x, Genesis Plus GX and FinalBurn Neo may only be redistributed for non-commercial purposes, so the APKs must be given away free. The complete source code of RetroArch and of each GPL or MPL core is attached to this release. All licence texts are inside the APK and in [legal/](legal).

**Requirements:** Android 8.0 (API 26) or newer, unchanged, and an ARM processor.

**Updating from 0.2.3, 0.2.2, 0.2.1, 0.2.0 or 0.1.0:** install the APK for your TV over it, no need to uninstall. It is signed with the same key, so your sources, settings and cached games are kept.

### Tested

- Unit tests (80) and Android lint (0 errors) pass.
- Both APKs have the same signing certificate as the 0.2.3 APK (checked with apksigner), and each contains the licence texts and `VERSIONS.txt`.
- On a real Freebox Player POP (32-bit Android) with an Xbox Nano controller: games start in the built-in RetroArch, the A button works, and games copy from the Freebox's own disk once its sleep is turned off.
- Also checked on an Android emulator. Not every core and console has been tried, and the 64-bit APK has not been tried on a real TV.

### Known issues

- Settings changed in RetroArch's menu are only kept if RetroArch saves them: quit with *Close Content* or *Main Menu → Quit*, not with Home.
- On the Freebox Pop, the remote's voice search can appear as a second controller named *virtual-search*. It has no effect on games.

## RetroSalon 0.2.3 (2026-11-12)

**RetroSalon now tells you whether your TV runs a 32-bit or 64-bit Android.**

Some TV boxes run a 32-bit Android even with a 64-bit processor, for example the Freebox Pop player, and several emulators only exist in 64-bit.

- On first launch, after the file access step, a new **Your TV** screen shows the Android version, whether Android runs in 32-bit or 64-bit, and whether the processor is ARM or x86. It is also shown once after updating from an older version.
- It says what that means for the emulators:
  - **32-bit:** Dolphin, Azahar, NetherSX2, ARMSX2, Eden and the 2024 version of MAME4droid can't be installed. The screen suggests what to use instead: the "32bit" download of RetroArch from retroarch.com; RetroArch's SwanStation core, DuckStation or ePSXe for the PlayStation; and MAME4droid 0.139u1, which needs games in the MAME 0.139 format.
  - **64-bit:** every emulator RetroSalon supports can be installed, as long as the TV's Android version and power are enough. On an x86_64 TV that can't run 64-bit ARM apps, the screen lists the emulators that only exist for ARM.
- *Got it* or Back closes the screen. The same information stays in *Settings → Emulators*.

**One APK for every TV.** As before, the APK runs on 32-bit and 64-bit Android, ARM or x86. The README now says so, in a new *32-bit and 64-bit TVs* section.

**Requirements:** unchanged, Android 8.0 (API 26) or newer.

**Updating from 0.2.2, 0.2.1, 0.2.0 or 0.1.0:** install the new APK over it, no need to uninstall. It is signed with the same key, so your sources, settings and cached games are kept.

### Tested

- Unit tests (62, including 5 new tests for the 32-bit/64-bit detection, one with the Freebox Pop's values) and Android lint (0 errors) pass.
- The APK has the same signing certificate as the 0.2.2 APK (checked with apksigner).
- Checked on an Android 15 emulator (Pixel 9a profile, 64-bit x86 with ARM translation), in English and French:
  - installed over 0.2.2, the *Your TV* screen appears once, shows "Android 15 · 64-bit · x86 processor (Intel or AMD)", and doesn't come back on the next launch;
  - on a first launch: the welcome screen, then *Later*, then *Your TV*, then the home screen. *Got it* has the remote's focus and Back closes the screen;
  - *Settings → Emulators* shows the same information.
- The 32-bit text was checked with unit tests only: no 32-bit device or emulator was available.
- Since 0.2.2: on a real Freebox Pop, the Xbox Nano's battery level shows correctly through its Bluetooth battery service.

### Known issue

- On the Freebox Pop, the remote's voice search can appear as a second controller named *virtual-search*. It has no effect on games.

## RetroSalon 0.2.2 (2026-11-09)

**Controllers now show their battery level.**

- The sidebar and *Settings* show each connected controller with its battery level when the controller reports it, for example *Controller: Xbox Wireless Controller · 80%*. The badge turns orange at 20% or less, unless Android reports that the controller is charging or plugged in.
- RetroSalon reads the level in one of two ways:
  - **From Android**, on Android 12 and newer, for controllers whose driver reports a battery, such as the PlayStation DualShock 4 and DualSense. Nothing needs to be granted, and the badge also shows *charging* or *plugged in*.
  - **From the controller's Bluetooth battery service**, for Bluetooth LE controllers such as the Xbox Nano and Xbox Wireless Controllers with recent firmware, which don't report their battery to Android apps. This gives the level, but not whether the controller is charging, so the badge can turn orange even on the charger.
- A controller shows its level a few seconds after it connects. RetroSalon refreshes the level every minute or two while it is on screen, and stops while you play: it doesn't use Bluetooth during games.

**New optional permission: *Nearby devices*** (Android 12 and newer). The Bluetooth reading needs it. RetroSalon asks for it only when you press *Show the controller battery* in *Settings*; the button appears while a connected controller shows no level. RetroSalon uses it only to read the battery level. If you refuse, everything else works as before, and the button becomes *Open in the TV settings*, where you can allow it later (*Apps → RetroSalon → Permissions*). On Android 8 to 11, nothing needs to be granted.

**Requirements:** unchanged, Android 8.0 (API 26) or newer.

**Updating from 0.2.1, 0.2.0 or 0.1.0:** install the new APK over it, no need to uninstall. It is signed with the same key, so your sources, settings and cached games are kept.

### Tested

- Unit tests (57, including 18 new tests for the battery rules) and Android lint (0 errors) pass.
- The APK has the same signing certificate as the 0.2.1 APK (checked with apksigner).
- Checked on an Android 15 emulator (Pixel 9a profile) with simulated controllers: a DualShock 4 created with Android's `hid` test tool, and a controller without a battery.
  - The DualShock 4 shows 45%, 15% in orange, and "45% · charging"; in French, "45 % · en charge".
  - A controller connected while RetroSalon was in the background shows no level when you come back, then its level about 5 seconds later.
  - The *Show the controller battery* button appears for the controller without a battery, but not during the first seconds after a DualShock 4 connects. The system permission dialog opens. After a refusal, the button opens the TV settings and keeps the remote's focus. The refusal is remembered when the language or text size changes.
  - This APK installs over the previous build and shows the DualShock 4 at 15% in orange.
- Not checked yet: the Bluetooth battery service with a real Xbox controller (the emulator can't pair a Bluetooth controller), a real Android TV, and Android 8 to 11.

## RetroSalon 0.2.1 (2026-11-06)

**Network games already in the cache now play when the server is off.**

In 0.2.0, RetroSalon contacted the server before every launch of a network game, even when the game was already copied to the TV. With the NAS or PC off, the game didn't start.

- A game already in the cache now starts straight away, without contacting the server.
- When the server can't be reached, RetroSalon plays the copy in the cache, even if the game has changed on the server since it was copied. A game that isn't in the cache still shows the connection error, as in 0.2.0.
- Copies from the server are safer:
  - Files are copied under a temporary name and put in place together once everything is copied. A cancelled or interrupted copy never mixes two versions of a game, and leaves the copy already in the cache as it was. That copy still plays when the server is off.
  - Files that finished copying are kept, so the next launch copies only what is missing.
  - If a file's size changes on the server during the copy, it isn't kept, and RetroSalon asks you to try again.
  - When the game's main file was removed from the server, RetroSalon says "File not found".
  - Only one copy runs at a time. Emptying the cache or changing its limit waits for the copy in progress.
- Other fixes:
  - `.cue` and `.m3u` files saved with a byte order mark (BOM), as some Windows editors do, are read correctly.
  - A CloneCD game (`.ccd`) without its optional `.sub` file plays from the cache when the server is off, after RetroSalon has tried to reach the server.
  - If you cancel a launch and start another game right away, the cancelled launch can no longer change the new launch screen.

**Known limit:** a cached game is checked against the server's game list by the size of its main file. If you replace a game's tracks or discs on the server but keep its `.cue`, `.gdi`, `.ccd` or `.m3u` file as it is, use *Empty cache* in *Settings* so the new files are copied.

**Requirements:** unchanged, Android 8.0 (API 26) or newer.

**Updating from 0.2.0 or 0.1.0:** install the new APK over it, no need to uninstall. It is signed with the same key, so your sources, settings and cached games are kept.

### Tested

- Unit tests (39, including 24 new tests for the cache) and Android lint (0 errors) pass.
- The APK has the same signing certificate as the 0.2.0 APK (checked with apksigner).
- Checked with this APK on an Android 15 emulator (Pixel 9a profile), with an SMB source whose server is off:
  - a PSP game already in the cache starts in PPSSPP (0.2.0 fails this test);
  - a game that isn't in the cache shows "Couldn't prepare the game: Server unreachable…";
  - after that error, the game in the cache still starts.
- Checked with a test build on the same emulator: when the game list gives a different size for a cached game, the cached copy still starts with the server off.
- Not checked yet: a copy from a real SMB server (NAS or Windows PC), a real Android TV, and Android 8 to 10.

## RetroSalon 0.2.0 (2026-11-05)

**The interface now speaks English and French.**

- RetroSalon follows the system language: French when the TV is set to French, English for any other language.
- On Android 13 and newer, RetroSalon can have its own language, where the system settings offer it (*Apps → RetroSalon → Language*). Android lists only English and French there.
- Everything the app shows is translated: screens, buttons, error messages, the launch overlay and counts ("1 game", "2 games"). File sizes use the system's units.
- Only English and French are kept in the APK, including the texts of the bundled libraries, so a screen is never half in another language.
- Console and emulator names are not translated.
- The README now uses the English screen names: *Settings*, *Settings → Choose emulators*, *Test connection*.

**Requirements:** unchanged, Android 8.0 (API 26) or newer.

**Updating from 0.1.0:** install the new APK over it, no need to uninstall. It is signed with the same key, so your sources and settings are kept.

### Tested

- Unit tests (15) and Android lint (0 errors) pass.
- The APK has the same signing certificate as the 0.1.0 APK (checked with apksigner).
- Checked on an Android 15 emulator (Pixel 9a profile, system in English):
  - welcome, home and *Settings* screens in English;
  - the same screens in French after switching RetroSalon's language to French;
  - back to *System default*, the app returns to English;
  - Android's app language screen offers *System default*, English and Français.
- Not checked yet: a TV whose whole system is set to French, and the French texts on a real Android TV.

## RetroSalon 0.1.0 (2026-11-05)

First release: one launcher for every console on Android TV, designed for the Microsoft Xbox Nano controller.

**Requirements:** Android 8.0 (API 26) or newer. Built for Android 17 (API 37). Some consoles need a newer Android because of their emulator; see the README.

### Highlights

- **Every console in one library.** 24 consoles are recognized from their folder names, from the NES to the Nintendo Switch: Nintendo, Sega, Sony, NEC, SNK, Atari and arcade. Many alternative folder names are accepted, such as `ps1`, `genesis`, `gamecube` or `Mega Drive`.
- **One button to play.** RetroSalon opens each game in the right emulator. It supports RetroArch, PPSSPP, Dolphin, DuckStation, ePSXe, Flycast, Redream, M64Plus FZ, melonDS, DraStic, Azahar, Yaba Sanshiro 2, NetherSX2, ARMSX2, MAME4droid and Eden. You can choose the emulator for each console.
- **Microsoft Xbox Nano controller.**
  - The interface works fully with the D-pad and A/B.
  - The sidebar shows the connected controller, or a reminder to pair the Xbox Nano.
  - *Réglages* has the pairing steps.
- **Hybrid storage.**
  - Local games: TV storage or a USB drive. They are launched in place.
  - Network games: an SMB share on a NAS, PC or Samba server. They are copied to the TV when you play, with a progress bar and a cancel button, then kept in a cache.
  - The cache has an adjustable limit; the least recently played games are removed first. It can be emptied, or moved to a USB drive for new copies.
- **IPv6 first, IPv4 fallback.** The SMB connection tries every IPv6 address of the server first, waiting up to 3 seconds for each, then falls back to IPv4. It accepts names, IPv4 and IPv6 addresses (with or without brackets and port), and `smb://` and `\\server` forms. Only the server part of a full share address is used.
- **First-launch welcome screen.** It explains why file access is needed and opens the Android request directly. Android 11+ shows the *All files access* screen; Android 8–10 shows the usual dialog. When access is granted, the sources are scanned again. On TVs without that screen, *Réglages* shows the `adb` command to use.

### Game library

- Multi-file discs (`.cue`, `.gdi`, `.ccd`) are shown once. Their track files are hidden and copied along with them.
- Multi-disc games grouped with an `.m3u` playlist of the same title are shown once.
- Titles are cleaned up: region and tags in brackets are shown separately.
- Cartridge games up to the Nintendo DS can be stored as `.zip` or `.7z`.
- Media, BIOS and save folders are skipped. Sub-folders are scanned up to 3 levels deep.
- The library is saved on the TV. Games from a server that is offline stay listed.

### Emulator settings screen

For each console, the screen shows:

- which emulators are installed, and which one is used;
- emulators that need a newer Android ("Demande Android X");
- what is left to set up: the folder to allow in the emulator, or the RetroArch core to download.

### Security and licences

- File names sent by an SMB server that would point outside the console's cache folder (an absolute path or `..`) are refused.
- App data, including the SMB password, is excluded from Android backups and device transfers.
- MAME4droid launches are refused when a folder name contains an apostrophe, which would break its command line.
- Third-party copyright notices and licences ship inside the APK (`assets/THIRD_PARTY_NOTICES.md`) and in the repository (`legal/THIRD_PARTY_NOTICES.md`).

### Known limitations

- **Folder access in the emulators.** Most standalone emulators (PPSSPP, Dolphin, DuckStation…) must be given access to each console folder once, in their own settings. Until then, Android blocks them from opening the game.
- **SMB authentication.** Only NTLM user/password or guest access is supported. Kerberos isn't.
- **Saved password.** The SMB password is stored in the app's private storage on the TV. It isn't encrypted, but it is left out of Android backups, so it stays on the TV.
- **Language.** The interface is in French only. *(English added in 0.2.0.)*
- **No artwork.** There is no box art or game metadata yet.

### Tested

- Unit tests (15) pass:
  - console folder names and aliases;
  - hiding disc track files and grouping `.m3u` playlists;
  - title cleanup;
  - storage-framework IDs;
  - parsing of host addresses, including full `smb://server/share` addresses;
  - IPv6 → IPv4 fallback;
  - refusing server file names that leave the console folder.
- Checked on an Android 15 emulator (Pixel 9a profile, landscape):
  - first-launch welcome screen → *All files access* → back to the app with access granted;
  - adding a local folder with the D-pad, console detection and the game list;
  - the "no emulator installed" message.
- Launching a PSP game in PPSSPP 1.19.3: the game was sent to PPSSPP correctly. PPSSPP then needed folder access, as described above.

### Not tested yet

- A real Android TV device or a real Xbox Nano controller.
- Two controllers at once. The README's *Two players* section explains how each emulator assigns players.
- An SMB server.
- Launches with RetroArch and the other emulators.

Please report what works and what doesn't on your TV.

### Build

Gradle 9.8.2, Android Gradle Plugin 9.5.0, Kotlin 2.4.21, Android SDK 37.2, Compose BOM 2026.09.00, TV Material 1.1.0, smbj 0.15.0.

The APK is signed with the author's Android debug key, for sideloading. If a later version is signed with a different key, RetroSalon will have to be uninstalled before updating, and its sources added again.
