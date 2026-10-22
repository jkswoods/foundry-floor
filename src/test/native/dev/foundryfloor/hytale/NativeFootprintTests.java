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
        // Custom-model floors must satisfy the same native Full bottom-face
        // requirement as a stock workbench. Model collision alone supplies none.
        if(com.hypixel.hytale.builtin.blockphysics.BlockPhysicsUtil.doesMatchFaceType(new org.joml.Vector3i(),"Full",BlockFace.UP,Map.of()))throw new AssertionError("Missing support must reject a supported-only item");
        for(String name:List.of("FF_FloorPlate","FF_HazardFloor","FF_ConcreteFloor","FF_DrainGrate","FF_Catwalk","FF_CatwalkLeft","FF_CatwalkRight","FF_CatwalkBridge","FF_CatwalkCorner","FF_FactoryWall","FF_FactoryRoof","FF_FactoryWindow","FF_WindowPane","FF_SteelColumn")) {
            var item=JsonParser.parseString(Files.readString(Path.of("assets/Server/Item/Items/FoundryFloor/"+name+".json"))).getAsJsonObject();
            var base=item.getAsJsonObject("BlockType");List<JsonObject> variants=new ArrayList<>();variants.add(base);
            if(base.has("State"))for(var e:base.getAsJsonObject("State").getAsJsonObject("Definitions").entrySet())if(e.getKey().startsWith("Connect"))variants.add(e.getValue().getAsJsonObject());
            for(var variant:variants) {
            var up=(variant.has("Supporting")?variant:base).getAsJsonObject("Supporting").getAsJsonArray("Up");
            var supports=new BlockFaceSupport[up.size()];
            for(int i=0;i<supports.length;i++)supports[i]=BlockFaceSupport.CODEC.decode(org.bson.BsonDocument.parse(up.get(i).toString()),new com.hypixel.hytale.codec.ExtraInfo());
            for(Rotation yaw:Rotation.values()) {
                var rotated=Arrays.stream(supports).map(s->BlockFaceSupport.rotate(s,yaw,Rotation.None,Rotation.None)).toArray(BlockFaceSupport[]::new);
                if(!com.hypixel.hytale.builtin.blockphysics.BlockPhysicsUtil.doesMatchFaceType(new org.joml.Vector3i(),"Full",BlockFace.UP,Map.of(BlockFace.UP,rotated)))throw new AssertionError(name+" does not support a workbench at "+yaw);
                if(com.hypixel.hytale.builtin.blockphysics.BlockPhysicsUtil.doesMatchFaceType(new org.joml.Vector3i(0,1,0),"Full",BlockFace.UP,Map.of(BlockFace.UP,rotated)))throw new AssertionError(name+" incorrectly treats an upper rail/decorative filler as a full floor");
                checks++;
            }
            }
        }
        // Every open catwalk/deck variant ends exactly at the next cell's floor.
        // Thin bottom-aligned models left a 7/8-block gap below placed furniture.
        for(String stem:List.of("FF_Catwalk","FF_CatwalkLeft","FF_CatwalkRight","FF_CatwalkBridge","FF_CatwalkCorner","FF_DrainGrate"))for(int mask=0;mask<16;mask++) {
            String name=stem+(mask==0?"":String.format(Locale.ROOT,"_Connect%02X",mask));
            var array=JsonParser.parseString(Files.readString(Path.of("assets/Server/Item/Block/Hitboxes/FoundryFloor/"+name+".json"))).getAsJsonObject().getAsJsonArray("Boxes");
            var deck=array.get(0).getAsJsonObject();var lo=deck.getAsJsonObject("Min");var hi=deck.getAsJsonObject("Max");
            if(hi.get("Y").getAsDouble()!=1||hi.get("X").getAsDouble()!=1||hi.get("Z").getAsDouble()!=1||lo.get("X").getAsDouble()!=0||lo.get("Z").getAsDouble()!=0)throw new AssertionError(name+" has no continuous grid-height deck");
            checks++;
        }
        var root=new BlockPos(10,80,10);
        for(int i=0;i<=NativeDoorMotion.STEPS;i++) {
            var path=Path.of(String.format(Locale.ROOT,"assets/Server/Item/Block/Hitboxes/FoundryFloor/FF_PersonnelDoor_Pose%02d.json",i));
            var array=JsonParser.parseString(Files.readString(path)).getAsJsonObject().getAsJsonArray("Boxes");List<Box> detail=new ArrayList<>();
            for(var element:array) {var o=element.getAsJsonObject();var a=o.getAsJsonObject("Min");var b=o.getAsJsonObject("Max");detail.add(new Box(a.get("X").getAsDouble(),a.get("Y").getAsDouble(),a.get("Z").getAsDouble(),b.get("X").getAsDouble(),b.get("Y").getAsDouble(),b.get("Z").getAsDouble()));}
            for(Rotation yaw:Rotation.values()) {
                var rotation=RotationTuple.of(yaw,Rotation.None,Rotation.None);var forward=new org.joml.Vector3i(0,0,1);rotation.applyRotationTo(forward);cells.clear();
                FillerBlockUtil.forEachFillerBlock(new TestBoxes(detail.toArray(Box[]::new)).get(rotation.index()),(x,y,z)->cells.add(new BlockPos(x,y,z)));
                for(var cell:cells)if(cell.y()<0||cell.y()>1||!(cell.x()==0&&cell.z()==0||cell.x()==forward.x&&cell.z()==forward.z))throw new AssertionError("Door pose enters neighbouring wall: "+i+" "+yaw+" "+cell);
                checks++;
            }
        }
        for(long t=0;t<=NativeDoorMotion.DURATION;t+=50_000_000L) {
            int open=NativeDoorMotion.step(true,t),close=NativeDoorMotion.step(false,t);
            if(open+close!=NativeDoorMotion.STEPS||open<0||open>NativeDoorMotion.STEPS)throw new AssertionError("Door reverse timeline mismatch");checks++;
        }
        // A door must fit a 1x2 gap; controls and stairs must not reserve the
        // neighbouring wall/landing merely because a detail crosses a boundary.
        for(String name:List.of("FF_PersonnelDoor","FF_PersonnelDoor_Open","FF_RollerShutter","FF_CatwalkStairs","FF_CatwalkStairs_Connect02","FF_CatwalkStairs_Connect08","FF_CatwalkStairs_Connect0A","FF_SafetyRail","FF_PipeStraight","FF_PipeElbow")) {
            var array=JsonParser.parseString(Files.readString(Path.of("assets/Server/Item/Block/Hitboxes/FoundryFloor/"+name+".json"))).getAsJsonObject().getAsJsonArray("Boxes");
            List<Box> detail=new ArrayList<>();
            for(var element:array) {
                var o=element.getAsJsonObject();var a=o.getAsJsonObject("Min");var b=o.getAsJsonObject("Max");
                detail.add(new Box(a.get("X").getAsDouble(),a.get("Y").getAsDouble(),a.get("Z").getAsDouble(),b.get("X").getAsDouble(),b.get("Y").getAsDouble(),b.get("Z").getAsDouble()));
            }
            List<RotationTuple> rotations=new ArrayList<>();
            if(name.startsWith("FF_Pipe")) { rotations.add(RotationTuple.get(0));rotations.addAll(Arrays.asList(VariantRotation.All.getRotations())); }
            else for(Rotation yaw:Rotation.values())rotations.add(RotationTuple.of(yaw,Rotation.None,Rotation.None));
            for(var rotation:rotations) {
                cells.clear();
                FillerBlockUtil.forEachFillerBlock(new TestBoxes(detail.toArray(Box[]::new)).get(rotation.index()),(x,y,z)->cells.add(new BlockPos(x,y,z)));
                int width=name.equals("FF_RollerShutter")?1:0,height=name.equals("FF_RollerShutter")?2:name.startsWith("FF_PersonnelDoor")||name.startsWith("FF_CatwalkStairs")?1:0;
                // An open door projects into the clear front cell, while remaining
                // inside its doorway's width; the native fixture also checks walls.
                if(name.equals("FF_PersonnelDoor_Open")) {
                    var offset=new org.joml.Vector3i(0,0,1);rotation.applyRotationTo(offset);
                    for(var cell:cells)if(cell.y()<0||cell.y()>1||!(cell.x()==0&&cell.z()==0||cell.x()==offset.x&&cell.z()==offset.z))throw new AssertionError("Open door enters neighbouring wall: "+cell);
                    checks++;continue;
                }
                for(var cell:cells)if(Math.abs(cell.x())>width||Math.abs(cell.z())>width||cell.y()<0||cell.y()>height)throw new AssertionError(name+" leaks into adjacent building cells at "+rotation+": "+cell);
                checks++;
            }
        }
        var elbow=JsonParser.parseString(Files.readString(Path.of("assets/Server/Item/Items/FoundryFloor/FF_PipeElbow.json"))).getAsJsonObject().getAsJsonObject("BlockType");
        var variant=VariantRotation.valueOf(elbow.get("VariantRotation").getAsString());Set<String> portPairs=new HashSet<>();
        List<RotationTuple> elbowRotations=new ArrayList<>(Arrays.asList(variant.getRotations()));elbowRotations.add(RotationTuple.get(0));
        for(var rotation:elbowRotations) {
            var incoming=new org.joml.Vector3i(0,-1,0);var outgoing=new org.joml.Vector3i(1,0,0);rotation.applyRotationTo(incoming);rotation.applyRotationTo(outgoing);
            var ports=new TreeSet<String>();ports.add(incoming.toString());ports.add(outgoing.toString());portPairs.add(ports.toString());
        }
        if(portPairs.size()!=12)throw new AssertionError("Elbow rotation must reach all12 orthogonal face pairs without a helper block: "+portPairs.size());checks++;
        List<Path> poseFiles;
        try(var paths=Files.list(Path.of("assets/Server/Item/Block/Hitboxes/FoundryFloor"))) { poseFiles=paths.filter(path->path.getFileName().toString().matches("FF_Lift(H[0-9]+)?(Step|Dock)[0-9]+\\.json")).toList(); }
        if(poseFiles.size()!=745)throw new AssertionError("Expected745 native travel and dock shapes across five heights");checks++;
        for(Rotation yaw:Rotation.values()) {
            var rotation=RotationTuple.of(yaw,Rotation.None,Rotation.None);var shaft=NativeBlocks.liftOrigin(root,rotation.index());
            if(Math.abs(shaft.x()-root.x())+Math.abs(shaft.z()-root.z())!=3||shaft.y()!=root.y())throw new AssertionError("Controller/shaft offset rotation changed");checks++;
            for(var file:poseFiles) {
                var heightMatch=java.util.regex.Pattern.compile("FF_LiftH([0-9]+)").matcher(file.getFileName().toString());int height=heightMatch.find()?Integer.parseInt(heightMatch.group(1)):3;
                var array=JsonParser.parseString(Files.readString(file)).getAsJsonObject().getAsJsonArray("Boxes");List<Box> detail=new ArrayList<>();
                for(var element:array) {
                    var obj=element.getAsJsonObject();var min=obj.getAsJsonObject("Min");var max=obj.getAsJsonObject("Max");
                    detail.add(new Box(min.get("X").getAsDouble(),min.get("Y").getAsDouble(),min.get("Z").getAsDouble(),max.get("X").getAsDouble(),max.get("Y").getAsDouble(),max.get("Z").getAsDouble()));
                }
                var rotated=new TestBoxes(detail.toArray(Box[]::new)).get(rotation.index());cells.clear();FillerBlockUtil.forEachFillerBlock(rotated,(x,y,z)->cells.add(shaft.add(x,y,z)));
                for(var cell:cells)if(cell.equals(root)||Math.abs(cell.x()-shaft.x())>1||Math.abs(cell.z()-shaft.z())>1||cell.y()<shaft.y()||cell.y()>shaft.y()+height+3)throw new AssertionError("Lift pose writes outside reserved shaft: "+file+" "+yaw+" "+cell);
                if(cells.isEmpty())throw new AssertionError("Missing lift footprint");checks++;
            }
        }
        System.out.println(checks+" pinned native filler/threshold/legacy static-pose rotation checks passed.");
    }
}
