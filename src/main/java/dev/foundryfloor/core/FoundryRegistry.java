package dev.foundryfloor.core;

import java.io.*;
import java.util.*;

/** Server-owned placement claims. Pending native placements never enter the saved snapshot. */
public final class FoundryRegistry {
    public static final int MAX_BLOCKS=16384;
    public record Claim(BlockPos origin,String itemId,int rotation,UUID owner,Footprint footprint,String state,int liftStep,Set<UUID> passengers,BlockPos linkedTo,int travelBlocks,int panelStop) {
        public Claim(BlockPos origin,String itemId,int rotation,UUID owner,Footprint footprint,String state,int liftStep) {
            this(origin,itemId,rotation,owner,footprint,state,liftStep,Set.of(),null,3,-1);
        }
        public Claim(BlockPos origin,String itemId,int rotation,UUID owner,Footprint footprint,String state,int liftStep,Set<UUID> passengers,BlockPos linkedTo) { this(origin,itemId,rotation,owner,footprint,state,liftStep,passengers,linkedTo,3,-1); }
        public Claim(BlockPos origin,String itemId,int rotation,UUID owner,Footprint footprint,String state,int liftStep,Set<UUID> passengers,BlockPos linkedTo,int travelBlocks) { this(origin,itemId,rotation,owner,footprint,state,liftStep,passengers,linkedTo,travelBlocks,-1); }
        public Claim {
            Objects.requireNonNull(origin);Objects.requireNonNull(owner);Objects.requireNonNull(footprint);
            passengers=Set.copyOf(passengers);
            if(passengers.size()>8)throw new IllegalArgumentException("Lift carries at most eight registered passengers");
            if(!itemId.matches("FF_[A-Za-z0-9_]+")||itemId.length()>100||rotation<0||rotation>=64||state.length()>50||travelBlocks<3||travelBlocks>11||travelBlocks%2!=1||liftStep<0||liftStep>travelBlocks*20||panelStop< -1||panelStop>11)throw new IllegalArgumentException("Invalid Foundry claim");
        }
        public Claim withState(String next,int step) { return new Claim(origin,itemId,rotation,owner,footprint,next,step,passengers,linkedTo,travelBlocks,panelStop); }
    }
    private final Map<BlockPos,Claim> blocks=new TreeMap<>();
    private final Map<BlockPos,Claim> pending=new HashMap<>();
    private final Map<BlockPos,BlockPos> occupied=new HashMap<>();
    public synchronized Collection<Claim> claims() { return List.copyOf(blocks.values()); }
    public synchronized Claim at(BlockPos p) { return blocks.get(p); }
    public synchronized Claim occupying(BlockPos p) { return blocks.get(occupied.get(p)); }
    public synchronized boolean reserved(BlockPos p) { return occupied.containsKey(p); }
    public synchronized boolean reserve(Claim claim) {
        if(blocks.size()+pending.size()>=MAX_BLOCKS||blocks.containsKey(claim.origin)||pending.containsKey(claim.origin))return false;
        for(BlockPos p:claim.footprint.cells())if(occupied.containsKey(p))return false;
        pending.put(claim.origin,claim);for(BlockPos p:claim.footprint.cells())occupied.put(p,claim.origin);return true;
    }
    public synchronized void finishPlacement(BlockPos p,boolean nativePlaced) {
        Claim c=pending.remove(p);if(c==null)return;
        if(nativePlaced)blocks.put(p,c);else for(BlockPos cell:c.footprint.cells())occupied.remove(cell,p);
    }
    public synchronized void remove(BlockPos p,UUID actor) {
        Claim c=owned(p,actor);blocks.remove(p);for(BlockPos cell:c.footprint.cells())occupied.remove(cell,p);
    }
    public synchronized Claim owned(BlockPos p,UUID actor) {
        Claim c=blocks.get(p);if(c==null)throw new IllegalArgumentException("This Foundry block is not registered");
        if(!c.owner.equals(actor))throw new SecurityException("This Foundry block belongs to another player");return c;
    }
    public synchronized void state(BlockPos p,UUID actor,String state,int step) { Claim c=owned(p,actor);blocks.put(p,c.withState(state,step)); }
    public synchronized void passengers(BlockPos p,UUID actor,Set<UUID> passengers) { Claim c=owned(p,actor);blocks.put(p,new Claim(c.origin,c.itemId,c.rotation,c.owner,c.footprint,c.state,c.liftStep,passengers,c.linkedTo,c.travelBlocks,c.panelStop)); }
    public synchronized void link(BlockPos p,UUID actor,BlockPos target) { Claim c=owned(p,actor);blocks.put(p,new Claim(c.origin,c.itemId,c.rotation,c.owner,c.footprint,c.state,c.liftStep,c.passengers,target,c.travelBlocks,c.panelStop)); }
    public synchronized boolean footprint(BlockPos p,UUID actor,Footprint footprint) {
        Claim c=owned(p,actor);
        for(BlockPos cell:footprint.cells())if(occupied.containsKey(cell)&&!occupied.get(cell).equals(p))return false;
        for(BlockPos cell:c.footprint.cells())occupied.remove(cell,p);
        for(BlockPos cell:footprint.cells())occupied.put(cell,p);
        blocks.put(p,new Claim(c.origin,c.itemId,c.rotation,c.owner,footprint,c.state,c.liftStep,c.passengers,c.linkedTo,c.travelBlocks,c.panelStop));return true;
    }
    public synchronized byte[] snapshot() {
        try(var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes)) {
            out.writeInt(0x46465234);out.writeInt(blocks.size());
            for(Claim c:blocks.values()) {
                pos(out,c.origin);out.writeUTF(c.itemId);out.writeInt(c.rotation);out.writeLong(c.owner.getMostSignificantBits());out.writeLong(c.owner.getLeastSignificantBits());out.writeUTF(c.state);out.writeInt(c.liftStep);
                var cells=new TreeSet<>(c.footprint.cells());out.writeInt(cells.size());for(BlockPos p:cells)pos(out,p);
                var riders=new TreeSet<>(c.passengers);out.writeInt(riders.size());for(UUID rider:riders) { out.writeLong(rider.getMostSignificantBits());out.writeLong(rider.getLeastSignificantBits()); }
                out.writeBoolean(c.linkedTo!=null);if(c.linkedTo!=null)pos(out,c.linkedTo);
                out.writeInt(c.travelBlocks);out.writeInt(c.panelStop);
            }
            return bytes.toByteArray();
        } catch(IOException e) { throw new UncheckedIOException(e); }
    }
    public static FoundryRegistry restore(byte[] bytes) throws IOException {
        if(bytes.length>16*1024*1024)throw new IOException("Foundry snapshot exceeds bound");
        try(var in=new DataInputStream(new ByteArrayInputStream(bytes))) {
            int format=in.readInt();if(format!=0x46465231&&format!=0x46465232&&format!=0x46465233&&format!=0x46465234)throw new IOException("Unsupported Foundry snapshot");
            int count=in.readInt();if(count<0||count>MAX_BLOCKS)throw new IOException("Invalid Foundry block count");FoundryRegistry r=new FoundryRegistry();
            for(int i=0;i<count;i++) {
                BlockPos p=pos(in);String id=in.readUTF();int rot=in.readInt();UUID owner=new UUID(in.readLong(),in.readLong());String state=in.readUTF();int step=in.readInt();int n=in.readInt();
                if(n<1||n>2048)throw new IOException("Invalid assembly footprint size");Set<BlockPos> cells=new HashSet<>();for(int j=0;j<n;j++)if(!cells.add(pos(in)))throw new IOException("Duplicate assembly cell");
                Set<UUID> riders=new HashSet<>();BlockPos linked=null;
                if(format>=0x46465232) {
                    int size=in.readInt();if(size<0||size>8)throw new IOException("Invalid passenger count");
                    for(int j=0;j<size;j++)if(!riders.add(new UUID(in.readLong(),in.readLong())))throw new IOException("Duplicate passenger ticket");
                    if(in.readBoolean())linked=pos(in);
                }
                int height=format>=0x46465233?in.readInt():3;int stop=format>=0x46465234?in.readInt():-1;
                Claim c=new Claim(p,id,rot,owner,new Footprint(cells),state,step,riders,linked,height,stop);if(!r.reserve(c))throw new IOException("Overlapping or duplicate saved Foundry assembly");r.finishPlacement(p,true);
            }
            if(in.read()!=-1)throw new IOException("Trailing Foundry snapshot bytes");return r;
        } catch(IllegalArgumentException e) { throw new IOException("Invalid Foundry snapshot",e); }
    }
    private static void pos(DataOutputStream out,BlockPos p) throws IOException { out.writeInt(p.x());out.writeInt(p.y());out.writeInt(p.z()); }
    private static BlockPos pos(DataInputStream in) throws IOException { return new BlockPos(in.readInt(),in.readInt(),in.readInt()); }
}
