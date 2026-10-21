#!/usr/bin/env python3
"""Read actual exported Hytale nodes and render textured, depth-tested review images."""
from pathlib import Path
import json,math,sys
import numpy as np
from PIL import Image,ImageDraw,ImageFont,ImageFilter
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'art/previews'
TEX=ROOT/'assets/Common/Blocks/FoundryFloor/FactoryAtlas.png'
def write(p,d):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,indent=2)+'\n')
def quat(q):
    x,y,z,w=[q[k] for k in 'xyzw']
    return np.array([[1-2*y*y-2*z*z,2*x*y-2*z*w,2*x*z+2*y*w],[2*x*y+2*z*w,1-2*x*x-2*z*z,2*y*z-2*x*w],[2*x*z-2*y*w,2*y*z+2*x*w,1-2*x*x-2*y*y]])
def vec(v):return np.array([v[k] for k in 'xyz'],dtype=float)
def sample(keys,t,quaternion=False):
    a,b=keys[0],keys[-1];f=max(0,min(1,(t-a['time'])/(b['time']-a['time'])))
    f=f*f*(3-2*f)
    av=np.array(list(a['delta'].values()),dtype=float);bv=np.array(list(b['delta'].values()),dtype=float)
    if quaternion and av@bv<0:bv=-bv
    r=av*(1-f)+bv*f
    if quaternion:r/=np.linalg.norm(r)
    return dict(zip('xyzw' if quaternion else 'xyz',r))
def faces(model,animation=None,amount=0):
    fs=[];time=(animation or {}).get('duration',1)*amount
    def walk(nodes,R=np.eye(3),T=np.zeros(3)):
        for node in nodes:
            p=vec(node['position']);r=quat(node['orientation']);a=(animation or {}).get('nodeAnimations',{}).get(node['name'],{})
            if a.get('position'):p+=vec(sample(a['position'],time))
            if a.get('orientation'):r=r@quat(sample(a['orientation'],time,True))
            rr=R@r;tt=T+R@p
            s=node.get('shape')
            if s and s.get('type')=='box':
                size=vec(s['settings']['size']);off=vec(s['offset']);lo=off-size/2;hi=off+size/2
                x0,y0,z0=lo;x1,y1,z1=hi
                specs=[('front',[[x1,y0,z1],[x0,y0,z1],[x0,y1,z1],[x1,y1,z1]],[0,0,1],size[[0,1]]),('back',[[x0,y0,z0],[x1,y0,z0],[x1,y1,z0],[x0,y1,z0]],[0,0,-1],size[[0,1]]),('left',[[x0,y0,z1],[x0,y0,z0],[x0,y1,z0],[x0,y1,z1]],[-1,0,0],size[[2,1]]),('right',[[x1,y0,z0],[x1,y0,z1],[x1,y1,z1],[x1,y1,z0]],[1,0,0],size[[2,1]]),('top',[[x0,y1,z0],[x1,y1,z0],[x1,y1,z1],[x0,y1,z1]],[0,1,0],size[[0,2]]),('bottom',[[x0,y0,z1],[x1,y0,z1],[x1,y0,z0],[x0,y0,z0]],[0,-1,0],size[[0,2]])]
                for name,verts,normal,wh in specs:
                    uv=s['textureLayout'][name]['offset'];u,v=uv['x'],uv['y'];w,h=wh
                    fs.append(dict(v=(np.array(verts)@rr.T+tt).tolist(),n=(rr@normal).tolist(),uv=[[u,v+h],[u+w,v+h],[u+w,v],[u,v]]))
            if node.get('children'):walk(node['children'],rr,tt)
    walk(model['nodes']);return fs
