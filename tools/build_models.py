#!/usr/bin/env python3
"""Original modular industrial geometry; exports Hytale, Blockbench and offline review data.

No game process, installed mod directory or world is opened by this tool.
One Hytale block is 32 model units. All part names, pivots and UVs are retained.
"""
from pathlib import Path
import base64, copy, hashlib, json, math, random, uuid
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'assets'
COMMON = ASSETS / 'Common'
SERVER = ASSETS / 'Server'
SOURCE = ROOT / 'art/source'
PALETTE = {
    'steel': '#5b6b70', 'edge': '#899a9c', 'dark': '#29363b', 'black': '#172329',
    'yellow': '#d3a43e', 'hazard': '#cca13e', 'copper': '#ab7150', 'concrete': '#7e8780',
    'light': '#e4dcb0', 'green': '#72b9a2', 'red': '#b45c47', 'grip': '#596666',
}
MATS = list(PALETTE)
MODELS = []

def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n')

def v(a): return dict(zip('xyz', a))
def q(axis=(0, 1, 0), degrees=0):
    s = math.sin(math.radians(degrees) / 2)
    return dict(zip('xyzw', [i * s for i in axis] + [math.cos(math.radians(degrees) / 2)]))
def uid(name): return str(uuid.uuid5(uuid.NAMESPACE_URL, 'foundry-floor/' + name))
def part(name, c, size, mat='steel', group=None):
    return dict(name=name, center=list(c), size=list(size), material=mat, group=group)

def boltset(cx, y, cz, wx=12, wz=12, prefix='Bolt'):
    return [part(prefix + str(i), (cx+x, y, cz+z), (2, 1, 2), 'edge')
            for i, (x,z) in enumerate([(-wx,-wz),(wx,-wz),(-wx,wz),(wx,wz)])]

