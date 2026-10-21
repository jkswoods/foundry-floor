#!/usr/bin/env python3
"""Original lift controller and held native cab collision poses; no game/runtime access."""
import copy,json
import build_models as b

def build(lift):
    # Keep the full original source/model; the obtainable kit places this separate console.
    console=dict(id='FF_LiftController',title='Freight lift controller',groups={},motion=None,parts=[
        b.part('Base',(0,2,0),(22,4,22),'dark'),b.part('Pedestal',(0,17,0),(12,26,12),'steel'),
        b.part('Panel',(0,33,0),(22,10,18),'dark'),b.part('SafetyBorder',(0,39,0),(24,2,20),'hazard'),
        b.part('Up',(5,33,10),(5,5,2),'green'),b.part('Down',(-5,33,10),(5,5,2),'red'),
        b.part('Label',(0,40,0),(14,1,12),'edge')])
    b.export_model(console)
    controller=json.loads((b.SERVER/'Item/Items/FoundryFloor/FF_FreightLift.json').read_text())
    block=controller['BlockType'];block['CustomModel']='Blocks/FoundryFloor/FF_LiftController.blockymodel';block['HitboxType']='FF_LiftController'
    block['Interactions']={'Use':'FF_RuntimeUse'};block['InteractionHint']='server.foundryFloor.liftUse'
    block['State']={'Definitions':{s:({'AmbientSoundEventId':'FF_SFX_lift_travel'} if s in ('Raising','Lowering') else {}) for s in ('Lowered','Raising','Raised','Lowering','Blocked','Disabled')}}
    b.write(b.SERVER/'Item/Block/Hitboxes/FoundryFloor/FF_LiftController.json',b.hitboxes(console))
    b.write(b.SERVER/'Item/Items/FoundryFloor/FF_FreightLift.json',controller)
    # Travel gate follows the platform; open models are used only at the two docks.
    travel=copy.deepcopy(lift);travel['id']='FF_LiftTravel';travel['title']='Freight lift closed travel cab';travel['motion']=None
    travel['parts'] += [b.part('TravelGateTop',(0,73,37),(80,3,3),'yellow','Platform'),b.part('TravelGateMid',(0,36,37),(80,3,3),'edge','Platform'),b.part('TravelGateToe',(0,9,37),(80,8,3),'hazard','Platform')]
    for x in range(-36,37,9):travel['parts'].append(b.part('TravelGateBar'+str(x),(x,42,37),(1,58,1),'dark','Platform'))
    b.export_model(travel)
    helper=copy.deepcopy(controller);helper.pop('Recipe');helper['MaxStack']=1;helper['Categories']=[]
    hb=helper['BlockType'];hb['CustomModel']='Blocks/FoundryFloor/FF_FreightLift.blockymodel';hb['HitboxType']='FF_LiftDock00';hb.pop('Gathering',None)
    definitions={}
    for step in range(61):
        delta={'x':0,'y':step*1.6,'z':0}
        anim=dict(formatVersion=1,duration=1,holdLastKeyframe=True,nodeAnimations={'Platform':{'position':[dict(time=0,delta=delta,interpolationType='linear'),dict(time=1,delta=delta,interpolationType='linear')]}})
        path='Blocks/FoundryFloor/Animations/FF_LiftPose%02d.blockyanim'%step
        b.write(b.COMMON/path,anim)
        for dock in (False,True) if step in (0,60) else (False,):
            name=('Dock' if dock else 'Step')+'%02d'%step
            model=lift if dock else travel
            # Exact part envelopes plus a solid walkable deck prevent tiny grate gaps trapping feet.
            parts=[p for p in model['parts'] if not p['name'].startswith('CabDeck')]
            parts.append(b.part('CabSolidDeck',(0,2,0),(88,4,80),'steel','Platform'))
            posed=copy.deepcopy(model);posed['parts']=parts;posed['motion']=lift['motion']
            boxes=[]
            for p in parts:
                c,size=b.transform(p,posed,step/60)
                boxes.append(dict(Min={a:(c[i]-size[i]/2)/32+(0 if i==1 else .5) for i,a in enumerate('XYZ')},Max={a:(c[i]+size[i]/2)/32+(0 if i==1 else .5) for i,a in enumerate('XYZ')}))
            hit='FF_Lift'+name;b.write(b.SERVER/'Item/Block/Hitboxes/FoundryFloor'/(hit+'.json'),dict(Boxes=boxes))
            definitions[name]=dict(HitboxType=hit,CustomModel='Blocks/FoundryFloor/'+('FF_FreightLift' if dock else 'FF_LiftTravel')+'.blockymodel',CustomModelAnimation=path)
    hb['State']={'Definitions':definitions};hb['CustomModelAnimation']='Blocks/FoundryFloor/Animations/FF_LiftPose00.blockyanim'
    b.write(b.SERVER/'Item/Items/FoundryFloor/FF_LiftAssembly.json',helper)
    panel=b.SERVER/'Item/Items/FoundryFloor/FF_CallPanel.json';item=json.loads(panel.read_text());item['BlockType']['Interactions']={'Use':'FF_RuntimeUse'};item['BlockType']['InteractionHint']='server.foundryFloor.liftUse';b.write(panel,item)

if __name__=='__main__':
    b.models();build(next(m for m in b.MODELS if m['id']=='FF_FreightLift'))
