package dev.foundryfloor.hytale;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blockhitbox.BlockBoundingBoxes;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.event.events.ecs.*;
import com.hypixel.hytale.server.core.modules.entity.component.*;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.*;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.*;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.foundryfloor.core.*;
import java.util.*;

/** Factory architecture and mechanical controls. No shared worlds/settings or other mods are modified. */
public final class FoundryPlugin extends JavaPlugin {
    static ResourceType<EntityStore,FoundryWorld> worldType;
    static final Set<String> CONTROLS=Set.of("FF_RollerShutter","FF_SafetyGate","FF_PersonnelDoor","FF_ServiceHatch","FF_WallLamp");
    static final Set<String> ITEMS=Set.of("FF_FloorPlate","FF_HazardFloor","FF_ConcreteFloor","FF_DrainGrate","FF_Catwalk","FF_CatwalkLeft","FF_CatwalkRight","FF_CatwalkBridge","FF_CatwalkCorner","FF_CatwalkStairs","FF_SafetyRail","FF_SteelColumn","FF_SteelBeam","FF_RollerShutter","FF_SafetyGate","FF_PersonnelDoor","FF_ServiceHatch","FF_FreightLift","FF_LiftGuide","FF_CallPanel","FF_PipeStraight","FF_PipeElbow","FF_WallVent","FF_WallLamp","FF_CargoPallet","FF_FoundryBench");
    public FoundryPlugin(JavaPluginInit init) { super(init); }
    @Override protected void setup() {
        worldType=getEntityStoreRegistry().registerResource(FoundryWorld.class,"FoundryFloorWorldV1",FoundryWorld.CODEC);
        getCodecRegistry(Interaction.CODEC).register("FF_Use",UseFoundry.class,UseFoundry.CODEC);
        getEntityStoreRegistry().registerSystem(new Tick());getEntityStoreRegistry().registerSystem(new Placement());getEntityStoreRegistry().registerSystem(new Breaking());
    }
    @Override protected void start() {
        for(String id:ITEMS)if(Item.getAssetMap().getAsset(id)==null||BlockType.getAssetMap().getAsset(id)==null)throw new IllegalStateException("Required Foundry asset missing: "+id);
        NativeCraftingChecks.verify();
        getLogger().atInfo().log("Foundry Floor0.1.0:26 original architecture/bench assets resolved; owned server controls registered. Development build.");
    }
    static FoundryWorld data(Store<EntityStore> store) { return store.getResource(worldType); }
    static void message(PlayerRef p,String m) { p.sendMessage(Message.raw(m)); }
    static void use(Store<EntityStore> store,PlayerRef player,BlockPos target) {
        World w=store.getExternalData().getWorld();BlockPos p=NativeBlocks.origin(w,target);FoundryWorld state=data(store);
        try {
            if(player.getTransform().getPosition().distanceSquared(target.x()+.5,target.y()+.5,target.z()+.5)>64)throw new IllegalArgumentException("Stand within eight blocks of the control");
            var claim=state.registry.owned(p,player.getUuid());
            if(!claim.itemId().equals(NativeBlocks.itemId(NativeBlocks.block(w,p))))throw new IllegalArgumentException("The saved Foundry assembly is unavailable");
            if(!CONTROLS.contains(claim.itemId()))throw new IllegalArgumentException("This assembly's controller is not ready in this development build");
            long now=System.nanoTime();if(now<state.cooldowns.getOrDefault(p,0L))return;
            boolean lamp=claim.itemId().equals("FF_WallLamp");String next=lamp?(claim.state().equals("Off")?"On":"Off"):(claim.state().equals("Open")?"Closed":"Open");
            if(!NativeBlocks.clear(w,claim.footprint(),p))throw new IllegalArgumentException("Clear the assembly's reserved movement space first");
            BlockType base=BlockType.getAssetMap().getAsset(claim.itemId()),nextType=base.getBlockForState(next);
            if(!lamp&&entityInClosingShape(store,claim,nextType))throw new IllegalArgumentException("Someone is in the mechanism's movement space");
            if(!NativeBlocks.state(w,claim,next))throw new IllegalStateException("Native state change could not be confirmed");
            state.registry.state(p,player.getUuid(),next,0);state.cooldowns.put(p,now+(claim.itemId().equals("FF_RollerShutter")?2_700_000_000L:1_000_000_000L));
            // Playback is requested only after the native state and owner record commit.
            String cue=claim.itemId().equals("FF_RollerShutter")?"shutter_travel":"mechanical_latch";play(store,p,cue);
        } catch(IllegalArgumentException|SecurityException|IllegalStateException ex) { message(player,ex.getMessage()); }
    }
    static boolean entityInClosingShape(Store<EntityStore> store,FoundryRegistry.Claim claim,BlockType target) {
        if(target==null)return true;
        var asset=BlockBoundingBoxes.getAssetMap().getAsset(target.getHitboxTypeIndex());if(asset==null)return true;
        var variant=asset.get(claim.rotation());var boxes=variant.hasDetailBoxes()?variant.getDetailBoxes():new com.hypixel.hytale.math.shape.Box[]{variant.getBoundingBox()};boolean[] blocked={false};var origin=claim.origin();
        store.forEachChunk(Query.and(TransformComponent.getComponentType(),BoundingBox.getComponentType()),(chunk,buffer)->{
            for(int i=0;i<chunk.size()&&!blocked[0];i++) {
                var pos=chunk.getComponent(i,TransformComponent.getComponentType()).getPosition();var body=chunk.getComponent(i,BoundingBox.getComponentType());
                var worldBox=new com.hypixel.hytale.math.shape.Box(body.getBoundingBox()).offset(pos);
                for(var box:boxes)if(new com.hypixel.hytale.math.shape.Box(box).offset(origin.x(),origin.y(),origin.z()).isIntersecting(worldBox)) { blocked[0]=true;break; }
            }
        });return blocked[0];
    }
    static void play(Store<EntityStore> store,BlockPos p,String cue) {
        var map=com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent.getAssetMap();String id="FF_SFX_"+cue;
        if(map.getAsset(id)!=null)SoundUtil.playSoundEvent3d(map.getIndex(id),com.hypixel.hytale.protocol.SoundCategory.SFX,p.x()+.5,p.y()+.5,p.z()+.5,store);
    }
    static final class Tick extends TickingSystem<EntityStore> {
        @Override public void tick(float dt,int index,Store<EntityStore> store) { var s=data(store);int n=Math.min(256,s.confirmedActions.size());for(int i=0;i<n;i++)s.confirmedActions.remove().run(); }
    }
    static final class Placement extends EntityEventSystem<EntityStore,PlaceBlockEvent> {
        Placement() { super(PlaceBlockEvent.class); }
        @Override public Query<EntityStore> getQuery() { return PlayerRef.getComponentType(); }
        @Override public void handle(int index,ArchetypeChunk<EntityStore> chunk,Store<EntityStore> store,CommandBuffer<EntityStore> buffer,PlaceBlockEvent event) {
            if(event.isCancelled()||event.getItemInHand()==null)return;
            var s=data(store);World w=store.getExternalData().getWorld();BlockPos p=NativeBlocks.pos(event.getTargetBlock());PlayerRef player=chunk.getComponent(index,PlayerRef.getComponentType());String id=event.getItemInHand().getItemId();
            if(!ITEMS.contains(id)) {
                String blockId=event.getItemInHand().getItem().getBlockId();BlockType type=blockId==null?null:BlockType.getAssetMap().getAsset(blockId);
                int rotation=event.getRotation()==null?0:event.getRotation().index();
                for(BlockPos cell:NativeBlocks.placedFootprint(type,p,rotation).cells())if(s.registry.reserved(cell)) { event.setCancelled(true);message(player,"A Foundry assembly reserves this movement space");break; }
                return;
            }
            if(event.getRotation()==null||s.confirmedActions.size()>=256) { event.setCancelled(true);return; }
            var footprint=NativeBlocks.footprint(id,p,event.getRotation().index());var claim=new FoundryRegistry.Claim(p,id,event.getRotation().index(),player.getUuid(),footprint,"default",0);
            if(!NativeBlocks.clear(w,footprint,null)||!s.registry.reserve(claim)) { event.setCancelled(true);message(player,"Clear the whole Foundry assembly footprint and load its surrounding chunks before placing it");return; }
            s.confirmedActions.add(()->{s.registry.finishPlacement(p,id.equals(NativeBlocks.itemId(NativeBlocks.block(w,p))));});
        }
    }
    static final class Breaking extends EntityEventSystem<EntityStore,BreakBlockEvent> {
        Breaking() { super(BreakBlockEvent.class); }
        @Override public Query<EntityStore> getQuery() { return PlayerRef.getComponentType(); }
        @Override public void handle(int index,ArchetypeChunk<EntityStore> chunk,Store<EntityStore> store,CommandBuffer<EntityStore> buffer,BreakBlockEvent event) {
            if(event.isCancelled())return;
            var s=data(store);World w=store.getExternalData().getWorld();BlockPos p=NativeBlocks.origin(w,NativeBlocks.pos(event.getTargetBlock()));var claim=s.registry.at(p);if(claim==null)return;
            PlayerRef player=chunk.getComponent(index,PlayerRef.getComponentType());
            if(!claim.owner().equals(player.getUuid())||claim.itemId().equals("FF_FreightLift")&&claim.liftStep()!=0||s.confirmedActions.size()>=256) { event.setCancelled(true);message(player,"Only the owner may remove this assembly; lower and empty a freight lift first");return; }
            s.confirmedActions.add(()->{if(!claim.itemId().equals(NativeBlocks.itemId(NativeBlocks.block(w,p)))) { s.registry.remove(p,player.getUuid());s.cooldowns.remove(p);s.lifts.remove(p); }});
        }
    }
}
