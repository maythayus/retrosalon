# Third-party notices

RetroSalon includes or adapts the third-party material below. This file is shipped inside the APK
(`assets/THIRD_PARTY_NOTICES.md`) and attached to each GitHub release.

Since version 0.3.0, RetroSalon also contains RetroArch and libretro cores: see
[RetroArch and libretro cores](#retroarch-and-libretro-cores).

## Adapted material

| Material | Where it is used | Licence |
|---|---|---|
| ES-DE (EmulationStation Desktop Edition): Android emulator launch configuration (`es_find_rules.xml`, `es_systems.xml`): package and activity names, intent actions and extras | `app/src/main/java/fr/retrosalon/data/Catalog.kt` | MIT, see [ES-DE](#es-de) |
| RetroArch 1.22.2: names of the intent extras, activities and manifest entries that RetroArch's Android code expects (`MainMenuActivity.java`, `RetroActivityFuture.java`, `pkg/android/phoenix/AndroidManifest.xml`). Interface only: no RetroArch code is copied into these files. | `app/src/main/java/fr/retrosalon/data/EmbeddedRetroArch.kt`, `retroarch/src/main/AndroidManifest.xml` | RetroArch is GPL-3.0 |

## Libraries bundled in the APK

| Library | Version | Copyright | Licence |
|---|---|---|---|
| [smbj](https://github.com/hierynomus/smbj) | 0.15.0 | Copyright (C) 2016 SMBJ Contributors | Apache 2.0 |
| [asn-one](https://github.com/hierynomus/asn-one) | 0.6.0 | Copyright 2016 Jeroen van Erp | Apache 2.0 |
| [Bouncy Castle](https://www.bouncycastle.org) (`bcprov-jdk18on`) | 1.85.2 | Copyright (c) 2000-2026 The Legion of the Bouncy Castle Inc. | MIT, see [Bouncy Castle](#bouncy-castle) |
| [SLF4J API](https://www.slf4j.org) | 2.0.18 | Copyright (c) 2004-2022 QOS.ch Sarl | MIT, see [SLF4J](#slf4j) |
| [MBassador](https://github.com/bennidi/mbassador) | 1.3.2 | Copyright (c) 2012 Benjamin Diedrichsen | MIT, see [MBassador](#mbassador) |
| [Kotlin standard library](https://github.com/JetBrains/kotlin) | 2.4.21 | Copyright JetBrains s.r.o. and Kotlin Programming Language contributors | Apache 2.0 |
| [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) | 1.10.2 | Copyright JetBrains s.r.o. and contributors | Apache 2.0 |
| [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) (core) | 1.7.3 | Copyright JetBrains s.r.o. and contributors | Apache 2.0 |
| [JetBrains Java Annotations](https://github.com/JetBrains/java-annotations) | 23.0.0 | Copyright JetBrains s.r.o. | Apache 2.0 |
| [AndroidX / Jetpack Compose / Compose for TV](https://developer.android.com/jetpack/androidx) (`androidx.*`: activity, annotation, collection, compose, core, lifecycle, navigationevent, savedstate, tv-material, window and their dependencies) | Compose BOM 2026.09.00 | Copyright The Android Open Source Project | Apache 2.0 |
| [Guava ListenableFuture](https://github.com/google/guava) | 1.0 | Copyright The Guava Authors | Apache 2.0 |
| [JSpecify](https://jspecify.dev) | 1.0.0 | Copyright The JSpecify Authors | Apache 2.0 |

None of these libraries ships a NOTICE file.

## RetroArch and libretro cores

The RetroSalon APKs, from version 0.3.0, contain RetroArch and the libretro cores below. `retroarch/fetch.py`, run before the build, downloads them from libretro's official sources
(buildbot.libretro.com and libretro's repositories on GitHub); none of them is stored in this repository.

RetroArch's Android Java code is compiled into the same APK as RetroSalon, so the program formed by RetroSalon and
RetroArch (its Java code and native engine) is distributed under the GNU General Public License version 3 (full text:
[licenses/GPL-3.0.txt](licenses/GPL-3.0.txt)). RetroSalon's own source code stays under the MIT licence, which allows
this combination. The libretro cores, the menu sounds and the PPSSPP files below are not covered by that licence: each
keeps its own. Like libretro, this project treats each core as a separate program, loaded by RetroArch at run time
through the libretro API.

| Component | Version | In the APK | Authors | Licence |
|---|---|---|---|---|
| [RetroArch](https://github.com/libretro/RetroArch), Android Java code (RetroActivityCommon, RetroActivityCamera, RetroActivityIntent, RetroActivityFuture, MainMenuActivity, ConfigFile, UserPreferences, PlayCoreManager) | 1.22.2 (tag `v1.22.2`), unmodified | `classes*.dex` (`com.retroarch.*`) | The RetroArch team | GPL-3.0 |
| libretro-common `VfsImplementationSaf.java` | 1.22.2 (tag `v1.22.2`), unmodified | `classes*.dex` (`com.libretro.common.vfs`) | Copyright (C) 2010-2020 The RetroArch team | MIT, see [libretro-common](#libretro-common) |
| RetroArch native engine | 1.22.2, from the official APKs on buildbot.libretro.com/stable | `lib/<abi>/libretroarch-activity.so` | The RetroArch team | GPL-3.0. RetroArch's own third-party notices are in `info/open-source-notices.info` inside `assets/retroarch/ra-assets.zip` |
| RetroArch files: menus, fonts, controller profiles, core information, filters | 1.22.2 | `assets/retroarch/ra-assets.zip` | The RetroArch team and the authors listed in `open-source-notices.info` | GPL-3.0; the fonts keep their own licences (M+ FONTS, OFL-1.1, Bitstream Vera, Apache-2.0) |
| RetroArch menu sounds | 1.22.2 | `assets/retroarch/ra-assets.zip` (`assets/sounds`) | ViRiX Dreamcore | [CC BY-NC 3.0 US](https://creativecommons.org/licenses/by-nc/3.0/us/) (licence note: `assets/sounds/Licence.txt` in the zip) |
| [PPSSPP](https://github.com/hrydgard/ppsspp) system files: font atlases, translations, shaders, sounds, the Roboto Condensed and Inconsolata fonts, and the PSP system fonts (`flash0/font/*.pgf`) as redistributed by the PPSSPP project | buildbot.libretro.com/assets/system/PPSSPP.zip | `assets/retroarch/ppsspp-system.zip` | PPSSPP project (see `PPSSPP/LICENSE.TXT` in the zip) | GPL-2.0 or later for PPSSPP's files; Roboto Condensed and Inconsolata: OFL-1.1; the PSP system fonts are not covered by PPSSPP's licence |
| [Mesen](https://github.com/libretro/Mesen) | buildbot nightly of 2026-11-12; source: `0102910`* | `lib/<abi>/libmesen_libretro_android.so` | M. Bibaud (Sour) | GPL-3.0 |
| [FCEUmm](https://github.com/libretro/libretro-fceumm) | `e0f3086` (buildbot nightly of 2026-11-12) | `libfceumm_libretro_android.so` | FCEU Team, CaH4e3 | GPL-2.0 |
| [Snes9x](https://github.com/libretro/snes9x) | 1.63 (`fae2fea`) | `libsnes9x_libretro_android.so` | Snes9x Team | Snes9x licence, **non-commercial**; its NTSC filter (snes_ntsc, Shay Green) is LGPL-2.1 or later |
| [Mupen64Plus-Next](https://github.com/libretro/mupen64plus-libretro-nx) (GLES3) | `15bada8` | `libmupen64plus_next_gles3_libretro_android.so` | m4xw, Hacktarux, gonetz, GLideN64 contributors, Mupen64Plus Team | GPL-2.0 |
| [Gambatte](https://github.com/libretro/gambatte-libretro) | `77f2567` | `libgambatte_libretro_android.so` | Sinamas | GPL-2.0 |
| [mGBA](https://github.com/libretro/mgba) | 0.11-212 (`7a12d6d`) | `libmgba_libretro_android.so` | endrift | MPL-2.0 |
| [melonDS DS](https://github.com/JesseTG/melonds-ds) | 1.4.0, buildbot nightly of 2026-11-12; source: `f394adb`* | `libmelondsds_libretro_android.so` | Jesse Talavera, the melonDS team | GPL-3.0 or later |
| [Genesis Plus GX](https://github.com/libretro/Genesis-Plus-GX) | 1.7.4 (`b9c2316`) | `libgenesis_plus_gx_libretro_android.so` | Charles MacDonald, Eke-Eke | Genesis Plus GX licence, **non-commercial**; Nuked OPN2 (Alexey Khokholov), the NTSC filter and Blip Buffer (Shay Green) are LGPL-2.1 or later |
| [YabaSanshiro](https://github.com/libretro/yabause) | 3.4.2 (`09ed8e5`) | `libyabasanshiro_libretro_android.so` | Guillaume Duhammel, Theo Berkau, Anders Montonen, devmiyax | GPL-2.0 |
| [Flycast](https://github.com/flyinghead/flycast) | `72b8e67` (buildbot nightly of 2026-11-12) | `libflycast_libretro_android.so` | flyinghead | GPL-2.0 |
| [PCSX ReARMed](https://github.com/libretro/pcsx_rearmed) | r26 (`7b0e8f8`) | `libpcsx_rearmed_libretro_android.so` | PCSX Team, notaz, Exophase | GPL-2.0 |
| [SwanStation](https://github.com/libretro/swanstation) | 1.0.0 (`631f0fb`) | `libswanstation_libretro_android.so` | stenzek | GPL-3.0 |
| [PPSSPP](https://github.com/hrydgard/ppsspp) (core) | `55d8795` (buildbot nightly of 2026-11-10) | `libppsspp_libretro_android.so` | Henrik Rydgård | GPL-2.0 or later |
| [Beetle PCE Fast](https://github.com/libretro/beetle-pce-fast-libretro) | 1.31.0.0 (`2de1ce1`) | `libmednafen_pce_fast_libretro_android.so` | Mednafen Team | GPL-2.0 |
| [FinalBurn Neo](https://github.com/libretro/FBNeo) | buildbot nightly of 2026-11-12 (no revision reported) | `libfbneo_libretro_android.so` | Team FBNeo | FBNeo licence, **non-commercial** |
| [Stella](https://github.com/stella-emu/stella) | `da5a002` | `libstella_libretro_android.so` | Stephen Anthony, Bradford Mott, Eckhard Stolberg, Brian Watson | GPL-2.0 |

The versions in brackets are the source revisions the core binaries report. The Mesen, melonDS DS and FinalBurn Neo
binaries report none: they are libretro's nightly builds of 2026-11-12. \* For Mesen and melonDS DS, the source
revision given is not reported by the binary: it is the latest commit of the repository's main branch on that day
(from 2026-04-20 and 2026-09-27), and nothing was committed between it and the build. `fetch.py` writes `VERSIONS.txt` (in the APK:
`assets/retroarch/VERSIONS.txt`), which gives the SHA-256 of every file it downloaded and the build date of each core.

Licence texts shipped in each APK:

- GPL-3.0, GPL-2.0, MPL-2.0 and OFL-1.1 (fonts): [licenses/GPL-3.0.txt](licenses/GPL-3.0.txt),
  [licenses/GPL-2.0.txt](licenses/GPL-2.0.txt), [licenses/MPL-2.0.txt](licenses/MPL-2.0.txt) and
  [licenses/OFL-1.1.txt](licenses/OFL-1.1.txt) (`assets/licenses/` in the APK).
- The Snes9x, Genesis Plus GX and FinalBurn Neo licences, with their copyright notices, which must accompany every
  copy of these cores: `assets/retroarch/licenses/snes9x.txt`, `genesis_plus_gx.txt` and `fbneo.txt` in the APK.
  `fetch.py` takes them from each core's repository linked above (`LICENSE`, `LICENSE.txt` and `src/license.txt`, on
  the `master` branch), so they are not stored in this repository. `genesis_plus_gx.txt` also contains the full
  text of the GNU LGPL version 2.1, which covers the LGPL parts of Genesis Plus GX and Snes9x.

The licence of the menu sounds, CC BY-NC 3.0 US, is given by its address, as that licence allows:
<https://creativecommons.org/licenses/by-nc/3.0/us/legalcode>.

### Non-commercial components

Snes9x, Genesis Plus GX, FinalBurn Neo and RetroArch's menu sounds may only be used and redistributed for
non-commercial purposes. A RetroSalon APK must therefore be given away free of charge. It must not be sold, bundled
with a paid product or device, offered behind a paywall or in exchange for a donation, or distributed from a page or
an app that shows advertising. Anyone who redistributes it must keep these conditions.

### Changes to RetroArch

The RetroArch Java files listed above are unmodified from tag `v1.22.2`. RetroSalon builds them with its own Gradle
module and Android manifest (`retroarch/build.gradle.kts`, `retroarch/src/main/AndroidManifest.xml`): RetroArch runs in
its own `:retroarch` process, is not exported and has no launcher icon. When it starts a game or the menu, RetroSalon
writes some of RetroArch's settings (`EmbeddedRetroArch.kt`: core folder, core downloader hidden, menu driver,
menu buttons, controller defaults and player reservations), closes RetroArch when it leaves the screen, and changes
the device model name that RetroArch reads (`libretrosalon-getprop.so`). Changes by the RetroSalon author,
November 2026.

### Source code

Each GitHub release of RetroSalon, from 0.3.0, comes with the complete corresponding source code of RetroArch, of
every GPL or MPL core in its APKs, and of Snes9x and Genesis Plus GX for their LGPL parts (GPL-3.0 section 6,
GPL-2.0 section 3, LGPL-2.1 sections 4 and 6, MPL-2.0 section 3.2):
<https://github.com/maythayus/retrosalon/releases>. Next to the APKs, each `<name>-<commit>-source.tar.xz` holds
one component at the revision listed above, with its git submodules and, for melonDS DS, the dependencies its
build fetches, each at the commit the build used. `SOURCES.txt` at the top of each archive lists every repository
and commit in it.

- RetroSalon, including `retroarch/fetch.py` and the Gradle build that produce the APKs: this repository, at the
  release's tag (for example `v0.3.0`).
- RetroArch 1.22.2: `RetroArch-1.22.2-69a4f0e-source.tar.xz`, also at
  <https://github.com/libretro/RetroArch/tree/v1.22.2>.
- Each GPL or MPL core, Snes9x and Genesis Plus GX: its archive in the release, also in its repository in the table
  above, at the revision listed. The archives of Snes9x and Genesis Plus GX stay under their non-commercial
  licences, except the LGPL files in them.
- FinalBurn Neo: its licence does not require the source to be offered, and its binary reports no revision; its
  source is in its repository linked above.

If you redistribute a RetroSalon APK, pass on these archives with it, or the link to the release they come from.
An APK you build yourself contains the nightly cores of the day you first ran `fetch.py` (see its `VERSIONS.txt`):
if you share it, build it from a committed tree and offer the source of those revisions.

### Trademarks

RetroArch and libretro are trademarks of the libretro team. Nintendo, Sega, Sony, PlayStation, NEC, SNK, Atari,
Xbox and the console names are trademarks of their owners. RetroSalon is not affiliated with or endorsed by the
libretro team, the authors of any emulator or core, Microsoft, or any console maker. RetroSalon names RetroArch only
to identify it.

---

## Apache License 2.0

```text
                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity authorized by
      the copyright owner that is granting the License.

      "Legal Entity" shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of this definition,
      "control" means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" form shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" form shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (an example is provided in the Appendix below).

      "Derivative Works" shall mean any work, whether in Source or Object
      form, that is based on (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship. For the purposes
      of this License, Derivative Works shall not include works that remain
      separable from, or merely link (or bind by name) to the interfaces of,
      the Work and Derivative Works thereof.

      "Contribution" shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of this definition, "submitted"
      means any form of electronic, verbal, or written communication sent
      to the Licensor or its representatives, including but not limited to
      communication on electronic mailing lists, source code control systems,
      and issue tracking systems that are managed by, or on behalf of, the
      Licensor for the purpose of discussing and improving the Work, but
      excluding communication that is conspicuously marked or otherwise
      designated in writing by the copyright owner as "Not a Contribution."

      "Contributor" shall mean Licensor and any individual or Legal Entity
      on behalf of whom a Contribution has been received by Licensor and
      subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to reproduce, prepare Derivative Works of,
      publicly display, publicly perform, sublicense, and distribute the
      Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      (except as stated in this section) patent license to make, have made,
      use, offer to sell, sell, import, and otherwise transfer the Work,
      where such license applies only to those patent claims licensable
      by such Contributor that are necessarily infringed by their
      Contribution(s) alone or by combination of their Contribution(s)
      with the Work to which such Contribution(s) was submitted. If You
      institute patent litigation against any entity (including a
      cross-claim or counterclaim in a lawsuit) alleging that the Work
      or a Contribution incorporated within the Work constitutes direct
      or contributory patent infringement, then any patent licenses
      granted to You under this License for that Work shall terminate
      as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the following conditions:

      (a) You must give any other recipients of the Work or
          Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices
          stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works
          that You distribute, all copyright, patent, trademark, and
          attribution notices from the Source form of the Work,
          excluding those notices that do not pertain to any part of
          the Derivative Works; and

      (d) If the Work includes a "NOTICE" text file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE text file distributed
          as part of the Derivative Works; within the Source form or
          documentation, if provided along with the Derivative Works; or,
          within a display generated by the Derivative Works, if and
          wherever such third-party notices normally appear. The contents
          of the NOTICE file are for informational purposes only and
          do not modify the License. You may add Your own attribution
          notices within Derivative Works that You distribute, alongside
          or as an addendum to the NOTICE text from the Work, provided
          that such additional attribution notices cannot be construed
          as modifying the License.

      You may add Your own copyright statement to Your modifications and
      may provide additional or different license terms and conditions
      for use, reproduction, or distribution of Your modifications, or
      for any such Derivative Works as a whole, provided Your use,
      reproduction, and distribution of the Work otherwise complies with
      the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License, without any additional terms or conditions.
      Notwithstanding the above, nothing herein shall supersede or modify
      the terms of any separate license agreement you may have executed
      with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor,
      except as required for reasonable and customary use in describing the
      origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work (and each
      Contributor provides its Contributions) on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
      implied, including, without limitation, any warranties or conditions
      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
      PARTICULAR PURPOSE. You are solely responsible for determining the
      appropriateness of using or redistributing the Work and assume any
      risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory,
      whether in tort (including negligence), contract, or otherwise,
      unless required by applicable law (such as deliberate and grossly
      negligent acts) or agreed to in writing, shall any Contributor be
      liable to You for damages, including any direct, indirect, special,
      incidental, or consequential damages of any character arising as a
      result of this License or out of the use or inability to use the
      Work (including but not limited to damages for loss of goodwill,
      work stoppage, computer failure or malfunction, or any and all
      other commercial damages or losses), even if such Contributor
      has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. While redistributing
      the Work or Derivative Works thereof, You may choose to offer,
      and charge a fee for, acceptance of support, warranty, indemnity,
      or other liability obligations and/or rights consistent with this
      License. However, in accepting such obligations, You may act only
      on Your own behalf and on Your sole responsibility, not on behalf
      of any other Contributor, and only if You agree to indemnify,
      defend, and hold each Contributor harmless for any liability
      incurred by, or claims asserted against, such Contributor by reason
      of your accepting any such warranty or additional liability.

   END OF TERMS AND CONDITIONS
```

---

## ES-DE

```text
MIT License

Copyright (c) 2024-2026 Northwestern Software AB
Copyright (c) 2020-2024 Leon Styhre
Copyright (c) 2014 Alec Lofquist

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
```

## Bouncy Castle

```text
MIT License (https://opensource.org/licenses/MIT)

Copyright (c) 2000-2026 The Legion of the Bouncy Castle Inc. (https://www.bouncycastle.org).
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
associated documentation files (the "Software"), to deal in the Software without restriction,
including without limitation the rights to use, copy, modify, merge, publish, distribute,
sub license, and/or sell copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions: The above copyright notice and this
permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT
OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
```

## SLF4J

```text
Copyright (c) 2004-2022 QOS.ch Sarl (Switzerland)
All rights reserved.

Permission is hereby granted, free  of charge, to any person obtaining
a  copy  of this  software  and  associated  documentation files  (the
"Software"), to  deal in  the Software without  restriction, including
without limitation  the rights to  use, copy, modify,  merge, publish,
distribute,  sublicense, and/or sell  copies of  the Software,  and to
permit persons to whom the Software  is furnished to do so, subject to
the following conditions:

The  above  copyright  notice  and  this permission  notice  shall  be
included in all copies or substantial portions of the Software.

THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
```

## MBassador

```text
MIT License

Copyright (c) 2012 Benjamin Diedrichsen

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
```

## libretro-common

`VfsImplementationSaf.java`, from RetroArch's `libretro-common`:

```text
Copyright  (C) 2010-2020 The RetroArch team

---------------------------------------------------------------------------------------
The following license statement only applies to this file (VfsImplementationSaf.java).
---------------------------------------------------------------------------------------

Permission is hereby granted, free of charge,
to any person obtaining a copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
```
