"""Récupère RetroArch et ses cœurs pour la version perso de RetroSalon.

Tout vient des sources officielles de libretro :
  - le code Java d'Android de RetroArch, sur GitHub (tag de la version) ;
  - le moteur natif (libretroarch-activity.so) et ses fichiers (menus, polices, profils de manettes,
    fiches des cœurs), dans les APK officiels de buildbot.libretro.com/stable ;
  - les cœurs, sur buildbot.libretro.com/nightly (les mêmes que le « Core Downloader » de RetroArch) ;
  - les fichiers système de PPSSPP (polices PSP), sur buildbot.libretro.com/assets/system ;
  - le texte de licence des cœurs non commerciaux, dans leurs dépôts sur GitHub.

Usage : python -I retroarch/fetch.py [dossier de sortie]   (par défaut : retroarch/vendor)
Rien n'est versionné dans git : le dossier de sortie est ignoré.
"""

import hashlib
import io
import os
import shutil
import sys
import urllib.request
import zipfile

RA_VERSION = "1.22.2"
RA_TAG = f"v{RA_VERSION}"

# Le moteur 32 bits vient de l'APK ra32, le 64 bits de l'APK aarch64.
ENGINE = {
    "armeabi-v7a": f"https://buildbot.libretro.com/stable/{RA_VERSION}/android/RetroArch_ra32.apk",
    "arm64-v8a": f"https://buildbot.libretro.com/stable/{RA_VERSION}/android/RetroArch_aarch64.apk",
}

# Code Java de RetroArch (le moteur natif l'appelle par JNI : il doit venir du même tag).
JAVA = {
    "pkg/android/phoenix-common/src/": [
        "com/retroarch/browser/retroactivity/RetroActivityCommon.java",
        "com/retroarch/browser/retroactivity/RetroActivityCamera.java",
        "com/retroarch/browser/retroactivity/RetroActivityIntent.java",
        "com/retroarch/browser/preferences/util/ConfigFile.java",
        "com/retroarch/browser/preferences/util/UserPreferences.java",
    ],
    "pkg/android/phoenix/src/": [
        "com/retroarch/browser/retroactivity/RetroActivityFuture.java",
        "com/retroarch/browser/mainmenu/MainMenuActivity.java",
    ],
    "pkg/android/play-core-stub/": [
        "com/retroarch/playcore/PlayCoreManager.java",
    ],
    "libretro-common/vfs/saf/src/": [
        "com/libretro/common/vfs/VfsImplementationSaf.java",
    ],
}

# Cœurs intégrés : ceux du catalogue de RetroSalon (doit rester aligné avec Catalog.kt).
CORES = [
    "mesen", "fceumm",                      # NES
    "snes9x",                               # Super Nintendo
    "mupen64plus_next_gles3",               # Nintendo 64
    "gambatte", "mgba",                     # Game Boy, Game Boy Color, Game Boy Advance
    "melondsds",                            # Nintendo DS
    "genesis_plus_gx",                      # Master System, Mega Drive, Game Gear, Mega-CD
    "yabasanshiro",                         # Saturn
    "flycast",                              # Dreamcast
    "pcsx_rearmed", "swanstation",          # PlayStation
    "ppsspp",                               # PSP
    "mednafen_pce_fast",                    # PC Engine
    "fbneo",                                # Neo Geo, arcade
    "stella",                               # Atari 2600
]
CORE_URL = "https://buildbot.libretro.com/nightly/android/latest/{abi}/{core}_libretro_android.so.zip"
PPSSPP_URL = "https://buildbot.libretro.com/assets/system/PPSSPP.zip"

# Licences des cœurs non commerciaux : leur texte doit accompagner chaque copie du cœur.
# Livrées dans l'APK (assets/retroarch/licenses), voir legal/THIRD_PARTY_NOTICES.md.
CORE_LICENSES = {
    "snes9x": "https://raw.githubusercontent.com/libretro/snes9x/master/LICENSE",
    "genesis_plus_gx": "https://raw.githubusercontent.com/libretro/Genesis-Plus-GX/master/LICENSE.txt",
    "fbneo": "https://raw.githubusercontent.com/libretro/FBNeo/master/src/license.txt",
}

# Dossiers de l'APK officiel copiés dans RetroSalon (les autres ne servent pas sur une TV :
# bases de données des playlists, overlays tactiles, shaders, téléchargeables depuis RetroArch).
ASSET_DIRS = ("assets/", "info/", "autoconfig/", "filters/")


def download(url, cache):
    name = url.rsplit("/", 1)[1]
    path = os.path.join(cache, name if "nightly" not in url else url.split("/")[-2] + "_" + name)
    if not os.path.exists(path):
        print("téléchargement :", url)
        tmp = path + ".part"
        with urllib.request.urlopen(url, timeout=120) as r, open(tmp, "wb") as f:
            shutil.copyfileobj(r, f)
        os.replace(tmp, path)
    return path


def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for block in iter(lambda: f.read(1 << 20), b""):
            h.update(block)
    return h.hexdigest()


