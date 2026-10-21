package dev.foundryfloor.hytale;

import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.server.core.asset.type.blockhitbox.BlockBoundingBoxes;
import com.hypixel.hytale.server.core.util.FillerBlockUtil;
import dev.foundryfloor.core.BlockPos;
import java.util.*;

/** Exercises the real pinned native filler utility without starting a server or accessing a world. */
public final class NativeFootprintTests {
    public static void main(String[] args) {
        var unit=new BlockBoundingBoxes.RotatedVariantBoxes(new Box[]{new Box(0,0,0,1,1,1)});
        Set<BlockPos> cells=new HashSet<>();FillerBlockUtil.forEachFillerBlock(unit,(x,y,z)->cells.add(new BlockPos(x,y,z)));
        if(!cells.equals(Set.of(new BlockPos(0,0,0))))throw new AssertionError("Native unit footprint convention changed: "+cells);
        var wide=new BlockBoundingBoxes.RotatedVariantBoxes(new Box[]{new Box(-1,0,0,2,3,1)});
        cells.clear();FillerBlockUtil.forEachFillerBlock(wide,(x,y,z)->cells.add(new BlockPos(x,y,z)));
        if(cells.size()!=9||!cells.contains(new BlockPos(-1,2,0))||!cells.contains(new BlockPos(1,0,0)))throw new AssertionError("Native shutter footprint convention changed: "+cells);
        try { FillerBlockUtil.forEachFillerBlock(1f,unit,(x,y,z)->{});throw new AssertionError("Float argument is a threshold, not model scale"); }
        catch(IllegalArgumentException expected) { }
        System.out.println("3 pinned native filler/threshold checks passed.");
    }
}
