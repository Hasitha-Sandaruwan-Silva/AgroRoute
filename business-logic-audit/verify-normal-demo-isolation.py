"""Inspect built debug/release artifacts; never print key/configuration content."""
from pathlib import Path
from zipfile import ZipFile
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
out=root/"build/normal-demo/app"
android="{http://schemas.android.com/apk/res/android}"
for variant,filename in [("debug","app-debug.apk"),("release","app-release-unsigned.apk")]:
    with ZipFile(out/"outputs/apk"/variant/filename) as archive:
        dex=b"".join(archive.read(n) for n in archive.namelist() if n.startswith("classes") and n.endswith(".dex"))
    for marker in (b"Lcom/agroroute/demo/",b"Try Demo Mode",b"demo_farmer_001",b"agroroute_local_demo_db"):
        assert (marker in dex)==(variant=="debug"),f"Unexpected {variant} demo artifact boundary"
    manifest=ET.parse(out/"intermediates/merged_manifests"/variant/f"process{variant.title()}Manifest/AndroidManifest.xml")
    launchers=[]
    for activity in manifest.iter("activity"):
        for intent in activity.findall("intent-filter"):
            if any(c.get(android+"name")=="android.intent.category.LAUNCHER" for c in intent.findall("category")):
                launchers.append(activity.get(android+"name"))
    assert launchers==["com.agroroute.MainActivity"],launchers
    print(f"PASS {variant}: correct demo DEX boundary; one normal launcher")
for source in (root/"app/src/main").rglob("*.kt"):
    text=source.read_text(encoding="utf-8")
    assert "demo_farmer_" not in text and "demo_supplier_" not in text and "agroroute_local_demo_db" not in text
for source in (root/"app/src/debug/java/com/agroroute/demo").glob("*.kt"):
    text=source.read_text(encoding="utf-8")
    assert "import com.google.firebase" not in text
    assert "import com.agroroute.core.remote.Firestore" not in text
print("PASS demo identity/data-source code stays debug-only with no Firebase client dependency")
