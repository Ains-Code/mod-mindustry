package mindustrytool.features.assistantbuilder;

import mindustry.content.Blocks;
import mindustry.world.Block;

public enum ArchitecturalStyle {
    GRID("@assistantbuilder.style.grid", 3, false, Blocks.titaniumWall),
    SYMMETRIC("@assistantbuilder.style.symmetric", 4, true, Blocks.titaniumWallLarge),
    COMPACT("@assistantbuilder.style.compact", 0, false, Blocks.copperWall);

    public final String nameKey;
    public final int spacing;
    public final boolean mirrored;
    public final Block skinWall;

    ArchitecturalStyle(String nameKey, int spacing, boolean mirrored, Block skinWall) {
        this.nameKey = nameKey;
        this.spacing = spacing;
        this.mirrored = mirrored;
        this.skinWall = skinWall;
    }
}
