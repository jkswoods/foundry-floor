package dev.foundryfloor.core;

/** Integer block position independent of the server runtime. */
public record BlockPos(int x, int y, int z) implements Comparable<BlockPos> {
    public BlockPos add(int dx, int dy, int dz) { return new BlockPos(Math.addExact(x,dx),Math.addExact(y,dy),Math.addExact(z,dz)); }
    @Override public int compareTo(BlockPos p) { int c=Integer.compare(x,p.x);if(c==0)c=Integer.compare(y,p.y);if(c==0)c=Integer.compare(z,p.z);return c; }
}
