"""Door rigidity and surface alignment regressions using the shipped model files."""
from pathlib import Path
import json, math, unittest

ROOT = Path(__file__).resolve().parents[3]
MODELS = ROOT / 'assets/Common/Blocks/FoundryFloor'

class ArchitectureTests(unittest.TestCase):
    def test_translucent_pane_is_baked_into_every_whole_door_pose(self):
        item=json.loads((ROOT/'assets/Server/Item/Items/FoundryFloor/FF_PersonnelDoor.json').read_text())
        definitions=item['BlockType']['State']['Definitions']
        for step in range(17):
            state=definitions[f'DoorPose{step:02d}']
            self.assertNotIn('CustomModelAnimation',state)
            model=json.loads((ROOT/'assets/Common'/state['CustomModel']).read_text())
            roots={n['name']:n for n in model['nodes']};pane=roots['WiredWindow']
            self.assertEqual(pane['shape']['offset'],dict(x=0,y=0,z=0))
            self.assertTrue(all(f['transparent'] for f in pane['shape']['textureLayout'].values()))
            self.assertEqual(pane['orientation'],roots['DoorLower']['orientation'])
            # Pane centre must follow the physical hinge, independently of any
            # animation evaluator (which player QA showed the alpha pass ignores).
            a=math.radians(-90*step/16)
            expected=[-12+12*math.cos(a),43,-12*math.sin(a)]
            for k,value in zip('xyz',expected):self.assertAlmostEqual(pane['position'][k],value,places=7)
            for n in roots.values():self.assertFalse(n.get('children'))
        for terminal,step in [('Open',16),('Closed',0)]:
            self.assertEqual(definitions[terminal]['CustomModel'],definitions[f'DoorPose{step:02d}']['CustomModel'])
            self.assertNotIn('CustomModelAnimation',definitions[terminal])

    def test_open_decks_meet_the_native_placement_plane(self):
        for stem in ['FF_Catwalk','FF_CatwalkLeft','FF_CatwalkRight','FF_CatwalkBridge','FF_CatwalkCorner','FF_DrainGrate']:
            for mask in range(16):
                name=stem+(f'_Connect{mask:02X}' if mask else '')
                model=json.loads((MODELS/f'{name}.blockymodel').read_text())
                grates=[n for n in model['nodes'] if 'Grating' in n['name']]
                self.assertTrue(grates,name)
                for n in grates:
                    self.assertEqual(n['position']['y']+n['shape']['settings']['size']['y']/2,32,name)

    def test_editable_glass_has_unique_bone_and_shape_ids(self):
        model=json.loads((ROOT/'art/source/FF_PersonnelDoor.bbmodel').read_text())
        ids=[e['uuid'] for e in model['elements']]+[g['uuid'] for g in model['outliner'] if isinstance(g,dict)]
        self.assertEqual(len(ids),len(set(ids)))

if __name__ == '__main__':unittest.main()
