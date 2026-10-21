package dev.foundryfloor.core;

import java.io.IOException;
import java.util.*;

/** Changed-path conservation/ownership/persistence and lift safety tests; no game process. */
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
        FreightMotion lift=new FreightMotion(0);check(lift.request(true),"upper call accepted");check(!lift.request(false),"in-flight competing call rejected");
        List<Integer> path=new ArrayList<>();for(int i=0;i<200;i++)lift.advance(1.0/30,(a,b)->{check(b-a==1,"no skipped upward collision steps");path.add(b);return true;});
        check(lift.step()==60&&lift.offset()==3&&lift.state()==FreightMotion.State.RAISED&&path.size()==60,"bounded three-block arrival");
        check(!lift.request(true),"repeat arrival request has no effect");check(lift.request(false),"lower call accepted");
        for(int i=0;i<12;i++)lift.advance(.1,(a,b)->b>=50);
        check(lift.step()==50&&lift.state()==FreightMotion.State.BLOCKED,"obstruction stops before unsafe collision commit");
        double held=lift.offset();for(int i=0;i<100;i++)lift.advance(.1,(a,b)->true);check(lift.offset()==held,"blocked platform stays physical at its last committed height");
        FreightMotion reload=new FreightMotion(lift.step());check(reload.state()==FreightMotion.State.DISABLED&&reload.step()==50,"mid-travel reload holds, never teleports to a stop");
        check(reload.request(false),"clear lower request resumes from held height");for(int i=0;i<200;i++)reload.advance(1.0/30,(a,b)->true);
        check(reload.step()==0&&reload.state()==FreightMotion.State.LOWERED,"resume reaches exact lower stop");
        check(reload.request(true),"new trip");reload.disable();reload.advance(20,(a,b)->{throw new AssertionError("disabled travel committed");});check(reload.step()==0&&!reload.moving(),"unload/disconnect disables movement without catch-up");
        System.out.println(checks+" Foundry core invariants passed.");
    }
}
