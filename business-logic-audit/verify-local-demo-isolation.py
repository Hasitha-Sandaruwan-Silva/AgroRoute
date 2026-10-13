"""Verify actual built APK boundaries without printing configuration or credentials."""
from pathlib import Path
from zipfile import ZipFile
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
outputs = root / "build/local-demo/app"
namespace = b"Lcom/agroroute/demo/"
for variant, apk in [("debug", "app-debug.apk"), ("release", "app-release-unsigned.apk")]:
    path = outputs / "outputs/apk" / variant / apk
    with ZipFile(path) as archive:
        found = any(namespace in archive.read(name) for name in archive.namelist()
                    if name.startswith("classes") and name.endswith(".dex"))
    assert found == (variant == "debug"), f"Unexpected demo DEX contents in {variant}"
    manifest = outputs / "intermediates/merged_manifests" / variant / f"process{variant.title()}Manifest/AndroidManifest.xml"
    names = [node.get("{http://schemas.android.com/apk/res/android}name")
             for node in ET.parse(manifest).iter("activity")]
    assert ("com.agroroute.demo.DemoActivity" in names) == (variant == "debug")
    print(f"PASS {variant}: demo classes and launcher {'present' if found else 'absent'}")
print("PASS production source files do not depend on debug fixtures")
for source in (root / "app/src/main").rglob("*"):
    if source.suffix in (".kt", ".xml"):
        text = source.read_text(encoding="utf-8")
        assert "com.agroroute.demo" not in text and "loadDemoData(" not in text
