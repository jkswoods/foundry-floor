package dev.foundryfloor.hytale;

import com.hypixel.hytale.server.core.asset.type.blockhitbox.BlockBoundingBoxes;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockOperations;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.util.FillerBlockUtil;
import com.hypixel.hytale.protocol.BlockPosition;
import dev.foundryfloor.core.*;
import java.util.*;

/** Loaded-only native block adapter. Never force-loads a chunk or writes outside a reserved assembly. */
final class NativeBlocks {
    private NativeBlocks() {}
    static BlockPos pos(org.joml.Vector3i p) { return new BlockPos(p.x,p.y,p.z); }
    static BlockSection section(World w,BlockPos p) {
        var chunks=w.getChunkStore();var ref=chunks.getChunkSectionReferenceAtBlock(p.x(),p.y(),p.z());
        return ref==null||!ref.isValid()?null:chunks.getStore().getComponent(ref,BlockSection.getComponentType());
    }
    static BlockType block(World w,BlockPos p) { var s=section(w,p);return s==null?null:BlockType.getAssetMap().getAsset(s.get(p.x()&31,p.y()&31,p.z()&31)); }
    static boolean empty(World w,BlockPos p) { var s=section(w,p);return s!=null&&s.get(p.x()&31,p.y()&31,p.z()&31)==BlockType.EMPTY_ID&&s.getFiller(p.x()&31,p.y()&31,p.z()&31)==0; }
    static BlockPos origin(World w,BlockPos p) {
        if(section(w,p)==null)return p;
        BlockPosition base=BlockOperations.resolveBaseBlockPosition(w.getChunkStore(),new BlockPosition(p.x(),p.y(),p.z()));
        return base==null?p:new BlockPos(base.x,base.y,base.z);
    }
    static String itemId(BlockType b) {
        if(b==null)return null;
        if(b.getId().startsWith("FF_")&&!b.isState())return b.getId();
        String key=b.getDefaultStateKey();return key!=null&&key.startsWith("FF_")?key:null;
    }
    static int rotation(World w,BlockPos p) { var s=section(w,p);if(s==null)throw new IllegalArgumentException("Assembly is not loaded");return s.getRotationIndex(p.x()&31,p.y()&31,p.z()&31); }
    static Footprint footprint(String id,BlockPos p,int rot) {
        Set<BlockPos> cells=new HashSet<>();cells.add(p);BlockType base=BlockType.getAssetMap().getAsset(id);
        if(base==null)throw new IllegalArgumentException("Unknown Foundry block");
        for(String state:List.of("default","Open","Closed","On","Off")) {
            BlockType type=state.equals("default")?base:base.getBlockForState(state);if(type==null)continue;
            var boxes=BlockBoundingBoxes.getAssetMap().getAsset(type.getHitboxTypeIndex());
            if(boxes==null)throw new IllegalArgumentException("Missing native assembly collision");
            FillerBlockUtil.forEachFillerBlock(boxes.get(rot),(x,y,z)->cells.add(p.add(x,y,z)));
        }
        // The cab's whole shaft is a reservation, including its empty passenger volume.
        if(id.equals("FF_FreightLift")) {
            BlockPos shaft=liftOrigin(p,rot);
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=0;y<7;y++)cells.add(shaft.add(x,y,z));
        }
        return new Footprint(cells);
    }
    static BlockPos liftOrigin(BlockPos p,int rotation) {
        var offset=new org.joml.Vector3i(0,0,-3);RotationTuple.get(rotation).applyRotationTo(offset);return p.add(offset.x,offset.y,offset.z);
    }
    static boolean clearLift(World w,FoundryRegistry.Claim claim) {
        BlockPos shaft=liftOrigin(claim.origin(),claim.rotation());
        for(BlockPos cell:claim.footprint().cells()) {
            if(empty(w,cell))continue;
            if(section(w,cell)==null)return false;
            BlockPos origin=origin(w,cell);
            if(origin.equals(claim.origin())&&claim.itemId().equals(itemId(block(w,origin))))continue;
            if(origin.equals(shaft)&&"FF_LiftAssembly".equals(itemId(block(w,origin))))continue;
            return false;
        }
        return true;
    }
    static Footprint placedFootprint(BlockType type,BlockPos p,int rot) {
        if(type==null)return Footprint.single(p);
        Set<BlockPos> cells=new HashSet<>();cells.add(p);var boxes=BlockBoundingBoxes.getAssetMap().getAsset(type.getHitboxTypeIndex());
        if(boxes!=null)FillerBlockUtil.forEachFillerBlock(boxes.get(rot),(x,y,z)->cells.add(p.add(x,y,z)));
        return new Footprint(cells);
    }
    static boolean clear(World w,Footprint footprint,BlockPos own) {
        for(BlockPos cell:footprint.cells()) {
            if(empty(w,cell))continue;
            if(own!=null&&section(w,cell)!=null&&origin(w,cell).equals(own))continue;
            return false;
        }
        return true;
    }
    static boolean state(World w,FoundryRegistry.Claim claim,String state) {
        BlockType current=block(w,claim.origin());if(!claim.itemId().equals(itemId(current)))return false;
        BlockType base=BlockType.getAssetMap().getAsset(claim.itemId());BlockType target=state.equals("default")?base:base.getBlockForState(state);
        if(target==null)return false;
        return set(w,claim.origin(),target,claim.rotation());
    }
    static boolean set(World w,BlockPos p,BlockType target,int rotation) {
        var chunks=w.getChunkStore();var ref=chunks.getChunkSectionReferenceAtBlock(p.x(),p.y(),p.z());
        if(ref==null||!ref.isValid())return false;
        BlockType current=block(w,p);
        if(current.getId().equals(target.getId()))return true;
        // Same native flags as the pinned interaction-state helper: preserve rotation, recalculate fillers, publish state.
        BlockOperations.setBlock(chunks,ref,p.x(),p.y(),p.z(),BlockType.getAssetMap().getIndex(target.getId()),target,rotation,0,198);
        BlockType after=block(w,p);return after!=null&&after.getId().equals(target.getId());
    }
}
