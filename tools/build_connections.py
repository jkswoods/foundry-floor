#!/usr/bin/env python3
"""Original edge variants. Bits are local north, east, south, west (1,2,4,8)."""
import copy,json
import build_models as b

GRATES={'FF_Catwalk','FF_CatwalkLeft','FF_CatwalkRight','FF_CatwalkBridge','FF_CatwalkCorner','FF_DrainGrate'}

def connected_deck(y,mask,prefix):
    p=[]
    for bit,name,c,size,mat in [
        (8,'Left',(-15,y+2,0),(2,4,32),'dark'),(2,'Right',(15,y+2,0),(2,4,32),'dark'),
        (4,'Front',(0,y+2,15),(28,4,2),'yellow'),(1,'Back',(0,y+2,-15),(28,4,2),'yellow')]:
        if not mask&bit:p.append(b.part(prefix+name,c,size,mat))
    zlo=-16 if mask&1 else -14;zhi=16 if mask&4 else 14
    xlo=-16 if mask&8 else -14;xhi=16 if mask&2 else 14
    for i in range(8):
        lo=max(xlo,-15+i*4);hi=min(xhi,-13+i*4)
        p.append(b.part(prefix+'Grating'+str(i),((lo+hi)/2,y+3,(zlo+zhi)/2),(hi-lo,2,zhi-zlo),'grip'))
    for i,z in enumerate([-8,8]):p.append(b.part(prefix+'Bearer'+str(i),((xlo+xhi)/2,y+1,z),(xhi-xlo,2,2),'edge'))
    return p

def parts(m,mask):
    mid=m['id'];p=copy.deepcopy(m['parts'])
    if mid in GRATES:
        prefix='Drain' if mid=='FF_DrainGrate' else 'Deck'
        p=[x for x in p if not x['name'].startswith(prefix)]+connected_deck(28,mask,prefix)
        if mask&8:p=[x for x in p if not x['name'].startswith('Rail')]
        if mask&2:p=[x for x in p if not x['name'].startswith('RightRail')]
        if mask&1:p=[x for x in p if not x['name'].startswith('EndRail')]
    elif mid=='FF_HazardFloor':
        p=[x for x in p if x['name']!='SafetyStrip']
        zlo=-16 if mask&1 else -12;zhi=16 if mask&4 else 12
        for bit,c,s in [(1,(0,32.25,-14),(32,.5,4)),(2,(14,32.25,(zlo+zhi)/2),(4,.5,zhi-zlo)),(4,(0,32.25,14),(32,.5,4)),(8,(-14,32.25,(zlo+zhi)/2),(4,.5,zhi-zlo))]:
            if not mask&bit:p.append(b.part('Boundary'+str(bit),c,s,'hazard'))
    elif mid=='FF_CatwalkStairs':
        for side,bit in [(-1,8),(1,2)]:
            if mask&bit:p=[x for x in p if x['name']!='Handrail'+str(side) and not x['name'].startswith('StairPost'+str(side)) and not (x['name'].startswith('Stringer') and x['center'][0]*side>0)]
        lo=-16 if mask&8 else -14;hi=16 if mask&2 else 14
        for x in p:
            if x['name'].startswith(('Tread','Nosing')):x['center'][0]=(lo+hi)/2;x['size'][0]=hi-lo
    elif mid=='FF_WallVent':
        p=[b.part('VentBack',(0,16,-14),(31.5,31.5,2),'black')]
        for bit,c,s in [(1,(0,31,-10),(32,2,6)),(2,(15,16,-10),(2,28,6)),(4,(0,1,-10),(32,2,6)),(8,(-15,16,-10),(2,28,6))]:
            if not mask&bit:p.append(b.part('VentBorder'+str(bit),c,s,'steel'))
        lo=-16 if mask&8 else -14;hi=16 if mask&2 else 14
        for i in range(8):
            y=3 if i==0 and not mask&4 else 29 if i==7 and not mask&1 else 2+4*i
            p.append(b.part('Louvre'+str(i),((lo+hi)/2,y,-9),(hi-lo,2,3),'edge'))
    elif mid=='FF_CargoPallet':
        p=[b.part('Runner'+str(x),(x,2,0),(5,4,32),'dark') for x in [-10,10]]
        xlo=-16 if mask&8 else -14;xhi=16 if mask&2 else 14
        zlo=-16 if mask&1 else -14;zhi=16 if mask&4 else 14
        for i in range(4):
            lo=max(xlo,-15+8*i);hi=min(xhi,-9+8*i)
            p.append(b.part('CargoSlat'+str(i),((lo+hi)/2,5,(zlo+zhi)/2),(hi-lo,2,zhi-zlo),'copper'))
        for bit,c,s in [(1,(0,5,-15),(32,2,2)),(2,(15,5,(zlo+zhi)/2),(2,2,zhi-zlo)),(4,(0,5,15),(32,2,2)),(8,(-15,5,(zlo+zhi)/2),(2,2,zhi-zlo))]:
            if not mask&bit:p.append(b.part('PalletRim'+str(bit),c,s,'steel'))
    return p

def build(models):
    for m in models:
        if m['id'] not in GRATES|{'FF_HazardFloor','FF_CatwalkStairs','FF_WallVent','FF_CargoPallet'}:continue
        original=copy.deepcopy(m)
        path=b.SERVER/'Item/Items/FoundryFloor'/(m['id']+'.json');item=json.loads(path.read_text());defs={}
        for mask in range(16):
            variant=copy.deepcopy(original);variant['parts']=parts(original,mask)
            if mask:variant['id']=m['id']+'_Connect%02X'%mask
            b.export_model(variant)
            b.write(b.SERVER/'Item/Block/Hitboxes/FoundryFloor'/(variant['id']+'.json'),b.hitboxes(variant))
            defs['Connect%02X'%mask]=dict(CustomModel='Blocks/FoundryFloor/'+variant['id']+'.blockymodel',HitboxType=variant['id'])
        m['parts']=parts(original,0)
        item['BlockType']['State']={'Definitions':defs};b.write(path,item)
