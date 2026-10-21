package dev.foundryfloor.hytale;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Resource;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.foundryfloor.core.*;
import java.io.IOException;
import java.util.*;

/** Native world resource. Ordinary Hytale world save/load retains ownership and mechanical state. */
public final class FoundryWorld implements Resource<EntityStore> {
    public static final BuilderCodec<FoundryWorld> CODEC=BuilderCodec.builder(FoundryWorld.class,FoundryWorld::new)
        .append(new KeyedCodec<>("Snapshot",Codec.STRING),FoundryWorld::read,FoundryWorld::write).add().build();
    FoundryRegistry registry=new FoundryRegistry();
    final ArrayDeque<Runnable> confirmedActions=new ArrayDeque<>();
    final Set<BlockPos> connectionUpdates=new HashSet<>();
    final Set<BlockPos> reconciledConnections=new HashSet<>();
    int connectionScan;
    final Map<BlockPos,Long> cooldowns=new HashMap<>();
    final Set<BlockPos> retiredReconciled=new HashSet<>();
    private String write() { return Base64.getEncoder().encodeToString(registry.snapshot()); }
    private void read(String value) {
        try { registry=FoundryRegistry.restore(Base64.getDecoder().decode(value)); }
        catch(IOException|IllegalArgumentException e) { throw new IllegalStateException("Cannot restore Foundry ownership/state; refusing to reset it",e); }
    }
    @Override public FoundryWorld clone() { FoundryWorld copy=new FoundryWorld();copy.read(write());return copy; }
}
