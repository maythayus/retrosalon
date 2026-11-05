# Release notes

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
- **Language.** The interface is in French only.
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