def main():
    here = os.path.dirname(os.path.abspath(__file__))
    out = os.path.abspath(sys.argv[1]) if len(sys.argv) > 1 else os.path.join(here, "vendor")
    cache = os.path.join(out, "cache")
    os.makedirs(cache, exist_ok=True)
    for sub in ("java", "jniLibs", "assets"):
        shutil.rmtree(os.path.join(out, sub), ignore_errors=True)
    log = [f"RetroArch {RA_VERSION} (GitHub libretro/RetroArch, tag {RA_TAG})"]

    # 1. Code Java, depuis GitHub.
    for prefix, files in JAVA.items():
        for rel in files:
            url = f"https://raw.githubusercontent.com/libretro/RetroArch/{RA_TAG}/{prefix}{rel}"
            dst = os.path.join(out, "java", rel)
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            with urllib.request.urlopen(url, timeout=60) as r:
                data = r.read()
            open(dst, "wb").write(data)
            log.append(f"  java  {prefix}{rel}  sha256 {hashlib.sha256(data).hexdigest()}")
    with urllib.request.urlopen(f"https://raw.githubusercontent.com/libretro/RetroArch/{RA_TAG}/COPYING", timeout=60) as r:
        data = r.read()
    open(os.path.join(out, "COPYING"), "wb").write(data)
    log.append(f"  texte COPYING  sha256 {hashlib.sha256(data).hexdigest()}")

    # 2. Moteur natif et fichiers de RetroArch, depuis les APK officiels.
    assets_zip = None
    for abi, url in ENGINE.items():
        apk = download(url, cache)
        log.append(f"  apk   {url}  sha256 {sha256(apk)}")
        z = zipfile.ZipFile(apk)
        lib = z.read(f"lib/{abi}/libretroarch-activity.so")
        os.makedirs(os.path.join(out, "jniLibs", abi), exist_ok=True)
        open(os.path.join(out, "jniLibs", abi, "libretroarch-activity.so"), "wb").write(lib)
        if assets_zip is None:
            # Un seul zip dans l'APK de RetroSalon, décompressé sur la TV au premier lancement :
            # des milliers de petits fichiers, inutile de les passer un par un dans le build.
            os.makedirs(os.path.join(out, "assets", "retroarch"), exist_ok=True)
            assets_zip = os.path.join(out, "assets", "retroarch", "ra-assets.zip")
            count = 0
            with zipfile.ZipFile(assets_zip, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as dst:
                for info in z.infolist():
                    name = info.filename
                    if not name.startswith("assets/") or info.is_dir():
                        continue
                    rel = name[len("assets/"):]
                    if rel.startswith(ASSET_DIRS):
                        dst.writestr(rel, z.read(info))
                        count += 1
            log.append(f"  fichiers de RetroArch : {count} ({', '.join(d.rstrip('/') for d in ASSET_DIRS)})")

    # 3. Cœurs, pour chaque architecture.
    for abi in ENGINE:
        for core in CORES:
            url = CORE_URL.format(abi=abi, core=core)
            zpath = download(url, cache)
            z = zipfile.ZipFile(zpath)
            so = f"{core}_libretro_android.so"
            data = z.read(so)
            # Préfixe « lib » : Android n'extrait de l'APK que les fichiers lib*.so.
            open(os.path.join(out, "jniLibs", abi, f"lib{so}"), "wb").write(data)
            date = z.getinfo(so).date_time
            log.append(f"  cœur  {abi:12s} {core:24s} {len(data):>10d} octets  du {date[0]:04d}-{date[1]:02d}-{date[2]:02d}"
                       f"  sha256 {hashlib.sha256(data).hexdigest()}")

    # 4. Fichiers système de PPSSPP : atlas de polices, traductions, shaders, et les polices système de la PSP
    #    (flash0/font) telles que le projet PPSSPP les redistribue. Voir legal/THIRD_PARTY_NOTICES.md.
    ppsspp = download(PPSSPP_URL, cache)
    shutil.copyfile(ppsspp, os.path.join(out, "assets", "retroarch", "ppsspp-system.zip"))
    log.append(f"  système {PPSSPP_URL}  sha256 {sha256(ppsspp)}")

    # 5. Texte de licence des cœurs non commerciaux.
    licenses = os.path.join(out, "assets", "retroarch", "licenses")
    os.makedirs(licenses, exist_ok=True)
    for core, url in CORE_LICENSES.items():
        with urllib.request.urlopen(url, timeout=60) as r:
            data = r.read()
        open(os.path.join(licenses, f"{core}.txt"), "wb").write(data)
        log.append(f"  licence {url}  sha256 {hashlib.sha256(data).hexdigest()}")

    # La liste des fichiers et de leurs empreintes, aussi dans l'APK (assets/retroarch/VERSIONS.txt).
    for path in (os.path.join(out, "VERSIONS.txt"), os.path.join(out, "assets", "retroarch", "VERSIONS.txt")):
        open(path, "w", encoding="utf-8").write("\n".join(log) + "\n")
    print("\n".join(log))
    print("prêt :", out)


if __name__ == "__main__":
    main()
