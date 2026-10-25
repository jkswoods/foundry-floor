#!/usr/bin/env python3
"""Deterministic mod-only JAR: classes plus original native assets; no game binaries or reports."""
from pathlib import Path
import hashlib,json,zipfile
from release_pipeline import config, filename
ROOT=Path(__file__).resolve().parents[1]
out=ROOT/'build/libs'/filename(config(ROOT/'release.json'))
entries={}
for folder in [ROOT/'build/classes',ROOT/'assets']:
    for p in folder.rglob('*'):
        if p.is_file():entries[p.relative_to(folder).as_posix()]=p.read_bytes()
with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
    for name,data in sorted(entries.items()):
        info=zipfile.ZipInfo(name,(2026,10,21,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=0o100644<<16;z.writestr(info,data)
print(out.name,hashlib.sha256(out.read_bytes()).hexdigest(),len(entries),'entries')
