package dev.foundryfloor.core;

/** Bounded four-second, three-block lift travel. Native motion/collision must commit each step. */
public final class FreightMotion {
    public enum State { LOWERED, RAISING, RAISED, LOWERING, BLOCKED, DISABLED }
    public interface StepCommit { boolean commit(int before,int after); }
    private int step,target;
    private double accumulator;
    private State state;
    public FreightMotion(int step) {
        if(step<0||step>60)throw new IllegalArgumentException("Lift step outside its shaft");this.step=step;target=step;
        state=step==0?State.LOWERED:step==60?State.RAISED:State.DISABLED;
    }
    public int step() { return step; }
    public double offset() { return step/20.0; }
    public State state() { return state; }
    public boolean moving() { return state==State.RAISING||state==State.LOWERING; }
    public boolean request(boolean upper) {
        int requested=upper?60:0;
        if(moving()||step==requested)return false;
        target=requested;state=target>step?State.RAISING:State.LOWERING;accumulator=0;return true;
    }
    public void disable() { state=State.DISABLED;accumulator=0; }
    public void block() { state=State.BLOCKED;accumulator=0; }
    public void advance(double dt,StepCommit commit) {
        if(!Double.isFinite(dt)||dt<0)throw new IllegalArgumentException("Invalid world tick");
        if(!moving())return;
        // Never skip collision volumes or catch up a large unloaded-time backlog.
        accumulator=Math.min(.2,accumulator+Math.min(.1,dt));
        while(accumulator>=1.0/15&&moving()) {
            accumulator-=1.0/15;int next=step+Integer.compare(target,step);
            if(!commit.commit(step,next)) { block();return; }
            step=next;
            if(step==target) { state=step==0?State.LOWERED:State.RAISED;accumulator=0; }
        }
    }
}
