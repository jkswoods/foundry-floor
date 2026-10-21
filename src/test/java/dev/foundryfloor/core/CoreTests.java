package dev.foundryfloor.core;

import java.io.*;
import java.util.*;

/** Changed-path conservation/ownership/persistence and legacy data tests; no game process. */
public final class CoreTests {
    private static int checks;
    private static void check(boolean b,String why) { if(!b)throw new AssertionError(why);checks++; }
    private static FoundryRegistry.Claim claim(BlockPos p,UUID owner,Footprint footprint) { return new FoundryRegistry.Claim(p,"FF_SafetyGate",0,owner,footprint,"Closed",0); }
    public static void main(String[] args) throws Exception {
        UUID owner=UUID.randomUUID(),other=UUID.randomUUID();BlockPos p=new BlockPos(1,70,-2),q=p.add(1,0,0);FoundryRegistry r=new FoundryRegistry();
        check(r.reserve(claim(p,owner,new Footprint(Set.of(p,q)))),"clear assembly reserved");
        check(!r.reserve(claim(q,other,Footprint.single(q))),"overlap refused before item consumption");
        check(FoundryRegistry.restore(r.snapshot()).claims().isEmpty(),"unconfirmed placement never saves");
        r.finishPlacement(p,false);check(!r.reserved(q),"failed native placement releases sweep reservation");
        check(r.reserve(claim(p,owner,new Footprint(Set.of(p,q)))),"retry clear assembly");r.finishPlacement(p,true);
        check(r.occupying(q).origin().equals(p),"filler targets resolve owner claim");
        try { r.state(p,other,"Open",0);throw new AssertionError("foreign use accepted"); } catch(SecurityException expected) { checks++; }
        r.state(p,owner,"Open",0);FoundryRegistry saved=FoundryRegistry.restore(r.snapshot());
        check(saved.at(p).state().equals("Open")&&saved.at(p).owner().equals(owner),"state/owner normal snapshot round trip");
        check(Arrays.equals(r.snapshot(),saved.snapshot()),"snapshot deterministic");
        byte[] corrupt=r.snapshot();corrupt[0]=0;try { FoundryRegistry.restore(corrupt);throw new AssertionError("corrupt snapshot reset silently"); } catch(IOException expected) { checks++; }
        saved.remove(p,owner);check(saved.claims().isEmpty()&&!saved.reserved(q),"one removal clears all sweep claims");
        Set<UUID> passengers=Set.of(UUID.randomUUID(),UUID.randomUUID());
        r.passengers(p,owner,passengers);r.link(p,owner,q);r.state(p,owner,"Disabled",23);
        var loaded=FoundryRegistry.restore(r.snapshot()).at(p);
        check(loaded.passengers().equals(passengers)&&loaded.linkedTo().equals(q)&&loaded.liftStep()==23,"passenger tickets/link/exact held height save together");
        r.state(p,owner,"Blocked",23);check(r.at(p).passengers().equals(passengers)&&r.at(p).linkedTo().equals(q),"state update preserves safety tickets/link");
        try { r.passengers(p,other,Set.of());throw new AssertionError("foreign passenger mutation"); }catch(SecurityException expected) { checks++; }
        try { r.link(p,other,null);throw new AssertionError("foreign panel relink"); }catch(SecurityException expected) { checks++; }
        Set<UUID> tooMany=new HashSet<>();for(int i=0;i<9;i++)tooMany.add(UUID.randomUUID());
        try { r.passengers(p,owner,tooMany);throw new AssertionError("unbounded passenger set"); }catch(IllegalArgumentException expected) { checks++; }
        try(var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes)) {
            // F3A v1 is retained; migration must never erase ownership or movement reservations.
            out.writeInt(0x46465231);out.writeInt(1);out.writeInt(p.x());out.writeInt(p.y());out.writeInt(p.z());out.writeUTF("FF_SafetyGate");out.writeInt(0);out.writeLong(owner.getMostSignificantBits());out.writeLong(owner.getLeastSignificantBits());out.writeUTF("Open");out.writeInt(0);out.writeInt(1);out.writeInt(p.x());out.writeInt(p.y());out.writeInt(p.z());
            var legacy=FoundryRegistry.restore(bytes.toByteArray()).at(p);
            check(legacy.owner().equals(owner)&&legacy.state().equals("Open")&&legacy.passengers().isEmpty()&&legacy.linkedTo()==null,"v1 saves migrate ownership/state without inventing passenger tickets");
        }
        var registry=new FoundryRegistry();var liftClaim=new FoundryRegistry.Claim(p,"FF_FreightLift",0,owner,new Footprint(Set.of(p,q)),"Lowered",0);
        check(registry.reserve(liftClaim),"height fixture reserved");registry.finishPlacement(p,true);
        check(registry.footprint(p,owner,Footprint.single(p))&&!registry.reserved(q),"resize releases old decorative reservation");
        check(registry.reserve(claim(q,other,Footprint.single(q))),"released wall cell reusable");registry.finishPlacement(q,true);
        check(!registry.footprint(p,owner,new Footprint(Set.of(p,q)))&&!registry.reserved(p.add(0,1,0)),"resize cannot steal another assembly");
        registry.state(p,owner,"Disabled",59);
        check(FoundryRegistry.restore(registry.snapshot()).at(p).travelBlocks()==3&&FoundryRegistry.restore(registry.snapshot()).at(p).liftStep()==59,"retired travel and held height remain readable together");
        var panelPos=p.add(10,0,0);var panel=new FoundryRegistry.Claim(panelPos,"FF_CallPanel",0,owner,Footprint.single(panelPos),"Disabled",0,Set.of(),p,3,4);
        check(registry.reserve(panel),"legacy panel reserved");registry.finishPlacement(panelPos,true);
        var restored=FoundryRegistry.restore(registry.snapshot());check(restored.at(panelPos).panelStop()==4&&restored.at(panelPos).linkedTo().equals(p),"FFR4 retired link and landing remain readable");
        // Exact old FFR3 bytes: height is retained; old panels are deliberately unprogrammed.
        try(var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes)) {
            out.writeInt(0x46465233);out.writeInt(1);out.writeInt(p.x());out.writeInt(p.y());out.writeInt(p.z());out.writeUTF("FF_CallPanel");out.writeInt(0);out.writeLong(owner.getMostSignificantBits());out.writeLong(owner.getLeastSignificantBits());out.writeUTF("default");out.writeInt(0);out.writeInt(1);out.writeInt(p.x());out.writeInt(p.y());out.writeInt(p.z());out.writeInt(0);out.writeBoolean(false);out.writeInt(7);
            var old=FoundryRegistry.restore(bytes.toByteArray()).at(p);check(old.travelBlocks()==7&&old.panelStop()==-1&&old.owner().equals(owner),"FFR3 migration retains capacity and ownership without guessing a landing");
        }
        System.out.println(checks+" Foundry core invariants passed.");
    }
}
