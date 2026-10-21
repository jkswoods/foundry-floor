package dev.foundryfloor.hytale;

import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.server.core.asset.type.blockhitbox.BlockBoundingBoxes;
import com.hypixel.hytale.server.core.util.FillerBlockUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.*;
import com.google.gson.*;
import dev.foundryfloor.core.BlockPos;
import java.nio.file.*;
import java.util.*;

/** Exercises the real pinned native filler utility without starting a server or accessing a world. */
public final class NativeFootprintTests {
    private static final class TestBoxes extends BlockBoundingBoxes {
        TestBoxes(Box[] boxes) { this.id="FF_Test";this.baseDetailBoxes=boxes;processConfig(); }
    }
    public static void main(String[] args) throws Exception {
        int checks=3;
        var unit=new BlockBoundingBoxes.RotatedVariantBoxes(new Box[]{new Box(0,0,0,1,1,1)});
        Set<BlockPos> cells=new HashSet<>();FillerBlockUtil.forEachFillerBlock(unit,(x,y,z)->cells.add(new BlockPos(x,y,z)));
        if(!cells.equals(Set.of(new BlockPos(0,0,0))))throw new AssertionError("Native unit footprint convention changed: "+cells);
        var wide=new BlockBoundingBoxes.RotatedVariantBoxes(new Box[]{new Box(-1,0,0,2,3,1)});
        cells.clear();FillerBlockUtil.forEachFillerBlock(wide,(x,y,z)->cells.add(new BlockPos(x,y,z)));
        if(cells.size()!=9||!cells.contains(new BlockPos(-1,2,0))||!cells.contains(new BlockPos(1,0,0)))throw new AssertionError("Native shutter footprint convention changed: "+cells);
        try { FillerBlockUtil.forEachFillerBlock(1f,unit,(x,y,z)->{});throw new AssertionError("Float argument is a threshold, not model scale"); }
        catch(IllegalArgumentException expected) { }
        var root=new BlockPos(10,80,10);
        List<Path> poseFiles;
        try(var paths=Files.list(Path.of("assets/Server/Item/Block/Hitboxes/FoundryFloor"))) { poseFiles=paths.filter(path->path.getFileName().toString().matches("FF_Lift(Step|Dock)[0-9]+\\.json")).toList(); }
        if(poseFiles.size()!=63)throw new AssertionError("Expected61 travel poses plus two dock shapes");checks++;
        for(Rotation yaw:Rotation.values()) {
            var rotation=RotationTuple.of(yaw,Rotation.None,Rotation.None);var shaft=NativeBlocks.liftOrigin(root,rotation.index());
            if(Math.abs(shaft.x()-root.x())+Math.abs(shaft.z()-root.z())!=3||shaft.y()!=root.y())throw new AssertionError("Controller/shaft offset rotation changed");checks++;
            for(var file:poseFiles) {
                var array=JsonParser.parseString(Files.readString(file)).getAsJsonObject().getAsJsonArray("Boxes");List<Box> detail=new ArrayList<>();
                for(var element:array) {
                    var obj=element.getAsJsonObject();var min=obj.getAsJsonObject("Min");var max=obj.getAsJsonObject("Max");
                    detail.add(new Box(min.get("X").getAsDouble(),min.get("Y").getAsDouble(),min.get("Z").getAsDouble(),max.get("X").getAsDouble(),max.get("Y").getAsDouble(),max.get("Z").getAsDouble()));
                }
                var rotated=new TestBoxes(detail.toArray(Box[]::new)).get(rotation.index());cells.clear();FillerBlockUtil.forEachFillerBlock(rotated,(x,y,z)->cells.add(shaft.add(x,y,z)));
                for(var cell:cells)if(cell.equals(root)||Math.abs(cell.x()-shaft.x())>1||Math.abs(cell.z()-shaft.z())>1||cell.y()<shaft.y()||cell.y()>shaft.y()+6)throw new AssertionError("Lift pose writes outside reserved shaft: "+file+" "+yaw+" "+cell);
                if(cells.isEmpty())throw new AssertionError("Missing lift footprint");checks++;
            }
        }
        System.out.println(checks+" pinned native filler/threshold/lift-pose rotation checks passed.");
    }
}
