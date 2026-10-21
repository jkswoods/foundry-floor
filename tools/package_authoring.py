#!/usr/bin/env python3
"""Package original model/rig sources only; this archive is not an installable gameplay mod."""
from pathlib import Path
import hashlib,json,zipfile
ROOT=Path(__file__).resolve().parents[1]
entries={}
for p in (ROOT/'art/source').glob('*'):
    if p.is_file() and (p.suffix=='.bbmodel' or p.name.endswith('.parts.json') or p.name=='FactoryAtlas.png'):
        entries['Editable/'+p.name]=p.read_bytes()
for p in (ROOT/'assets/Common/Blocks/FoundryFloor').rglob('*'):
    if p.is_file():entries['Native/Blocks/FoundryFloor/'+p.relative_to(ROOT/'assets/Common/Blocks/FoundryFloor').as_posix()]=p.read_bytes()
entries['README.txt']=b'''FOUNDRY FLOOR - 0.1.0 AUTHORING KIT

26 craftable factory/bench designs, plus2 technical lift models.
Editable/ contains Blockbench projects, part specifications and the original atlas.
Native/ contains exported Hytale models, the texture atlas and71 animation clips:
ten original mechanism clips and61 held cab poses.

Open .bbmodel projects with Blockbench and its official Hytale Models plugin.
One Hytale block is32 model units. The shared texture is512x512.
The shutter has eleven independent slat bones; the lift cab uses Platform.
The separate lift controller and travel-gate sources support the native plugin.

This is a model/rig authoring archive. It is not an installable gameplay JAR.
Crafting, mechanical authority, collision and passenger transport require the
separate gameplay plugin, which is still undergoing native verification.
No tested playable release or public distribution license is declared here.
All included geometry and texture artwork is original project material.
'''
output=ROOT/'build/FoundryFloor-0.1.0-authoring-kit.zip';output.parent.mkdir(parents=True,exist_ok=True)
with zipfile.ZipFile(output,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=9) as archive:
    for name,content in sorted(entries.items()):
        info=zipfile.ZipInfo(name,(2026,10,21,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=0o100644<<16;archive.writestr(info,content)
with zipfile.ZipFile(output) as archive:
    assert archive.testzip() is None
    assert len([n for n in archive.namelist() if n.endswith('.bbmodel')])==28
    assert len([n for n in archive.namelist() if n.endswith('.blockymodel')])==28
    assert len([n for n in archive.namelist() if n.endswith('.blockyanim')])==71
    assert not any(n.startswith(('/', '../')) or '/audio/' in n or n.endswith(('.jar','.log','.bson')) for n in archive.namelist())
print(output.name,hashlib.sha256(output.read_bytes()).hexdigest(),len(entries),'original authoring files')
