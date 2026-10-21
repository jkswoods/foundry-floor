#!/usr/bin/env python3
"""Keep inert, hidden old-world assets. No elevator recipe, controls, clock or travel code."""
from pathlib import Path
import copy,json,re
ROOT=Path(__file__).resolve().parents[1]
RETIRED={'FF_FreightLift','FF_LiftGuide','FF_CallPanel','FF_LiftAssembly'}
def retired_model(name):return name.startswith(('FF_Lift','FF_FreightLift','FF_CallPanel'))
def retire():
    for name in RETIRED:
        path=ROOT/'assets/Server/Item/Items/FoundryFloor'/(name+'.json')
        item=json.loads(path.read_text());item.pop('Recipe',None);item['Categories']=[]
        block=item['BlockType'];block.pop('Interactions',None);block.pop('InteractionHint',None);block.pop('AmbientSoundEventId',None)
        definitions=block.get('State',{}).get('Definitions',{})
        for state in list(definitions):
            if 'Move' in state:definitions[state]=copy.deepcopy(definitions[state.replace('Move','Step')])
            definitions[state].pop('AmbientSoundEventId',None)
        path.write_text(json.dumps(item,indent=2)+'\n')
    for p in (ROOT/'assets/Common/Blocks/FoundryFloor/Animations').glob('*.blockyanim'):
        if retired_model(p.stem) and not re.fullmatch(r'FF_Lift(H[0-9]+)?Pose[0-9]+',p.stem):p.unlink()
    # Compatibility collision retains the saved deck, not the retired guide/cab enclosure.
    for p in (ROOT/'assets/Server/Item/Block/Hitboxes/FoundryFloor').glob('FF_Lift*.json'):
        if not re.fullmatch(r'FF_Lift(H[0-9]+)?(Step|Dock)[0-9]+',p.stem):continue
        value=json.loads(p.read_text());deck=value['Boxes'][-1]
        assert abs(deck['Max']['Y']-deck['Min']['Y']-.125)<1e-7,p
        p.write_text(json.dumps({'Boxes':[deck]},indent=2)+'\n')
    lang=ROOT/'assets/Server/Languages/en-US/server.lang'
    lines=[l for l in lang.read_text().splitlines() if not any('foundryFloor.'+id+'.' in l for id in RETIRED)]
    for id in sorted(RETIRED):
        lines.extend(['foundryFloor.'+id+'.name = Retired elevator part','foundryFloor.'+id+'.description = Elevator content is deferred. Existing parts can be dismantled; new placement is disabled.'])
    lang.write_text('\n'.join(lines)+'\n')
    p=ROOT/'assets/Common/UI/Custom/FoundryFloor/LiftControls.ui'
    if p.exists():p.unlink()
    for folder in ['art/source','art/previews','docs/assets/models']:
        for p in (ROOT/folder).glob('*'):
            if p.is_file() and retired_model(p.stem):p.unlink()
if __name__=='__main__':retire()