def render(fs,width=700,height=640,yaw=35,elev=24,transparent=False,fit=.88):
    atlas=np.array(Image.open(TEX).convert('RGBA'))
    verts=np.array([p for f in fs for p in f['v']]);lo=verts.min(0);hi=verts.max(0);center=(lo+hi)/2
    a,b=np.radians([yaw,elev]);eye=np.array([math.sin(a)*math.cos(b),math.sin(b),math.cos(a)*math.cos(b)])
    right=np.cross([0,1,0],eye);right/=np.linalg.norm(right);up=np.cross(eye,right);basis=np.stack([right,up,eye])
    pv=(verts-center)@basis.T;scale=min(width/max(1,np.ptp(pv[:,0])),height/max(1,np.ptp(pv[:,1])))*fit
    def project(v):
        p=(np.array(v)-center)@basis.T
        return np.stack([p[...,0]*scale+width/2,-p[...,1]*scale+height/2,p[...,2]],axis=-1)
    yy=np.linspace(0,1,height)[:,None,None]
    rgb=np.broadcast_to(np.array([19,30,34])*(1-yy)+np.array([31,46,49])*yy,(height,width,3)).copy()
    zbuf=np.full((height,width),-np.inf);alpha=np.zeros((height,width),dtype=np.uint8)
    light=np.array([-.45,.85,.6]);light/=np.linalg.norm(light)
    def translucent(f):
        u,v=np.array(f['uv'][0]).astype(int).clip(0,511)
        return atlas[v,u,3]<255
    # Paint opaque geometry first, then transparent faces from back to front.
    for f in sorted(fs,key=lambda f:(translucent(f),np.mean(project(f['v'])[:,2]))):
        n=np.array(f['n'])
        if n@eye<=0:continue
        shade=.53+.47*max(0,n@light);p=project(f['v']);uv=np.array(f['uv'])
        for ids in [[0,1,2],[0,2,3]]:
            tri=p[ids];tx=uv[ids];mn=np.maximum(np.floor(tri[:,:2].min(0)).astype(int),0);mx=np.minimum(np.ceil(tri[:,:2].max(0)).astype(int),[width-1,height-1])
            if (mx<mn).any():continue
            x,y=np.meshgrid(np.arange(mn[0],mx[0]+1)+.5,np.arange(mn[1],mx[1]+1)+.5);a,c,e=tri
            den=(c[1]-e[1])*(a[0]-e[0])+(e[0]-c[0])*(a[1]-e[1])
            if abs(den)<1e-9:continue
            w0=((c[1]-e[1])*(x-e[0])+(e[0]-c[0])*(y-e[1]))/den;w1=((e[1]-a[1])*(x-e[0])+(a[0]-e[0])*(y-e[1]))/den;w2=1-w0-w1
            z=w0*a[2]+w1*c[2]+w2*e[2];region=zbuf[mn[1]:mx[1]+1,mn[0]:mx[0]+1]
            mask=(w0>=-1e-6)&(w1>=-1e-6)&(w2>=-1e-6)&(z>region)
            if not mask.any():continue
            u=(w0*tx[0,0]+w1*tx[1,0]+w2*tx[2,0]).astype(int).clip(0,511);v=(w0*tx[0,1]+w1*tx[1,1]+w2*tx[2,1]).astype(int).clip(0,511)
            opacity=atlas[v,u,3]/255.0;colors=np.clip(atlas[v,u,:3]*shade,0,255)
            target=rgb[mn[1]:mx[1]+1,mn[0]:mx[0]+1];alpha_region=alpha[mn[1]:mx[1]+1,mn[0]:mx[0]+1]
            if transparent:
                old=alpha_region/255.0;combined=opacity+old*(1-opacity)
                mixed=(colors*opacity[...,None]+target*old[...,None]*(1-opacity[...,None]))/np.maximum(combined[...,None],1e-9)
                target[mask]=mixed[mask];alpha_region[mask]=(combined[mask]*255).astype('uint8')
            else:target[mask]=(colors*opacity[...,None]+target*(1-opacity[...,None]))[mask];alpha_region[mask]=255
            if not translucent(f):region[mask]=z[mask]
    if transparent:return Image.fromarray(np.dstack([rgb.astype('uint8'),alpha]))
    return Image.fromarray(rgb.astype('uint8'))
