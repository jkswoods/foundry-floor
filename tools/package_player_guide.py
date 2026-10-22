#!/usr/bin/env python3
"""Deterministic built player-guide preview; never includes a game build or private evidence."""
from pathlib import Path
import argparse,hashlib,zipfile
parser=argparse.ArgumentParser(description=__doc__);parser.add_argument("--public-alpha",action="store_true");args=parser.parse_args()

ROOT=Path(__file__).resolve().parents[1]
site=ROOT/'build/player-site'
if not (site/'index.html').is_file():raise SystemExit('Build the guide first: python -m mkdocs build --strict')
allowed={'.html','.css','.js','.json','.xml','.gz','.png','.svg','.ico','.woff','.woff2','.eot','.ttf'}
entries={}
for path in sorted(site.rglob('*')):
    if not path.is_file():continue
    if path.is_symlink() or path.suffix not in allowed:raise SystemExit('Unexpected guide asset: '+path.name)
    entries['site/'+path.relative_to(site).as_posix()]=path.read_bytes()
entries['DOCUMENTATION-NOTICES.md']=(ROOT/'docs/third-party.md').read_bytes()
entries['README.txt']=b'''FOUNDRY FLOOR 0.2.0 - PRIVATE PLAYER GUIDE PREVIEW

This archive is the built documentation site, not the installable gameplay mod.
It describes the Hytale0.7.3 private development build and its known limits.
Current-build native screenshots and complete gameplay acceptance are pending.
No public hosting or production support is declared.

To preview with working search, serve the site directory over local HTTP:
  python3 -m http.server 8000 --bind 127.0.0.1 --directory site
Then open http://127.0.0.1:8000 in a browser. Stop that preview with Ctrl+C.
The site can also be placed at a separately authorized static hosting destination.

The source is https://github.com/jkswoods/foundry-floor (private access required).
Third-party documentation notices are retained in DOCUMENTATION-NOTICES.md
and the site's Documentation notices page. No new original-mod license is granted.
'''
if args.public_alpha:
    entries['README.txt']=b'''FOUNDRY FLOOR 0.2.0 - PLAYER GUIDE

This archive is the player documentation, not the installable gameplay mod.
It covers the public alpha for Hytale0.7.3 / Java25, including installation,
crafting, connections, controls, compatibility and backups.

Online guide: https://jkswoods.github.io/foundry-floor/
Downloads: https://github.com/jkswoods/foundry-floor/releases
Source: https://github.com/jkswoods/foundry-floor

To preview with working search, serve the site folder locally:
  python3 -m http.server 8000 --bind 127.0.0.1 --directory site
Then open http://127.0.0.1:8000/ .

Illustrations are original model renders. Gameplay screenshots are not included.
Third-party documentation notices are retained in DOCUMENTATION-NOTICES.md
and the site's Documentation notices page. No new redistribution license is granted.
'''
entries['SHA256SUMS.txt']=''.join(hashlib.sha256(data).hexdigest()+'  '+name+'\n' for name,data in sorted(entries.items())).encode()
output=ROOT/'build/FoundryFloor-0.2.0-player-guide-preview.zip'
with zipfile.ZipFile(output,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=9) as archive:
    for name,data in sorted(entries.items()):
        info=zipfile.ZipInfo(name,(2026,10,21,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=0o100644<<16;archive.writestr(info,data)
with zipfile.ZipFile(output) as archive:
    assert archive.testzip() is None
    assert not any(name.startswith(('/', '../')) or '/..' in name or name.endswith(('.jar','.log','.bson')) for name in archive.namelist())
print(output.name,hashlib.sha256(output.read_bytes()).hexdigest(),len(entries),'player-guide files')