def deck(y=0, w=32, d=32, grated=True, prefix='Deck', group=None):
    p = [part(prefix+'Left', (-w/2+1,y+2,0),(2,4,d),'dark',group),
         part(prefix+'Right',(w/2-1,y+2,0),(2,4,d),'dark',group),
         part(prefix+'Front',(0,y+2,d/2-1),(w-4,4,2),'yellow',group),
         part(prefix+'Back',(0,y+2,-d/2+1),(w-4,4,2),'yellow',group)]
    if grated:
        for i in range(int((w-4)//4)):
            p.append(part(prefix+'Grating'+str(i),(-w/2+4+i*4,y+3,0),(2,2,d-4),'grip',group))
        for i,z in enumerate([-d/4,d/4]):
            p.append(part(prefix+'Bearer'+str(i),(0,y+1,z),(w-4,2,2),'edge',group))
    else: p.append(part(prefix+'Plate',(0,y+2,0),(w-4,4,d-4),'steel',group))
    return p

def rail(side='left', y=4, prefix='Rail', group=None):
    x = -14 if side == 'left' else 14
    p = [part(prefix+'Post'+str(i),(x,y+14,z),(2,28,2),'yellow',group) for i,z in enumerate([-14,14])]
    p += [part(prefix+'Top',(x,y+28,0),(2,2,30),'yellow',group),
          part(prefix+'Mid',(x,y+16,0),(2,2,28),'edge',group),
          part(prefix+'Toe',(x,y+4,0),(2,6,28),'dark',group)]
    for i,z in enumerate([-14,14]):p.append(part(prefix+'Foot'+str(i),(x,y+.5,z),(4,1,4),'edge',group))
    return p

def add(id, title, category, parts, description, cost=4, quantity=1, groups=None, motion=None, collision=None):
    MODELS.append(dict(id='FF_'+id, title=title, category=category, parts=parts,
                       description=description, cost=cost, quantity=quantity,
                       groups=groups or {}, motion=motion, collision=collision))

def models():
    plate=[part('Foundation',(0,14,0),(32,28,32),'dark'),part('SteelTread',(0,30,0),(32,4,32),'grip')]+boltset(0,32.5,0)
    add('FloorPlate','Riveted floor plate','Floors',plate,'A full block of treaded steel with corner fasteners; tiles on a 1 × 1 grid.',4,4)
    add('HazardFloor','Safety edge floor','Floors',[part('Foundation',(0,14,0),(32,28,32),'dark'),part('Deck',(0,30,0),(32,4,32),'grip'),part('SafetyStrip',(0,32.25,12),(32,.5,8),'hazard')]+boltset(0,32.5,0,12,9),'A directional safety stripe marks loading bays and changes in level.',4,4)
    add('ConcreteFloor','Poured factory concrete','Floors',[part('Concrete',(0,15,0),(32,30,32),'concrete'),part('TopPanel',(0,31,0),(31,2,31),'concrete')],'Quiet neutral infill for large factory floors.',2,4)
    drain=[part('ConcreteBed',(0,12,0),(32,24,32),'concrete'),part('DrainRecess',(0,26,0),(28,4,28),'black')]
    drain+=deck(28,32,32,True,'Drain')
    add('DrainGrate','Recessed drain grate','Floors',drain,'An open grate set flush into a full-height concrete floor.',3,2)
    add('Catwalk','Open grated catwalk','Catwalks',deck(),'A thin 1 × 1 deck with real gaps and an underside support frame.',3,2)
    add('CatwalkLeft','Catwalk · left rail','Catwalks',deck()+rail(),'Walkway with left handrail and toe board.',5,2)
    add('CatwalkRight','Catwalk · right rail','Catwalks',deck()+rail('right'),'Walkway with right handrail and toe board.',5,2)
    add('CatwalkBridge','Catwalk · both rails','Catwalks',deck()+rail()+rail('right',prefix='RightRail'),'A protected straight bridge segment.',7,2)
    corner=deck()+rail()
    # Separate generation keeps corner rails deterministic.
    end=rail('left',prefix='EndRail')
    for p in end:
        p['center'][0],p['center'][2]=-p['center'][2],p['center'][0]
        p['size'][0],p['size'][2]=p['size'][2],p['size'][0]
    add('CatwalkCorner','Catwalk · outer corner','Catwalks',corner+end,'Outside railing for a right-angle landing.',6,2)
    stairs=[]
    for i in range(8):
        z=14-i*4;y=i*4
        stairs += [part('Tread'+str(i),(0,y+2,z),(28,4,4),'grip'),part('Nosing'+str(i),(0,y+4.25,z+1),(28,.5,2),'yellow')]
        for side in [-1,1]:stairs.append(part('Stringer'+str(i)+str(side),(side*15,y/2+1,z),(2,y+2,4),'dark'))
    for side in [-1,1]:
        for i in [0,7]:stairs.append(part('StairPost'+str(side)+str(i),(side*15,18+i*4,14-i*4),(2,28,2),'yellow'))
        handrail=part('Handrail'+str(side),(side*15,46,0),(2,2,44),'yellow')
        handrail['rotation']=dict(axis=[1,0,0],angle=45)
        stairs.append(handrail)
    add('CatwalkStairs','Grated stair flight','Catwalks',stairs,'Eight treads rise exactly one block over one block of run. Repeat for taller landings.',8,1)
    add('SafetyRail','Safety railing','Structure',rail(y=0),'Independent handrail with a kick plate; rotates through four directions.',3,2)
    column=[part('ColumnFoot',(0,2,0),(24,4,24),'edge'),part('ColumnWeb',(0,18,0),(5,28,12),'dark'),part('FrontFlange',(0,18,7),(16,28,3),'steel'),part('BackFlange',(0,18,-7),(16,28,3),'steel'),part('ColumnCap',(0,31,0),(24,2,24),'edge')]+boltset(0,4.5,0,9,9)
    add('SteelColumn','Bolted I-section column','Structure',column,'Stackable one-block structural column with broad bolted feet.',4,2)
    beam=[part('Web',(0,16,0),(4,22,32),'dark'),part('TopFlange',(0,28,0),(20,4,32),'steel'),part('BottomFlange',(0,4,0),(20,4,32),'steel')]
    add('SteelBeam','I-section crossbeam','Structure',beam,'A one-block beam module for overhead frames and gantries.',4,2)
    # 3-block-wide roller shutter. Slats independently telescope into the hood.
    shutter=[part('GuideLeft',(-46,44,0),(4,88,8),'dark'),part('GuideRight',(46,44,0),(4,88,8),'dark'),part('RollerHood',(0,92,0),(96,8,14),'steel'),part('HoodLip',(0,87,7),(96,2,2),'yellow')]
    groups={};targets={}
    for i in range(11):
        y=4+i*8;g='Slat'+str(i);groups[g]=[0,y,0];targets[g]=dict(position=[0,85+i*.3-y,0])
        shutter += [part(g+'Panel',(0,y,0),(88,8,3),'steel',g),part(g+'Rib',(0,y-3,2),(88,1,2),'edge',g)]
    shutter += [part('BottomSafetyBar',(0,1,3),(88,2,3),'hazard','Slat0'),part('PullHandle',(0,7,4),(12,2,3),'dark','Slat0'),part('Motor',(-40,92,-10),(15,10,10),'dark'),part('ButtonBox',(49,32,5),(6,12,5),'yellow'),part('Button',(49,34,8),(3,3,1),'green')]
    add('RollerShutter','Rolling loading-bay shutter','Interactive',shutter,'A 3 × 3 opening. Eleven slats retract into the overhead hood; guides and motor stay fixed.',18,1,groups,dict(kind='shutter',duration=2.5,targets=targets))
    # Gate assembly: pivot at the left jamb. Moving pieces all share the hinge.
    gate=[part('HingePost',(-14,16,0),(4,32,4),'dark'),part('CatchPost',(14,16,0),(4,32,4),'dark')]
    gate += [part('GateTop',(0,28,0),(24,2,2),'yellow','Gate'),part('GateMid',(0,16,0),(24,2,2),'yellow','Gate'),part('GateToe',(0,4,0),(24,6,2),'hazard','Gate'),part('GateLatch',(11,22,2),(3,5,3),'edge','Gate')]
    for x in [-10,10]:gate.append(part('GateUpright'+str(x),(x,16,0),(2,26,2),'yellow','Gate'))
    add('SafetyGate','Hinged catwalk safety gate','Interactive',gate,'A one-block safety gate with an offset hinge and matching toe board.',5,1,{'Gate':[-12,16,0]},dict(kind='hinge',duration=.8,targets={'Gate':dict(axis=[0,1,0],angle=-95)}))
    door=[part('DoorFrameL',(-15,32,0),(2,64,6),'edge'),part('DoorFrameR',(15,32,0),(2,64,6),'edge'),part('DoorLintel',(0,63,0),(28,2,6),'yellow'),part('DoorPanel',(0,31,0),(28,62,3),'steel','Door'),part('LowerKickPlate',(0,10,2),(26,16,1),'dark','Door'),part('WindowRecess',(0,43,2),(18,18,1),'dark','Door'),part('WiredWindow',(0,43,3),(14,14,1),'green','Door'),part('DoorHandle',(10,28,3),(2,7,2),'yellow','Door')]
    add('PersonnelDoor','Factory personnel door','Interactive',door,'A 1 × 2 steel door with inspection window and kick plate.',8,1,{'Door':[-14,31,0]},dict(kind='hinge',duration=.8,targets={'Door':dict(axis=[0,1,0],angle=-95)}))
    hatch=deck(grated=False)
    for p in hatch:p['group']='Hatch'
    hatch += [part('HatchHandle',(0,5,8),(8,2,2),'edge','Hatch'),part('HatchHingeL',(-10,3,-15),(5,4,4),'copper'),part('HatchHingeR',(10,3,-15),(5,4,4),'copper')]
    add('ServiceHatch','Floor service hatch','Interactive',hatch,'A one-block hinged maintenance cover. The whole deck pivots from its rear edge.',5,1,{'Hatch':[0,3,-14]},dict(kind='hatch',duration=.8,targets={'Hatch':dict(axis=[1,0,0],angle=-95)}))
    # Elevator visual assembly: three-block cab, six-block shaft, three-block travel.
    lift=[]
    for x in [-46,46]:
        for y in [48,144]:lift += [part('Guide'+str(x)+str(y),(x,y,-40),(4,96,6),'edge'),part('GuideBack'+str(x)+str(y),(x-math.copysign(2.5,x),y,-44),(9,96,2),'dark')]
    lift += [part('HeadBeam',(0,188,-40),(96,8,12),'dark'),part('MotorHousing',(0,196,-38),(28,12,16),'steel'),part('Sheave',(0,193,-28),(16,16,4),'copper')]
    lift += deck(0,88,80,True,'CabDeck','Platform')
    for x in [-42,42]:
        for z in [-36,36]:lift.append(part('CabPost'+str(x)+str(z),(x,40,z),(3,72,3),'yellow','Platform'))
        lift += [part('CabSideTop'+str(x),(x,74,0),(3,3,76),'yellow','Platform'),part('CabSideMid'+str(x),(x,35,0),(2,2,72),'edge','Platform'),part('CabToe'+str(x),(x,9,0),(2,8,72),'dark','Platform')]
    lift += [part('CabBackTop',(0,74,-36),(80,3,3),'yellow','Platform'),part('CabBackMid',(0,35,-36),(80,2,2),'edge','Platform'),part('CabBackToe',(0,9,-36),(80,8,2),'hazard','Platform')]
    for i in range(9):lift.append(part('RearMesh'+str(i),(-36+i*9,46,-36),(1,50,1),'dark','Platform'))
    lift += [part('CarControl',(39,40,35),(5,14,4),'dark','Platform'),part('CarButton',(39,43,38),(2,3,1),'green','Platform')]
    add('FreightLift','Caged freight elevator','Interactive',lift,'Place the controller in front of a clear 3 × 3 × 7 shaft. The caged platform travels three blocks between two docks.',28,1,{'Platform':[0,0,0]},dict(kind='lift',duration=4,targets={'Platform':dict(position=[0,96,0])}))
    liftguide=[part('Rail',(0,32,0),(4,64,5),'edge'),part('RailBack',(0,32,-4),(12,64,3),'dark')]
    for y in [8,56]:liftguide += [part('Bracket'+str(y),(0,y,-8),(20,4,8),'steel'),part('Bolt'+str(y),(0,y,3),(3,3,1),'copper')]
    add('LiftGuide','Lift guide extension','Structure',liftguide,'Two-block guide segment for elevator shaft construction.',4,2)
    call=[part('Backplate',(0,16,-13),(12,22,4),'steel'),part('Face',(0,16,-10),(10,18,2),'yellow'),part('UpButton',(0,20,-8),(4,4,2),'green'),part('DownButton',(0,12,-8),(4,4,2),'red'),part('Conduit',(0,4,-14),(3,8,3),'dark')]
    add('CallPanel','Lift call panel','Fixtures',call,'Use to link to the nearest owned freight lift within twelve blocks; calls the cab to this floor.',3,1)
    pipe=[part('Duct',(0,16,0),(12,12,32),'copper')]
    for z in [-14,14]:pipe += [part('Flange'+str(z),(0,16,z),(18,18,4),'dark'),part('Band'+str(z),(0,16,z),(20,2,5),'edge')]
    add('PipeStraight','Flanged utility duct','Fixtures',pipe,'One-block utility duct with matched 12-unit ports.',3,2)
    elbow=[part('DuctRear',(0,16,-8),(12,12,16),'copper'),part('DuctSide',(8,16,0),(16,12,12),'copper'),part('RearFlange',(0,16,-14),(18,18,4),'dark'),part('SideFlange',(14,16,0),(4,18,18),'dark')]
    add('PipeElbow','Utility duct elbow','Fixtures',elbow,'A 90-degree return with the same port centers as the straight duct.',3,2)
    vent=[part('VentBody',(0,16,-11),(28,28,10),'steel'),part('DarkRecess',(0,16,-5),(24,24,2),'black')]
    for i in range(6):vent.append(part('Louvre'+str(i),(0,6+i*4,-3),(24,2,3),'edge'))
    add('WallVent','Louvred wall vent','Fixtures',vent,'Recessed vent cover with separate louvres.',4,2)
    lamp=[part('LampBracket',(0,18,-13),(10,18,6),'dark'),part('LampHousing',(0,20,-7),(18,12,8),'steel'),part('Diffuser',(0,20,-2),(14,8,2),'light')]
    for x in [-6,0,6]:lamp.append(part('Guard'+str(x),(x,20,0),(1,10,2),'dark'))
    add('WallLamp','Guarded factory lamp','Fixtures',lamp,'Caged wall light with native light on/off states.',3,2,motion=dict(kind='lamp',duration=.1,targets={}))
    pallet=[part('RunnerL',(-10,2,0),(5,4,30),'dark'),part('RunnerR',(10,2,0),(5,4,30),'dark')]
    for i in range(5):pallet.append(part('CargoSlat'+str(i),(-12+i*6,5,0),(5,2,32),'copper'))
    add('CargoPallet','Steel cargo pallet','Fixtures',pallet,'A low loading-bay prop with visible forklift pockets.',3,2)
    bench=[part('BenchTop',(0,29,0),(56,6,28),'grip'),part('FrontSafetyLip',(0,27,14),(56,3,2),'hazard'),part('LowerShelf',(0,6,0),(50,3,24),'dark')]
    for x in [-23,23]:
        for z in [-10,10]:bench += [part('BenchLeg'+str(x)+str(z),(x,16,z),(4,26,4),'steel'),part('BenchFoot'+str(x)+str(z),(x,2,z),(7,4,7),'dark')]
    bench += [part('DrawerBody',(-10,20,0),(26,12,22),'steel'),part('DrawerFace',(-10,20,12),(24,10,2),'dark'),part('DrawerHandle',(-10,22,14),(12,2,3),'yellow'),part('ViceBase',(17,34,0),(12,4,14),'edge'),part('ViceJawL',(13,39,0),(4,8,12),'steel'),part('ViceJawR',(21,39,0),(4,8,12),'steel'),part('ViceScrew',(17,36,9),(14,3,3),'copper'),part('ToolBoard',(0,46,-12),(50,26,3),'dark'),part('BoardTop',(0,59,-12),(54,2,4),'yellow')]
    for i,x in enumerate([-18,-8,2]):bench += [part('HangingTool'+str(i),(x,47,-9),(3,14,2),'edge'),part('ToolHead'+str(i),(x,53,-8),(8,3,3),'copper')]
    add('FoundryBench','Foundry Workbench','Crafting',bench,'Craft the factory block collection in dedicated Floors, Walkways, Structure, Fixtures and Mechanisms tabs.',8,1)

def texture():
    im=Image.new('RGBA',(512,512));d=ImageDraw.Draw(im);rng=random.Random(4107)
    for i,(name,color) in enumerate(PALETTE.items()):
        x=(i%4)*128;y=(i//4)*128
        rgb=tuple(int(color[j:j+2],16) for j in [1,3,5])
        for yy in range(128):
            for xx in range(128):
                n=rng.choice([-5,-3,-1,0,0,1,3,5]); c=tuple(max(0,min(255,k+n)) for k in rgb)
                if name=='hazard' and ((xx+yy)//6)%2==0:c=(36,46,48)
                if name=='grip' and xx%12 in [2,3] and yy%8 in [2,3,4]:c=(118,129,124)
                im.putpixel((x+xx,y+yy),(*c,255))
        if name not in ['light','green','red','hazard','concrete','grip']:
            for k in range(18):
                xx=x+rng.randrange(8,110);yy=y+rng.randrange(4,124)
                d.line((xx,yy,min(x+127,xx+rng.randrange(3,12)),yy),fill=(*[min(255,v+14) for v in rgb],255))
        if name=='concrete':
            for yy in range(0,128,32):d.line((x, y+yy, x+127,y+yy), fill=(86,97,92,255))
    for path in [COMMON/'Blocks/FoundryFloor/FactoryAtlas.png',SOURCE/'FactoryAtlas.png']:
        path.parent.mkdir(parents=True,exist_ok=True);im.save(path)

def node(name, position, shape=None, children=None):
    a=dict(id=uid(name),name=name,position=v(position),orientation=q())
    if shape:a['shape']=shape
    if children is not None:a['children']=children
    return a

def export_model(m):
    nodes=[];groups={};elements=[];bbgroups={}
    for g,pivot in m['groups'].items():
        group=node(g,pivot,children=[]);nodes.append(group);groups[g]=group
        bbgroups[g]=dict(name=g,origin=pivot,rotation=[0,0,0],uuid=uid(m['id']+'/'+g),children=[])
    for p in m['parts']:
        c=p['center'];s=p['size'];mi=MATS.index(p['material']);ox=mi%4*128;oy=mi//4*128
        shape=dict(type='box',offset=v([0,0,0]),stretch=v([1,1,1]),settings=dict(size=v(s),isStaticBox=True),unwrapMode='custom',visible=True,doubleSided=False,shadingMode='standard',textureLayout={f:dict(offset=dict(x=ox,y=oy),mirror=dict(x=False,y=False),angle=0) for f in ['front','back','left','right','top','bottom']})
        parent=groups.get(p['group']);local=[c[i]-m['groups'][p['group']][i] for i in range(3)] if parent else c
        n=node(p['name'],local,shape)
        if p.get('rotation'):n['orientation']=q(p['rotation']['axis'],p['rotation']['angle'])
        (parent['children'] if parent else nodes).append(n)
        faces={}
        for f in ['north','south','west','east','up','down']:
            w,h=(s[0],s[2]) if f in ['up','down'] else (s[2],s[1]) if f in ['west','east'] else (s[0],s[1])
            faces[f]=dict(uv=[ox,oy,ox+w,oy+h],texture=0)
        e=dict(name=p['name'],type='cube',uuid=uid(m['id']+'/'+p['name']),**{'from':[c[i]-s[i]/2 for i in range(3)],'to':[c[i]+s[i]/2 for i in range(3)]},origin=m['groups'].get(p['group'],c),rotation=[0,0,0],box_uv=False,faces=faces)
        elements.append(e)
        if p.get('rotation'):e['rotation']=[c*p['rotation']['angle'] for c in p['rotation']['axis']]
        if parent:bbgroups[p['group']]['children'].append(e['uuid'])
    runtime=dict(lod='auto',nodes=nodes)
    write(COMMON/'Blocks/FoundryFloor'/(m['id']+'.blockymodel'),runtime)
    outliner=[g for g in bbgroups.values()]+[e['uuid'] for e,p in zip(elements,m['parts']) if not p['group']]
    bb=dict(meta=dict(format_version='5.0',model_format='hytale_prop',box_uv=False),name=m['title'],resolution=dict(width=512,height=512),elements=elements,outliner=outliner,textures=[dict(name='FactoryAtlas.png',id='0',uuid=uid('atlas'),source='data:image/png;base64,'+base64.b64encode((SOURCE/'FactoryAtlas.png').read_bytes()).decode(),width=512,height=512,uv_width=512,uv_height=512)],animations=[])
    if m['motion'] and m['motion']['targets']:
        for action,reverse in [('Open',False),('Close',True)]:
            if m['motion']['kind']=='lift':action='Down' if reverse else 'Up'
            anim=dict(formatVersion=1,duration=round(m['motion']['duration']*60),holdLastKeyframe=True,nodeAnimations={})
            animators={}
            for g,t in m['motion']['targets'].items():
                channel='position' if 'position' in t else 'orientation'
                a=v([0,0,0]) if channel=='position' else q()
                b=v(t['position']) if channel=='position' else q(t['axis'],t['angle'])
                if reverse:a,b=b,a
                anim['nodeAnimations'][g]={channel:[dict(time=0,delta=a,interpolationType='smooth'),dict(time=anim['duration'],delta=b,interpolationType='smooth')]}
                bba=[0,0,0];bbb=t.get('position',[t.get('angle',0)*c for c in t.get('axis',[0,1,0])])
                if reverse:bba,bbb=bbb,bba
                kfs=[dict(channel='position' if channel=='position' else 'rotation',time=time,interpolation='linear',data_points=[dict(zip('xyz',map(str,delta)))],uuid=uid(m['id']+g+action+str(time))) for time,delta in [(0,bba),(m['motion']['duration'],bbb)]]
                animators[uid(m['id']+'/'+g)]=dict(name=g,type='bone',keyframes=kfs)
            write(COMMON/'Blocks/FoundryFloor/Animations'/(m['id']+'_'+action+'.blockyanim'),anim)
            bb['animations'].append(dict(uuid=uid(m['id']+'/'+action),name=action,loop='hold',override=False,length=m['motion']['duration'],snapping=60,animators=animators))
    write(SOURCE/(m['id']+'.bbmodel'),bb)
    write(SOURCE/(m['id']+'.parts.json'),m)

def transform(p,m,amount):
    c=list(p['center']);g=p['group'];t=(m.get('motion') or {}).get('targets',{}).get(g)
    if not t:
        size=p['size']
        if p.get('rotation'):
            a=math.radians(p['rotation']['angle']);co=abs(math.cos(a));si=abs(math.sin(a));sx,sy,sz=size
            size=[sx,co*sy+si*sz,si*sy+co*sz]
        return c,size
    pivot=m['groups'][g]
    if 'position' in t:return [c[i]+amount*t['position'][i] for i in range(3)],p['size']
    a=math.radians(t['angle']*amount);co=math.cos(a);si=math.sin(a);x,y,z=[c[i]-pivot[i] for i in range(3)]
    if t['axis'][1]:rot=[co*x+si*z,y,-si*x+co*z]
    else:rot=[x,co*y-si*z,si*y+co*z]
    # Axis-aligned envelope of a rotated box; used only for collision generation.
    sx,sy,sz=p['size']
    size=[abs(co)*sx+abs(si)*sz,sy,abs(si)*sx+abs(co)*sz] if t['axis'][1] else [sx,abs(co)*sy+abs(si)*sz,abs(si)*sy+abs(co)*sz]
    return [rot[i]+pivot[i] for i in range(3)],size

def hitboxes(m,opened=False):
    # Grate tops deliberately collide as decks: tiny visual gaps do not trap feet.
    if m['id'] in ['FF_FloorPlate','FF_HazardFloor','FF_ConcreteFloor']:
        ps=[part('Floor',(0,16,0),(32,32,32))]
    elif m['id'].startswith('FF_Catwalk') and m['id']!='FF_CatwalkStairs':
        ps=[part('WalkableDeck',(0,2,0),(32,4,32))]+[p for p in m['parts'] if 'Rail' in p['name']]
    elif m['id']=='FF_DrainGrate':ps=[part('Floor',(0,16,0),(32,32,32))]
    else:ps=m['parts']
    boxes=[]
    for p in ps:
        c,s=transform(p,m,1 if opened else 0)
        lo=[(c[i]-s[i]/2)/32+(0 if i==1 else .5) for i in range(3)]
        hi=[(c[i]+s[i]/2)/32+(0 if i==1 else .5) for i in range(3)]
        boxes.append(dict(Min=dict(zip('XYZ',lo)),Max=dict(zip('XYZ',hi))))
    return dict(Boxes=boxes)

def export_item(m):
    mid=m['id'];b=dict(Material='Solid',DrawType='Model',Opacity='Transparent',CustomModel='Blocks/FoundryFloor/'+mid+'.blockymodel',CustomModelTexture=[dict(Texture='Blocks/FoundryFloor/FactoryAtlas.png',Weight=1)],VariantRotation='NESW',HitboxType=mid,BlockSoundSetId='FF_Metal',PhysicalMaterialId='Metal',BlockParticleSetId='Metal',ParticleColor='#657578',Gathering=dict(Breaking=dict(GatherType='Benches')))
    write(SERVER/'Item/Block/Hitboxes/FoundryFloor'/(mid+'.json'),hitboxes(m))
    motion=m['motion'];kind=(motion or {}).get('kind')
    if kind in ['shutter','hinge','hatch']:
        b['InteractionHint']='server.foundryFloor.toggle'
        b['Interactions']=dict(Use='FF_Toggle')
        cue='shutter_travel' if kind=='shutter' else 'mechanical_latch'
        b['State']=dict(Definitions=dict(Open=dict(HitboxType=mid+'_Open',CustomModelAnimation='Blocks/FoundryFloor/Animations/'+mid+'_Open.blockyanim',InteractionSoundEventId='FF_SFX_'+cue,SoundOcclusionOpacity=0),Closed=dict(HitboxType=mid,CustomModelAnimation='Blocks/FoundryFloor/Animations/'+mid+'_Close.blockyanim',InteractionSoundEventId='FF_SFX_'+cue)))
        write(SERVER/'Item/Block/Hitboxes/FoundryFloor'/(mid+'_Open.json'),hitboxes(m,True))
    if kind=='lamp':
        b['Light']=dict(Color='#DB9',Radius=5)
        b['InteractionHint']='server.foundryFloor.toggle'
        b['Interactions']=dict(Use='FF_ToggleLamp')
        b['State']=dict(Definitions=dict(Off=dict(Light=None,InteractionSoundEventId='FF_SFX_mechanical_latch'),On=dict(Light=dict(Color='#DB9',Radius=5),InteractionSoundEventId='FF_SFX_mechanical_latch')))
    if kind in ['shutter','hinge','hatch','lamp']:
        b['Interactions']=dict(Use='FF_RuntimeUse')
        for definition in b.get('State',{}).get('Definitions',{}).values():definition.pop('InteractionSoundEventId',None)
    # The lift has no Use hook: animation is not passenger transport or collision motion.
    item=dict(TranslationProperties=dict(Name='server.foundryFloor.'+mid+'.name',Description='server.foundryFloor.'+mid+'.description'),Icon='Icons/Items/'+mid+'.png',MaxStack=16 if mid in ['FF_RollerShutter','FF_FreightLift'] else 64,Categories=['Furniture.Doors' if kind in ['shutter','hinge','hatch'] else 'Furniture.Lighting' if kind=='lamp' else 'Furniture.Furniture'],PlayerAnimationsId='Block',Interactions=dict(Primary='Block_Primary',Secondary='Block_Secondary'),BlockType=b,ItemSoundSetId='FF_ISS_Metal',Recipe=dict(Input=[dict(ItemId='Ingredient_Bar_Iron',Quantity=m['cost'])],BenchRequirement=[dict(Type='Crafting',Id='Workbench',Categories=['Workbench_Crafting'])],TimeSeconds=2,OutputQuantity=m['quantity']))
    category={'Floors':'Floors','Catwalks':'Walkways','Structure':'Structure','Fixtures':'Fixtures','Interactive':'Mechanisms'}
    if mid=='FF_FoundryBench':
        item['Categories']=['Furniture.Benches'];item['MaxStack']=1
        item['Recipe']['Input'].append(dict(ItemId='Ingredient_Bar_Copper',Quantity=4))
        icons={'Floors':'FF_FloorPlate','Walkways':'FF_CatwalkBridge','Structure':'FF_SteelColumn','Fixtures':'FF_WallVent','Mechanisms':'FF_RollerShutter'}
        b['Bench']=dict(Type='Crafting',Id='FoundryFloor',Categories=[dict(Id='Foundry_'+c,Icon='Icons/CraftingCategories/FoundryFloor/'+icon+'.png',Name='server.foundryFloor.bench.'+c) for c,icon in icons.items()],TierLevels=[dict(CraftingTimeReductionModifier=0)],LocalOpenSoundEventId='FF_SFX_bench_tools_ui',LocalCloseSoundEventId='FF_SFX_bench_tools_ui',CompletedSoundEventId='FF_SFX_bench_tools')
        b['BlockEntity']=dict(Components=dict(BenchBlock={}))
    else:item['Recipe']['BenchRequirement']=[dict(Type='Crafting',Id='FoundryFloor',Categories=['Foundry_'+category[m['category']]])]
    write(SERVER/'Item/Items/FoundryFloor'/(mid+'.json'),item)

def main():
    models();texture()
    for m in MODELS:export_model(m);export_item(m)
    from build_lift import build as build_lift
    build_lift(next(m for m in MODELS if m['id']=='FF_FreightLift'))
    write(SERVER/'Item/Block/Sounds/FoundryFloor/FF_Metal.json',dict(Parent='Metal',SoundEvents=dict(Build='FF_SFX_metal_handling',Break='FF_SFX_metal_handling')))
    write(ASSETS/'manifest.json',dict(Group='FoundryFloor',Name='FoundryFloor',Version='0.1.0',Description='Original modular factory architecture, crafting and mechanical controls. Private development build.',Main='dev.foundryfloor.hytale.FoundryPlugin',ServerVersion='=0.7.2',IncludesAssetPack=True))
    write(SERVER/'Item/RootInteractions/FoundryFloor/FF_RuntimeUse.json',dict(Interactions=[dict(Type='FF_Use')]))
    for id,changes in [('FF_Toggle',{'default':'Open','Closed':'Open','Open':'Closed'}),('FF_ToggleLamp',{'default':'Off','Off':'On','On':'Off'})]:
        write(SERVER/'Item/RootInteractions/FoundryFloor'/(id+'.json'),dict(Interactions=[dict(Type='ChangeState',Changes=changes)]))
    lines=['foundryFloor.toggle = Toggle','foundryFloor.liftUse = Operate freight lift']+['foundryFloor.bench.'+c+' = '+c for c in ['Floors','Walkways','Structure','Fixtures','Mechanisms']]
    for m in MODELS:lines += ['foundryFloor.'+m['id']+'.name = '+m['title'],'foundryFloor.'+m['id']+'.description = '+m['description']]
    lang=SERVER/'Languages/en-US/server.lang';lang.parent.mkdir(parents=True,exist_ok=True);lang.write_text('\n'.join(lines)+'\n')
    write(ROOT/'data/catalog.json',MODELS)
    plan=dict(provider='ElevenLabs connected plugin only',nativeListening='pending acquired shared-client slot and human listening',items=[])
    checklist=[]
    for m in MODELS:
        kind=(m['motion'] or {}).get('kind');cues=['metal_handling']
        if kind=='shutter':cues+=['shutter_travel']
        if kind in ['hinge','hatch','lamp']:cues+=['mechanical_latch']
        if kind=='lift':cues+=['lift_travel','mechanical_latch']
        if m['id']=='FF_CallPanel':cues+=['mechanical_latch','control_accept']
        if m['id']=='FF_FreightLift':cues+=['control_accept']
        if m['id']=='FF_FoundryBench':cues+=['bench_tools']
        plan['items'].append(dict(itemId=m['id'],cues=cues,handlingTrigger='inventory move / confirmed placement and removal',actionTrigger='native bench UI open/close and committed craft completion' if m['id']=='FF_FoundryBench' else 'accepted state change' if kind in ['shutter','hinge','hatch','lamp'] else 'server-authoritative movement start/stop; controller pending' if kind=='lift' else 'none',nativeListening='pending',loopLifecycle='stop on arrived, blocked, cancelled, unload, removal and disconnect' if kind=='lift' else 'no loop'))
        checklist.append(dict(itemId=m['id'],model='exported',editableSource='exported',animation='exported' if m['groups'] else 'static',icon='pending render',schema='pending',nativeAppearance='pending acquired shared-client slot',nativeInteraction='pending' if kind or m['id']=='FF_FoundryBench' else 'static',audioCues=cues,nativeListening='pending'))
    write(ROOT/'data/audio-plan.json',plan);write(ROOT/'data/asset-checklist.json',checklist)
    print(f'Exported {len(MODELS)} original models, editable sources, recipes and interaction candidates.')

if __name__=='__main__':main()
