package mindustrytool.features.assistantbuilder;

import arc.struct.Seq;
import arc.struct.StringMap;
import mindustry.content.Blocks;
import mindustry.game.Schematic;
import mindustry.game.Schematic.Stile;

/**
 * Hand-designed schematics bundled with the mod itself, so Assistant Builder
 * has something sensible to auto-expand even if the player hasn't saved any
 * schematics yet.
 *
 * <p>
 * These are intentionally terrain-agnostic (no drills/ore dependency) so that
 * automatic placement anywhere near the core is reliably useful — ore-vein
 * aware layouts are a much harder problem (need vein detection) and are left
 * to the player's own saved schematics for now.
 * </p>
 */
public final class BuiltinSchematics {
    private BuiltinSchematics() {
    }

    /** A single relay tile — tiles into a power line/grid when repeated. */
    public static Schematic powerRelay() {
        Seq<Stile> tiles = new Seq<>();
        tiles.add(new Stile(Blocks.powerNode, 0, 0, null, (byte) 0));

        return build(tiles, 1, 1, "Power Relay",
                "Extends the power grid. Always buildable, no fuel or ore needed.");
    }

    /** A 3-wide wall-turret-wall section — tiles into a perimeter defense line when repeated. */
    public static Schematic wallTurretSection() {
        Seq<Stile> tiles = new Seq<>();
        tiles.add(new Stile(Blocks.copperWall, 0, 0, null, (byte) 0));
        tiles.add(new Stile(Blocks.duo, 1, 0, null, (byte) 0));
        tiles.add(new Stile(Blocks.copperWall, 2, 0, null, (byte) 0));

        return build(tiles, 3, 1, "Wall & Turret Section",
                "A defensive line segment: wall, turret, wall. Repeats into a perimeter.");
    }

    /** A mender flanked by walls — tiles into a repair-support line for a wall perimeter. */
    public static Schematic menderOutpost() {
        Seq<Stile> tiles = new Seq<>();
        tiles.add(new Stile(Blocks.copperWall, 0, 0, null, (byte) 0));
        tiles.add(new Stile(Blocks.mender, 1, 0, null, (byte) 0));
        tiles.add(new Stile(Blocks.copperWall, 2, 0, null, (byte) 0));

        return build(tiles, 3, 1, "Mender Outpost",
                "Keeps nearby walls and turrets repaired automatically.");
    }

    public static Seq<Schematic> all() {
        return Seq.with(powerRelay(), wallTurretSection(), menderOutpost());
    }

    private static Schematic build(Seq<Stile> tiles, int width, int height, String name, String description) {
        StringMap tags = new StringMap();
        tags.put("name", name);
        tags.put("description", description);

        return new Schematic(tiles, tags, width, height);
    }
}
