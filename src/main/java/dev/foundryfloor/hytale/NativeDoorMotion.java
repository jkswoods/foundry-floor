package dev.foundryfloor.hytale;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.foundryfloor.core.*;
import java.util.*;

/** Whole-door native poses: translucent glass never depends on client bone animation. */
final class NativeDoorMotion {
    static final int STEPS=16;
    static final long DURATION=800_000_000L;
    record Motion(boolean opening,long started) {}
    private NativeDoorMotion() {}
    static String pose(int step) { return String.format(Locale.ROOT,"DoorPose%02d",step); }
    static int step(boolean opening,long elapsed) {
        int progress=(int)Math.min(STEPS,Math.max(0,elapsed)/Math.max(1,DURATION/STEPS));
        return opening?progress:STEPS-progress;
    }
    static void start(Store<EntityStore> store,FoundryRegistry.Claim claim,String target) {
        var data=FoundryPlugin.data(store);var world=store.getExternalData().getWorld();
        if(data.doorMotions.containsKey(claim.origin()))throw new IllegalArgumentException("Wait for the door to finish moving");
        if(data.doorMotions.size()>=256)throw new IllegalArgumentException("Too many doors are moving; try again shortly");
        BlockType base=BlockType.getAssetMap().getAsset(claim.itemId());
        for(int i=0;i<=STEPS;i++) {
            BlockType shape=base.getBlockForState(pose(i));
            if(shape==null)throw new IllegalStateException("Door motion assets are unavailable");
            if(!NativeBlocks.clear(world,NativeBlocks.placedFootprint(shape,claim.origin(),claim.rotation()),claim.origin()))throw new IllegalArgumentException("Clear the door swing space first");
            if(FoundryPlugin.entityInClosingShape(store,claim,shape))throw new IllegalArgumentException("Someone is in the mechanism's movement space");
        }
        boolean opening=target.equals("Open");
        if(!NativeBlocks.state(world,claim,pose(opening?0:STEPS)))throw new IllegalStateException("Native door motion could not start");
        data.doorMotions.put(claim.origin(),new Motion(opening,System.nanoTime()));
    }
    static boolean restore(Store<EntityStore> store,FoundryRegistry.Claim claim) {
        var world=store.getExternalData().getWorld();String state=claim.state().equals("Open")?"Open":"Closed";
        BlockType shape=BlockType.getAssetMap().getAsset(claim.itemId()).getBlockForState(state);
        return shape!=null&&NativeBlocks.clear(world,NativeBlocks.placedFootprint(shape,claim.origin(),claim.rotation()),claim.origin())
            &&!FoundryPlugin.entityInClosingShape(store,claim,shape)&&NativeBlocks.state(world,claim,state);
    }
    static void tick(Store<EntityStore> store) {
        var data=FoundryPlugin.data(store);var world=store.getExternalData().getWorld();long now=System.nanoTime();
        var work=data.doorMotions.entrySet().iterator();
        while(work.hasNext()) {
            var e=work.next();var p=e.getKey();var claim=data.registry.at(p);var motion=e.getValue();
            if(claim==null||!claim.origin().equals(p)||!claim.itemId().equals("FF_PersonnelDoor")) { work.remove();continue; }
            if(NativeBlocks.section(world,p)==null) { data.reconciledConnections.remove(p);work.remove();continue; }
            if(!claim.itemId().equals(NativeBlocks.itemId(NativeBlocks.block(world,p)))) { work.remove();continue; }
            long elapsed=Math.max(0,now-motion.started());
            String state=elapsed>=DURATION?(motion.opening()?"Open":"Closed"):pose(step(motion.opening(),elapsed));
            // An entering entity/block pauses this bounded motion rather than being
            // struck by the next pose. Unload/reload completes its saved intent.
            BlockType target=BlockType.getAssetMap().getAsset(claim.itemId()).getBlockForState(state);
            if(!NativeBlocks.clear(world,NativeBlocks.placedFootprint(target,p,claim.rotation()),p)||FoundryPlugin.entityInClosingShape(store,claim,target)) {
                e.setValue(new Motion(motion.opening(),motion.started()+50_000_000L));continue;
            }
            if(NativeBlocks.state(world,claim,state)&&elapsed>=DURATION)work.remove();
        }
    }
}
