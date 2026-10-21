package dev.foundryfloor.core;

import java.util.*;

/** Conservatively reserved occupied/swept cells, already rotated by the native adapter. */
public record Footprint(Set<BlockPos> cells) {
    public Footprint { cells=Set.copyOf(cells);if(cells.isEmpty()||cells.size()>2048)throw new IllegalArgumentException("Invalid bounded assembly footprint"); }
    public static Footprint single(BlockPos p) { return new Footprint(Set.of(p)); }
}
