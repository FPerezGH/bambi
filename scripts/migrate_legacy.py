"""Export the installed legacy debug profile data without extracting Android Keystore secrets.
Run on a trusted computer: python -m pip install cryptography; python migrate_legacy.py
Requires adb in PATH and a single authorized Android TV device.
"""
import getpass
import json
import os
import pathlib
import subprocess
import xml.etree.ElementTree as ET
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC
from cryptography.hazmat.primitives import hashes
import re

PACKAGE = "com.cinefran.tv.debug"
MAGIC = b"CFRAN01"

def read_pref(name, required=False):
    if not re.fullmatch(r"[A-Za-z0-9_-]{1,90}", name):
        raise ValueError("Invalid preference name")
    result = subprocess.run(["adb", "exec-out", "run-as", PACKAGE, "cat", f"shared_prefs/{name}.xml"], capture_output=True)
    if result.returncode or not result.stdout.startswith(b"<?xml"):
        if required or b"No such file" not in (result.stderr + result.stdout):
            raise RuntimeError("Unable to read " + name + "; confirm the old app is installed and adb is authorized.")
        return {}
    root = ET.fromstring(result.stdout)
    if root.tag != "map":
        raise ValueError("Invalid Android preference file")
    values = {}
    for elem in root:
        key = elem.attrib["name"]
        tag = elem.tag
        if tag == "string":
            item = {"t": "s", "v": elem.text or ""}
        elif tag == "boolean":
            item = {"t": "b", "v": elem.attrib["value"] == "true"}
        elif tag == "int":
            item = {"t": "i", "v": int(elem.attrib["value"])}
        elif tag in ("long", "float"):
            item = {"t": "l" if tag == "long" else "f", "v": elem.attrib["value"]}
        elif tag == "set":
            item = {"t": "set", "v": [v.text or "" for v in elem]}
        else:
            raise ValueError("Unsupported preference type")
        values[key] = item
    return values

def main():
    subprocess.run(["adb", "get-state"], check=True, capture_output=True)
    settings = read_pref("settings", required=True)
    profiles = read_pref("profiles", required=True)
    ids = [p["id"] for p in json.loads(profiles["list"]["v"])]
    if not 1 <= len(ids) <= 5 or len(set(ids)) != len(ids):
        raise ValueError("Invalid profile list")
    prefs = {"settings": settings, "profiles": profiles}
    for pid in ids:
        if not re.fullmatch(r"[A-Za-z0-9_-]{1,64}", pid):
            raise ValueError("Invalid profile identifier")
        for name in ("watch_progress" if pid == "default" else "watch_progress_" + pid,
                     "my_list_" + pid, "episode_tracking_" + pid):
            prefs[name] = read_pref(name)
    credentials = dict.fromkeys(["rd_token", "tmdb_key", "subdl_key", "aiostreams_url", "torbox_key",
                                "premiumize_key", "alldebrid_key", "debridlink_key", "offcloud_key", "trakt_app_credentials"])
    print("The Android Keystore secrets cannot be transferred from the old installation.")
    for name in credentials:
        credentials[name] = getpass.getpass(f"{name} (Enter to skip, reconnect Trakt later): ").strip() or None
    password = getpass.getpass("Backup password (12+ characters): ")
    if len(password) < 12 or password != getpass.getpass("Confirm password: "):
        raise ValueError("Password too short or different")
    data = json.dumps({"schema": 1, "preferences": prefs, "credentials": credentials}, ensure_ascii=False).encode("utf-8")
    if len(data) > 16 * 1024 * 1024:
        raise ValueError("Backup too large")
    salt, nonce = os.urandom(16), os.urandom(12)
    key = PBKDF2HMAC(algorithm=hashes.SHA256(), length=32, salt=salt, iterations=600_000).derive(password.encode("utf-8"))
    out = pathlib.Path("CINEFRAN-legacy.cfb")
    with out.open("xb") as f:
        f.write(MAGIC + salt + nonce + AESGCM(key).encrypt(nonce, data, MAGIC))
    print("Encrypted backup created:", out.resolve())
    print("Copy it to Android TV and restore from CINEFRAN. Do not delete the legacy app until confirmed.")

if __name__ == "__main__":
    main()
