package dev.foundryfloor.hytale;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.foundryfloor.core.*;
import java.util.*;

/** Loaded-neighbour variants; never loads chunks or replaces other blocks. */
final class NativeConnections {
    private static final Set<String> GRATES=Set.of("FF_Catwalk","FF_CatwalkLeft","FF_CatwalkRight","FF_CatwalkBridge","FF_CatwalkCorner","FF_DrainGrate");
    private static final Set<String> CONNECTED=Set.of("FF_Catwalk","FF_CatwalkLeft","FF_CatwalkRight","FF_CatwalkBridge","FF_CatwalkCorner","FF_DrainGrate","FF_HazardFloor","FF_CatwalkStairs","FF_WallVent","FF_CargoPallet");
    private NativeConnections() {}
    static void changed(FoundryWorld data,BlockPos p) {
        data.connectionUpdates.add(p);
        for(int[] d:new int[][]{{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}})data.connectionUpdates.add(p.add(d[0],d[1],d[2]));
    }
    static void tick(Store<EntityStore> store) {
        var data=FoundryPlugin.data(store);World world=store.getExternalData().getWorld();
        var claims=new ArrayList<>(data.registry.claims());
        int count=Math.min(64,claims.size());
        for(int i=0;i<count;i++) {
            var c=claims.get(Math.floorMod(data.connectionScan++,claims.size()));
            if(data.reconciledConnections.contains(c.origin())||NativeBlocks.section(world,c.origin())==null)continue;
            if(!c.itemId().equals(NativeBlocks.itemId(NativeBlocks.block(world,c.origin()))))continue;
            if(!NativeBlocks.repairLegacyFillers(world,data.registry,c))continue;
            // Old development saves reserved a door's open space and oversized decoration.
            // Recompute against the current base asset once its actual block is loaded.
            if(!c.itemId().equals("FF_FreightLift"))data.registry.footprint(c.origin(),c.owner(),NativeBlocks.footprint(c.itemId(),c.origin(),c.rotation()));
            data.reconciledConnections.add(c.origin());changed(data,c.origin());
        }
        var work=data.connectionUpdates.iterator();List<BlockPos> batch=new ArrayList<>();
        while(work.hasNext()&&batch.size()<128) { batch.add(work.next());work.remove(); }
        for(BlockPos p:batch)refresh(world,data,p);
    }
    private static void refresh(World world,FoundryWorld data,BlockPos p) {
        var c=data.registry.at(p);if(c==null||!CONNECTED.contains(c.itemId())||!c.itemId().equals(NativeBlocks.itemId(NativeBlocks.block(world,p))))return;
        int mask=0;
        int[][] offsets=c.itemId().equals("FF_WallVent")?new int[][]{{0,1,0},{1,0,0},{0,-1,0},{-1,0,0}}:new int[][]{{0,0,-1},{1,0,0},{0,0,1},{-1,0,0}};
        for(int bit=0;bit<4;bit++) {
            var d=new org.joml.Vector3i(offsets[bit]);RotationTuple.get(c.rotation()).applyRotationTo(d);var neighbour=p.add(d.x,d.y,d.z);
            String id=NativeBlocks.itemId(NativeBlocks.block(world,neighbour));
            boolean compatible=id!=null&&(GRATES.contains(c.itemId())?GRATES.contains(id):c.itemId().equals(id));
            if(compatible&&(c.itemId().equals("FF_CatwalkStairs")||c.itemId().equals("FF_WallVent")))compatible=NativeBlocks.rotation(world,neighbour)==c.rotation();
            if(compatible)mask|=1<<bit;
        }
        NativeBlocks.state(world,c,String.format(Locale.ROOT,"Connect%02X",mask));
    }
}
