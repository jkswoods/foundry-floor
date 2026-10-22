"""Door rigidity and surface alignment regressions using the shipped model files."""
from pathlib import Path
import json, math, unittest

ROOT = Path(__file__).resolve().parents[3]
MODELS = ROOT / 'assets/Common/Blocks/FoundryFloor'

def rotate(q, p):
    x, y, z, w = (q[k] for k in 'xyzw')
    a, b, c = p
    return [(1-2*y*y-2*z*z)*a+(2*x*y-2*z*w)*b+(2*x*z+2*y*w)*c,
            (2*x*y+2*z*w)*a+(1-2*x*x-2*z*z)*b+(2*y*z-2*x*w)*c,
            (2*x*z-2*y*w)*a+(2*y*z+2*x*w)*b+(1-2*x*x-2*y*y)*c]

def sample(keys, amount):
    a, b = keys[0]['delta'], keys[-1]['delta']
    f = amount * amount * (3-2*amount)
    q = {k:a[k]*(1-f)+b[k]*f for k in a}
    length = math.sqrt(sum(v*v for v in q.values()))
    return {k:v/length for k,v in q.items()}

class ArchitectureTests(unittest.TestCase):
    def test_translucent_pane_receives_direct_hinge_motion(self):
        model = json.loads((MODELS/'FF_PersonnelDoor.blockymodel').read_text())
        roots = {n['name']:n for n in model['nodes']}
        # Translucent geometry needs its own animated shaped node, rather than
        # depending on the opaque renderer's ancestor transform.
        pane, hinge = roots['WiredWindow'], roots['Door']
        self.assertEqual(pane['shape']['type'], 'box')
        self.assertTrue(all(f['transparent'] for f in pane['shape']['textureLayout'].values()))
        self.assertEqual(pane['position'], hinge['position'])
        for action in ['Open','Close']:
            clip = json.loads((MODELS/f'Animations/FF_PersonnelDoor_{action}.blockyanim').read_text())
            self.assertTrue(clip['holdLastKeyframe'])
            for i in range(21):
                t = i/20
                pane_q = sample(clip['nodeAnimations']['WiredWindow']['orientation'], t)
                door_q = sample(clip['nodeAnimations']['Door']['orientation'], t)
                actual = rotate(pane_q, [pane['shape']['offset'][k] for k in 'xyz'])
                # The pane's centre is 12 units right and 12 units above the hinge.
                expected = rotate(door_q, [12,12,0])
                for a,b in zip(actual,expected):self.assertAlmostEqual(a,b,places=7)
                if action=='Open' and i==20:self.assertGreater(abs(actual[2]),11)

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
