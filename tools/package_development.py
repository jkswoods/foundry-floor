#!/usr/bin/env python3
"""Create a deterministic, mod-only development candidate from reviewed build artifacts."""
from pathlib import Path
import argparse
import hashlib
import json
import re
import zipfile

ROOT = Path(__file__).resolve().parents[1]
STAMP = (2026, 10, 21, 0, 0, 0)


def digest(data):
    return hashlib.sha256(data).hexdigest()


def read_archive(path):
    with zipfile.ZipFile(path) as archive:
        if archive.testzip() is not None:
            raise SystemExit('Archive integrity failed: ' + path.name)
        names = archive.namelist()
        if len(names) != len(set(names)) or any(n.startswith(('/', '../')) or '/..' in n for n in names):
            raise SystemExit('Unsafe archive members: ' + path.name)
        return {name: archive.read(name) for name in names}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source-commit', required=True, help='Full reviewed Git commit for the source and guide')
    parser.add_argument('--version', default='0.2.0-dev.1')
    parser.add_argument('--public-alpha',action='store_true',help='Package the player-approved public alpha profile')
    args = parser.parse_args()
    if not re.fullmatch(r'[0-9a-f]{40}', args.source_commit):
        parser.error('Use a full lowercase Git commit SHA')
    if not re.fullmatch(r'0\.2\.0-dev\.[1-9][0-9]*', args.version):
        parser.error('Use a 0.2.0-dev.N candidate version')

    runtime = ROOT / 'build/libs/FoundryFloor-0.2.0-dev.jar'
    guide = ROOT / 'build/FoundryFloor-0.2.0-player-guide-preview.zip'
    authoring = ROOT / 'build/FoundryFloor-0.2.0-authoring-kit.zip'
    for path in [runtime, guide, authoring]:
        if not path.is_file():
            raise SystemExit('Build the runtime, player guide and authoring kit first: missing ' + path.name)
    jar_members = read_archive(runtime)
    if any(not n.startswith(('Common/', 'Server/', 'dev/foundryfloor/')) and n not in {'manifest.json', 'SOURCE-AUDIO.json'} for n in jar_members):
        raise SystemExit('Unexpected runtime namespace')
    if any(n.endswith('.class') and not n.startswith('dev/foundryfloor/') for n in jar_members):
        raise SystemExit('Unexpected runtime class')
    manifest = json.loads(jar_members['manifest.json'])
    if manifest.get('Main') != 'dev.foundryfloor.hytale.FoundryPlugin' or manifest.get('ServerVersion') != '=0.7.3':
        raise SystemExit('Unexpected runtime compatibility or entry point')
    guide_members = read_archive(guide)
    if 'site/index.html' not in guide_members or 'DOCUMENTATION-NOTICES.md' not in guide_members:
        raise SystemExit('Missing player guide or required notices')
    if any(not n.startswith('site/') and n not in {'README.txt', 'DOCUMENTATION-NOTICES.md', 'SHA256SUMS.txt'} for n in guide_members):
        raise SystemExit('Unexpected guide namespace')
    model_members = read_archive(authoring)
    if len([n for n in model_members if n.endswith('.bbmodel')]) != 178 or len([n for n in model_members if n.endswith('.blockyanim')]) != 8:
        raise SystemExit('Incomplete model authoring kit')

    runtime_bytes = runtime.read_bytes()
    guide_bytes = guide.read_bytes()
    metadata = dict(package_version=args.version, source_commit=args.source_commit,
                    repository='https://github.com/jkswoods/foundry-floor',
                    hytale='0.7.3', java=25, manifest_version=manifest['Version'],
                    runtime_sha256=digest(runtime_bytes), guide_sha256=digest(guide_bytes),
                    profile='private development candidate',
                    status='Elevator deferred; remaining ordinary gameplay, audio and multiplayer acceptance pending')
    readme = f'''FOUNDRY FLOOR {args.version} - DEVELOPMENT CANDIDATE

Original modular factory architecture, a dedicated Foundry Workbench and
experimental mechanical controls for Hytale 0.7.3 / Java 25.

INSTALL
Back up the complete test world and player data with the matching JARs.
Close the world/server normally. Install only FoundryFloor-0.2.0-dev.jar in
UserData/Mods (or the server mods directory), then enable FoundryFloor in a
separate test world. Keep one version installed. Do not install these ZIPs
as game mods. The included player-guide archive contains installation,
all recipes, building layouts, controls, compatibility and backup guidance.

STATUS
The native registry resolves27 bench recipes plus the Foundry Workbench.
Elevators, guide extensions and call panels are deferred to a future release.
Existing elevator parts load as inert hidden compatibility assets and can be
owner-dismantled; no floor UI, passenger movement or empty-shaft reservation remains.
Remaining mechanical playtests, current native guide captures, human audio
listening and real multiplayer acceptance are pending.
This candidate is not the final tested development release.

CONTENTS
FoundryFloor-0.2.0-dev.jar is the gameplay plugin and original runtime assets.
FoundryFloor-0.2.0-player-guide-preview.zip is the built player guide.
BUILD.json identifies the reviewed source and exact artifact hashes.
SHA256SUMS.txt checks every other file in this archive.
The separate authoring-kit ZIP contains original editable models and rigs;
it is not an installable gameplay package.

SOURCE AND LICENSING
Source: https://github.com/jkswoods/foundry-floor (private access required).
Source commit: {args.source_commit}
No game client, server, stock assets or toolchains are bundled. No public
original-mod distribution license or production support is declared.
See THIRD_PARTY_NOTICES.md and the player-guide documentation notices.
'''.encode()
    if args.public_alpha:
        metadata.update(profile='public development alpha',status='Player QA accepted for crafting, building, mechanisms, normal save/reload, sounds and multiplayer; elevator deferred')
        readme=f'''FOUNDRY FLOOR {args.version} - PUBLIC ALPHA

Original factory architecture and mechanical controls for Hytale0.7.3 / Java25.
The collection includes the Foundry Workbench and27 factory recipes.

INSTALL
Back up your world and player files with matching JARs. Close the world/server,
then place FoundryFloor-0.2.0-dev.jar in UserData/Mods (or server mods directory).
Keep only one Foundry JAR installed and enable the mod for your world.
The other ZIP files are documentation/model sources, not installable game mods.

STATUS
Player testing covered crafting, placement, mechanisms, save/reload, sounds
and multiplayer. This is a prerelease, with no guarantee for arbitrary mod
combinations, other game versions or large production servers.
Elevators, guide extensions and call panels are deferred. Existing old parts
are inert and owner-dismantleable; no empty-shaft reservation remains.
The guide currently uses original model renders; native screenshots are pending.

CONTENTS
The JAR is the installable mod. The guide ZIP is an offline copy of the site.
BUILD.json records the source commit and exact runtime/guide hashes.
SHA256SUMS.txt verifies this archive's other members.

LINKS
Player guide: https://jkswoods.github.io/foundry-floor/
Source and issues: https://github.com/jkswoods/foundry-floor
Source commit: {args.source_commit}

No game binaries, stock assets or toolchains are bundled. No redistribution
license has been selected. Retained third-party notices are included.
'''.encode()
    members = {runtime.name: runtime_bytes, guide.name: guide_bytes,
               'README.txt': readme, 'BUILD.json': (json.dumps(metadata, indent=2) + '\n').encode(),
               'THIRD_PARTY_NOTICES.md': (ROOT / 'THIRD_PARTY_NOTICES.md').read_bytes()}
    members['SHA256SUMS.txt'] = ''.join(digest(data) + '  ' + name + '\n' for name, data in sorted(members.items())).encode()
    output = ROOT / 'build/releases' / args.version
    output.mkdir(parents=True, exist_ok=True)
    bundle = output / ('FoundryFloor-' + args.version + '-candidate.zip')
    with zipfile.ZipFile(bundle, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for name, data in sorted(members.items()):
            info = zipfile.ZipInfo(name, STAMP)
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            archive.writestr(info, data)
    written = read_archive(bundle)
    for line in written['SHA256SUMS.txt'].decode().splitlines():
        expected, name = line.split('  ', 1)
        if digest(written[name]) != expected:
            raise SystemExit('Bundle checksum mismatch')
    artifacts = {bundle.name: bundle.read_bytes(), runtime.name: runtime_bytes,
                 guide.name: guide_bytes, authoring.name: authoring.read_bytes()}
    for name, data in artifacts.items():
        if name != bundle.name:
            (output / name).write_bytes(data)
    (output / 'SHA256SUMS.txt').write_text(''.join(digest(data) + '  ' + name + '\n' for name, data in sorted(artifacts.items())))
    print(json.dumps(dict(version=args.version, bundle=bundle.name, sha256=digest(bundle.read_bytes()), files=len(written), source_commit=args.source_commit)))


if __name__ == '__main__':
    main()