def font(size,bold=False):return ImageFont.truetype('/System/Library/Fonts/Supplemental/Arial'+(' Bold' if bold else '')+'.ttf',size)
def main():
    OUT.mkdir(parents=True,exist_ok=True);catalog=json.loads((ROOT/'data/catalog.json').read_text());gallery=[];fsby={}
    for m in catalog:
        model=json.loads((ROOT/'assets/Common/Blocks/FoundryFloor'/(m['id']+'.blockymodel')).read_text());anims={}
        for p in (ROOT/'assets/Common/Blocks/FoundryFloor/Animations').glob(m['id']+'_*.blockyanim'):anims[p.stem.removeprefix(m['id']+'_')]=json.loads(p.read_text())
        fs=faces(model);fsby[m['id']]=fs
        render(fs,600,520).save(OUT/(m['id']+'.png'))
        icon=render(fs,256,256,transparent=True,fit=.82).resize((64,64),Image.Resampling.LANCZOS)
        dest=ROOT/'assets/Common/Icons/Items'/(m['id']+'.png');dest.parent.mkdir(parents=True,exist_ok=True);icon.save(dest,compress_level=9)
        if m['id'] in ['FF_FloorPlate','FF_CatwalkBridge','FF_SteelColumn','FF_WallVent','FF_RollerShutter']:
            category=ROOT/'assets/Common/Icons/CraftingCategories/FoundryFloor'/(m['id']+'.png');category.parent.mkdir(parents=True,exist_ok=True);icon.save(category,compress_level=9)
        if anims:
            a=anims.get('Open',anims.get('Up'));render(faces(model,a,1),600,520).save(OUT/(m['id']+'_Open.png'))
        gallery.append({k:m[k] for k in ['id','title','category','description','cost','quantity','motion']}|dict(model=model,animations=anims))
    placements=[]
    def place(mid,x,y,z,rotation=0):placements.append(dict(id='FF_'+mid,position=[x,y,z],rotation=rotation))
    for x in range(-160,193,32):
        for z in range(-128,97,32):place('HazardFloor' if z==96 else 'FloorPlate' if x<64 else 'ConcreteFloor',x,-32,z)
    place('RollerShutter',-64,0,-112)
    for x in range(-128,65,32):place('CatwalkBridge',x,96,-64,90)
    for x in [-128,-32,64]:
        for y in [0,32,64]:place('SteelColumn',x,y,-64)
    for i in range(3):place('CatwalkStairs',-128,32*i,64-32*i)
    place('CatwalkLeft',-128,96,-32)
    for z in range(-96,65,32):place('PipeStraight',-160,54,z)
    place('PipeElbow',-160,54,96)
    place('SafetyGate',-96,96,-64,90)
    place('CargoPallet',-32,0,48);place('CargoPallet',0,0,48)
    place('FoundryBench',-80,0,40)
    place('ServiceHatch',32,0,16)
    for x in [-128,-64,0]:place('WallLamp',x,48,-112)
    write(ROOT/'data/showcase-layout.json',placements)
    allfaces=[]
    for p in placements:
        angle=math.radians(p['rotation']);r=np.array([[math.cos(angle),0,math.sin(angle)],[0,1,0],[-math.sin(angle),0,math.cos(angle)]])
        for f in fsby[p['id']]:allfaces.append(dict(v=(np.array(f['v'])@r.T+np.array(p['position'])).tolist(),n=(r@f['n']).tolist(),uv=f['uv']))
    scene=render(allfaces,1800,1180,32,29,fit=.91);scene.save(OUT/'Factory-Assembly.png')
    sheet=Image.new('RGB',(2000,140+math.ceil(len(catalog)/5)*395),(16,28,32));d=ImageDraw.Draw(sheet)
    d.text((48,30),'FOUNDRY FLOOR',font=font(48,True),fill='#eee8d5');d.text((48,92),f'{len(catalog)} modular factory blocks  /  Original native geometry  /  Offline review',font=font(21),fill='#a2b1ad')
    for i,m in enumerate(catalog):
        x=(i%5)*400;y=140+(i//5)*395
        sheet.paste(Image.open(OUT/(m['id']+'.png')).resize((384,333)),(x+8,y))
        d.text((x+18,y+333),m['title'],font=font(17,True),fill='#e1cc90')
        d.text((x+18,y+359),m['id'],font=font(13),fill='#90a6a5')
    sheet.save(OUT/'Factory-Contact-Sheet.png')
    inter=Image.new('RGB',(1800,1000),(16,28,32));d=ImageDraw.Draw(inter)
    d.text((38,25),'MOVING PARTS  /  CLOSED → OPEN',font=font(32,True),fill='#eee8d5')
    for i,mid in enumerate(['FF_RollerShutter','FF_PersonnelDoor','FF_SafetyGate']):
        for j,suffix in enumerate(['','_Open']):inter.paste(Image.open(OUT/(mid+suffix+'.png')).resize((560,440)),(i*600+20,j*440+100))
    inter.save(OUT/'Mechanical-States.png')
    # Embed data and texture: the viewer works when opened directly as a file.
    import base64
    js='window.FF_GALLERY='+json.dumps(gallery,separators=(',',':'))+';window.FF_LAYOUT='+json.dumps(placements,separators=(',',':'))+';window.FF_ATLAS="data:image/png;base64,'+base64.b64encode(TEX.read_bytes()).decode()+'";'
    (ROOT/'viewer-data.js').write_text(js)
    checks=json.loads((ROOT/'data/asset-checklist.json').read_text())
    for c in checks:c['icon']='rendered from exported native geometry';c['offlineModelRender']='art/previews/'+c['itemId']+'.png'
    write(ROOT/'data/asset-checklist.json',checks)
    print(f'Rendered {len(catalog)} native models, icons, assembled factory and moving-state comparisons.')
def technical():
    OUT.mkdir(parents=True,exist_ok=True)
    for mid in ['FF_LiftController','FF_LiftTravel']:
        model=json.loads((ROOT/'assets/Common/Blocks/FoundryFloor'/(mid+'.blockymodel')).read_text())
        render(faces(model),600,520).save(OUT/(mid+'.png'))
        if mid=='FF_LiftTravel':
            clip=json.loads((ROOT/'assets/Common/Blocks/FoundryFloor/Animations/FF_LiftPose30.blockyanim').read_text())
            render(faces(model,clip,0),600,520).save(OUT/(mid+'_Held30.png'))
    print('Rendered the native lift controller, closed travel gate and mid-shaft held pose. Runtime icons unchanged.')
if __name__=='__main__':
    technical() if '--technical' in sys.argv else main()
