package dev.foundryfloor.hytale;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.*;
import com.hypixel.hytale.server.core.modules.entity.component.*;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.splitvelocity.VelocityConfig;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.foundryfloor.core.*;
import org.joml.Vector3d;
import java.util.*;

/** Bounded native cab/collider controller. Passenger velocity requires acquired-client acceptance. */
final class NativeLift {
    private static final VelocityConfig CARRY=new VelocityConfig();
    static { CARRY.setGroundResistance(1);CARRY.setAirResistance(1); }
    private NativeLift() {}
    static boolean placementClear(Store<EntityStore> store,FoundryRegistry.Claim claim) {
        return !obstructed(store,claim,0,60,Set.of());
    }
    static FoundryRegistry.Claim resolve(FoundryWorld data,BlockPos p) {
        var direct=data.registry.at(p);return direct!=null?direct:data.registry.occupying(p);
    }
    static boolean install(World world,FoundryRegistry.Claim claim) {
        BlockPos p=NativeBlocks.liftOrigin(claim.origin(),claim.rotation());
        String actual=NativeBlocks.itemId(NativeBlocks.block(world,p));
        if(!"FF_LiftAssembly".equals(actual)&&!NativeBlocks.empty(world,p))return false;
        if(!NativeBlocks.clearLift(world,claim))return false;
        return pose(world,claim,claim.liftStep(),claim.liftStep()==0||claim.liftStep()==60);
    }
    static boolean pose(World world,FoundryRegistry.Claim claim,int step,boolean dock) {
        BlockType base=BlockType.getAssetMap().getAsset("FF_LiftAssembly");
        BlockType next=base==null?null:base.getBlockForState((dock?"Dock":"Step")+String.format(Locale.ROOT,"%02d",step));
        return next!=null&&NativeBlocks.set(world,NativeBlocks.liftOrigin(claim.origin(),claim.rotation()),next,claim.rotation());
    }
    static void remove(World world,FoundryRegistry.Claim claim) {
        BlockPos p=NativeBlocks.liftOrigin(claim.origin(),claim.rotation());
        if("FF_LiftAssembly".equals(NativeBlocks.itemId(NativeBlocks.block(world,p))))NativeBlocks.set(world,p,BlockType.getAssetMap().getAsset("Empty"),0);
    }
    static void use(Store<EntityStore> store,PlayerRef actor,FoundryRegistry.Claim target) {
        var data=FoundryPlugin.data(store);World world=store.getExternalData().getWorld();var lift=target;Boolean upper=null;
        if(target.itemId().equals("FF_CallPanel")) {
            if(target.linkedTo()!=null)lift=data.registry.at(target.linkedTo());
            else {
                double best=144;lift=null;boolean ambiguous=false;
                for(var candidate:data.registry.claims())if(candidate.itemId().equals("FF_FreightLift")&&candidate.owner().equals(actor.getUuid())) {
                    var p=NativeBlocks.liftOrigin(candidate.origin(),candidate.rotation());
                    double d=distanceSquared(target.origin(),p);
                    if(d<best-.01) { best=d;lift=candidate;ambiguous=false; }else if(Math.abs(d-best)<.01)ambiguous=true;
                }
                if(ambiguous)throw new IllegalArgumentException("Two lifts are equally close; move this panel nearer its lift");
                if(lift==null)throw new IllegalArgumentException("Place an owned freight lift within twelve blocks of this panel");
                data.registry.link(target.origin(),actor.getUuid(),lift.origin());
            }
            if(lift==null||!lift.itemId().equals("FF_FreightLift"))throw new IllegalArgumentException("This panel's lift was removed; remove and replace the panel to relink");
            upper=target.origin().y()>=lift.origin().y()+2;
        }
        if(!lift.owner().equals(actor.getUuid()))throw new SecurityException("This freight lift belongs to another player");
        if(!"FF_FreightLift".equals(NativeBlocks.itemId(NativeBlocks.block(world,lift.origin()))))throw new IllegalArgumentException("The lift controller is not loaded");
        if(!NativeBlocks.clearLift(world,lift)||!install(world,lift))throw new IllegalArgumentException("Load and clear the lift's reserved shaft first");
        var motion=data.lifts.computeIfAbsent(lift.origin(),key->new FreightMotion(liftStep(data,key)));
        if(motion.moving())return;
        if(upper==null)upper=motion.step()<30;
        if(motion.step()==(upper?60:0)) { FoundryPlugin.message(actor,"The freight lift is already at this floor");return; }
        Set<UUID> riders=riders(store,lift,motion.step(),true);
        // Saved offline tickets protect passengers from losing their platform after reconnect/reload.
        for(UUID old:lift.passengers())if(world.getEntityRef(old)==null)throw new IllegalArgumentException("A saved passenger is offline; wait for them to return before moving this lift");
        if(riders.size()>8)throw new IllegalArgumentException("At most eight passengers can ride this lift");
        if(obstructed(store,lift,motion.step(),motion.step(),riders))throw new IllegalArgumentException("Clear the lift entrance and cabin before closing its travel gate");
        if(!pose(world,lift,motion.step(),false))throw new IllegalStateException("The travel gate could not close");
        data.registry.passengers(lift.origin(),lift.owner(),riders);
        if(!motion.request(upper))return;
        String name=name(motion.state());
        if(!NativeBlocks.state(world,lift,name)) { motion.disable();throw new IllegalStateException("The lift controller could not start"); }
        data.registry.state(lift.origin(),lift.owner(),name,motion.step());FoundryPlugin.play(store,lift.origin(),"control_accept");
    }
    private static int liftStep(FoundryWorld d,BlockPos p) { return d.registry.at(p).liftStep(); }
    static void tick(Store<EntityStore> store,float dt) {
        var data=FoundryPlugin.data(store);World world=store.getExternalData().getWorld();
        for(var saved:data.registry.claims())if(saved.itemId().equals("FF_FreightLift")) {
            var motion=data.lifts.get(saved.origin());
            if(motion==null) {
                // Loaded saves never resume a moving loop or accumulate elapsed time.
                if(NativeBlocks.section(world,saved.origin())==null)continue;
                motion=new FreightMotion(saved.liftStep());data.lifts.put(saved.origin(),motion);
                if(!NativeBlocks.clearLift(world,saved)||!install(world,saved))motion.disable();
                NativeBlocks.state(world,saved,name(motion.state()));data.registry.state(saved.origin(),saved.owner(),name(motion.state()),saved.liftStep());
                continue;
            }
            if(!motion.moving()) {
                // A previously unloaded controller may still contain its old running state.
                // Reconcile it when loaded so movement ambience never restarts by itself.
                NativeBlocks.state(world,saved,name(motion.state()));
                Set<UUID> current=riders(store,saved,motion.step(),false),tickets=new HashSet<>(saved.passengers());
                tickets.removeIf(id->world.getEntityRef(id)!=null&&!current.contains(id));
                if(!tickets.equals(saved.passengers()))data.registry.passengers(saved.origin(),saved.owner(),tickets);
                continue;
            }
            var claim=data.registry.at(saved.origin());
            if(dt>.12f||!Float.isFinite(dt)||!"FF_FreightLift".equals(NativeBlocks.itemId(NativeBlocks.block(world,claim.origin())))||!NativeBlocks.clearLift(world,claim)||!validRiders(store,claim,motion.step()))motion.disable();
            if(motion.moving()) {
                carry(store,claim,motion.step(),motion.state()==FreightMotion.State.RAISING?.75:-.75);
                var active=motion;
                motion.advance(dt,(before,after)->{
                    if(obstructed(store,claim,before,after,claim.passengers()))return false;
                    if(!pose(world,claim,after,after==0||after==60))return false;
                    data.registry.state(claim.origin(),claim.owner(),name(active.state()),after);return true;
                });
            }
            if(!motion.moving()) {
                carry(store,claim,motion.step(),0);
                NativeBlocks.state(world,claim,name(motion.state()));data.registry.state(claim.origin(),claim.owner(),name(motion.state()),motion.step());
                FoundryPlugin.play(store,claim.origin(),"mechanical_latch");
            }
        }
    }
    static boolean empty(Store<EntityStore> store,FoundryRegistry.Claim claim) {
        if(!claim.passengers().isEmpty()) {
            World world=store.getExternalData().getWorld();
            for(UUID id:claim.passengers())if(world.getEntityRef(id)==null)return false;
        }
        return riders(store,claim,claim.liftStep(),false).isEmpty();
    }
    private static Vector3d local(FoundryRegistry.Claim claim,Vector3d position) {
        var p=NativeBlocks.liftOrigin(claim.origin(),claim.rotation());var v=new Vector3d(position).sub(p.x()+.5,p.y(),p.z()+.5);RotationTuple.get(claim.rotation()).undoRotationTo(v);return v;
    }
    private static Set<UUID> riders(Store<EntityStore> store,FoundryRegistry.Claim claim,int step,boolean onDeck) {
        Set<UUID> found=new HashSet<>();
        for(PlayerRef p:store.getExternalData().getWorld().getPlayerRefs()) {
            var ref=p.getReference();if(ref==null||!ref.isValid())continue;
            var transform=store.getComponent(ref,TransformComponent.getComponentType());if(transform==null)continue;
            var pos=local(claim,transform.getPosition());double floor=step/20.0+.125;
            double xLimit=onDeck?.85:1.15,zLimit=onDeck?.75:1.05;
            if(Math.abs(pos.x)<xLimit&&Math.abs(pos.z)<zLimit&&pos.y>=floor-.25&&pos.y<=floor+(onDeck?.28:2.5))found.add(p.getUuid());
        }
        return found;
    }
    private static boolean validRiders(Store<EntityStore> store,FoundryRegistry.Claim claim,int step) {
        World w=store.getExternalData().getWorld();
        for(UUID id:claim.passengers()) {
            var ref=w.getEntityRef(id);if(ref==null||!ref.isValid()||store.getComponent(ref,DeathComponent.getComponentType())!=null)return false;
            var transform=store.getComponent(ref,TransformComponent.getComponentType());var velocity=store.getComponent(ref,Velocity.getComponentType());if(transform==null||velocity==null)return false;
            var p=local(claim,transform.getPosition());
            if(Math.abs(p.x)>1.15||p.z<-.99||p.z>1.04||Math.abs(p.y-(step/20.0+.125))>.4)return false;
        }
        return true;
    }
    private static void carry(Store<EntityStore> store,FoundryRegistry.Claim claim,int step,double speed) {
        World world=store.getExternalData().getWorld();
        for(UUID id:claim.passengers()) {
            var ref=world.getEntityRef(id);if(ref==null||!ref.isValid())continue;
            var v=store.getComponent(ref,Velocity.getComponentType());var t=store.getComponent(ref,TransformComponent.getComponentType());if(v==null||t==null)continue;
            double error=claim.origin().y()+step/20.0+.125-t.getPosition().y;
            double correction=speed==0?0:Math.max(-.2,Math.min(.2,error*2));
            // Native velocity channel only; no Teleport component, transform write or click-position warp.
            v.addInstruction(new Vector3d(v.getClientVelocity().x,speed+correction,v.getClientVelocity().z),CARRY,ChangeVelocityType.Set);
        }
    }
    private static boolean obstructed(Store<EntityStore> store,FoundryRegistry.Claim claim,int before,int after,Set<UUID> riders) {
        World world=store.getExternalData().getWorld();if(!NativeBlocks.clearLift(world,claim))return true;
        var origin=NativeBlocks.liftOrigin(claim.origin(),claim.rotation());
        // Cab moves within this whole swept enclosure. Other entities cannot be enclosed or crushed.
        double lower=Math.min(before,after)/20.0,upper=Math.max(before,after)/20.0;
        Box sweep=new Box(origin.x()-1,origin.y()+lower,origin.z()-1,origin.x()+2,origin.y()+upper+2.5,origin.z()+2);
        boolean[] blocked={false};
        store.forEachChunk(Query.and(TransformComponent.getComponentType(),BoundingBox.getComponentType()),(chunk,buffer)->{
            for(int i=0;i<chunk.size()&&!blocked[0];i++) {
                PlayerRef player=chunk.getComponent(i,PlayerRef.getComponentType());if(player!=null&&riders.contains(player.getUuid()))continue;
                var transform=chunk.getComponent(i,TransformComponent.getComponentType());var bounds=chunk.getComponent(i,BoundingBox.getComponentType());
                if(sweep.isIntersecting(new Box(bounds.getBoundingBox()).offset(transform.getPosition())))blocked[0]=true;
            }
        });return blocked[0];
    }
    private static double distanceSquared(BlockPos a,BlockPos b) { return Math.pow(a.x()-b.x(),2)+Math.pow(a.y()-b.y(),2)+Math.pow(a.z()-b.z(),2); }
    private static String name(FreightMotion.State state) {
        String s=state.name().toLowerCase(Locale.ROOT);return Character.toUpperCase(s.charAt(0))+s.substring(1);
    }
}
