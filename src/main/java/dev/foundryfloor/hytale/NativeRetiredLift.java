package dev.foundryfloor.hytale;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.blockanimation.BlockAnimationModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.foundryfloor.core.*;
import java.util.*;

/** Inert old-world compatibility. Never starts motion, carries passengers or accepts floor requests. */
final class NativeRetiredLift {
    static final Set<String> IDS=Set.of("FF_FreightLift","FF_LiftGuide","FF_CallPanel","FF_LiftAssembly");
    private NativeRetiredLift() {}
    static boolean retired(String id) { return IDS.contains(id); }
    static FoundryRegistry.Claim resolve(FoundryWorld data,BlockPos p) {
        var claim=data.registry.at(p);if(claim!=null)return claim;
        claim=data.registry.occupying(p);if(claim!=null)return claim;
        for(var old:data.registry.claims())if(old.itemId().equals("FF_FreightLift")&&NativeBlocks.liftOrigin(old.origin(),old.rotation()).equals(p))return old;
        return null;
    }
    static void reconcile(Store<EntityStore> store) {
        var data=FoundryPlugin.data(store);World world=store.getExternalData().getWorld();
        for(var old:data.registry.claims()) {
            if(!retired(old.itemId())||data.retiredReconciled.contains(old.origin()))continue;
            if(NativeBlocks.section(world,old.origin())==null)continue;
            if(old.itemId().equals("FF_FreightLift")) {
                var shaft=NativeBlocks.liftOrigin(old.origin(),old.rotation());
                if(NativeBlocks.section(world,shaft)==null)continue;
                if("FF_LiftAssembly".equals(NativeBlocks.itemId(NativeBlocks.block(world,shaft)))) {
                    BlockAnimationModule.get().clearBlockAnimationSpeed(world,shaft.x(),shaft.y(),shaft.z());
                    var base=BlockType.getAssetMap().getAsset("FF_LiftAssembly");
                    String prior=base.getStateForBlock(NativeBlocks.block(world,shaft).getId());
                    String state=(old.travelBlocks()==3?"":"H"+old.travelBlocks())+(prior!=null&&prior.contains("Dock")?"Dock":"Step")+String.format(Locale.ROOT,"%02d",old.liftStep());
                    var held=BlockType.getAssetMap().getAsset("FF_LiftAssembly").getBlockForState(state);
                    // Preserve the solid deck at its saved height. The retired guide/cab enclosure
                    // no longer needs collision or fillers through the former empty travel volume.
                    if(held==null||!NativeBlocks.set(world,shaft,held,old.rotation()))continue;
                    var helper=new FoundryRegistry.Claim(shaft,"FF_LiftAssembly",old.rotation(),old.owner(),Footprint.single(shaft),"Disabled",old.liftStep(),Set.of(),null,old.travelBlocks());
                    if(!NativeBlocks.repairLegacyFillers(world,data.registry,helper))continue;
                }
                NativeBlocks.state(world,old,"Disabled");
            }
            // Native block collision/fillers still occupy the actual old geometry. Empty travel
            // volume no longer belongs to the retired mechanism and can be built into normally.
            data.registry.footprint(old.origin(),old.owner(),Footprint.single(old.origin()));
            data.registry.state(old.origin(),old.owner(),"Disabled",old.liftStep());
            data.retiredReconciled.add(old.origin());
        }
    }
    static boolean removalReady(World world,FoundryRegistry.Claim old) {
        return NativeBlocks.section(world,NativeBlocks.liftOrigin(old.origin(),old.rotation()))!=null;
    }
    static void removeCab(World world,FoundryRegistry.Claim old) {
        var p=NativeBlocks.liftOrigin(old.origin(),old.rotation());
        if(!"FF_LiftAssembly".equals(NativeBlocks.itemId(NativeBlocks.block(world,p))))return;
        BlockAnimationModule.get().clearBlockAnimationSpeed(world,p.x(),p.y(),p.z());
        NativeBlocks.set(world,p,BlockType.getAssetMap().getAsset("Empty"),0);
    }
}
