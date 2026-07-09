package mindustrytool.features.assistantbuilder;

import java.util.Optional;

import arc.Core;
import arc.scene.ui.Dialog;
import mindustry.Vars;
import mindustry.entities.units.BuildPlan;
import mindustry.game.Schematic;
import mindustry.game.Schematic.Stile;
import mindustry.gen.Icon;
import mindustry.world.Block;
import mindustry.world.blocks.defense.Wall;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

public class AssistantBuilderFeature implements Feature {
    private static final String STYLE_SETTING_KEY = "mindustrytool.assistantbuilder.style";

    private ArchitecturalStyle style = readSavedStyle();
    private AssistantBuilderSettingDialog dialog;

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("@feature.assistant-builder")
                .description("@feature.assistant-builder.description")
                .icon(Icon.hammer)
                .order(3)
                .enabledByDefault(false)
                .build();
    }

    @Override
    public Optional<Dialog> setting() {
        if (dialog == null) {
            dialog = new AssistantBuilderSettingDialog(this);
        }
        return Optional.of(dialog);
    }

    public ArchitecturalStyle getStyle() {
        return style;
    }

    public void setStyle(ArchitecturalStyle style) {
        this.style = style;
        Core.settings.put(STYLE_SETTING_KEY, style.name());
    }

    public void queueBuild(Schematic schematic, int originX, int originY) {
        if (!isEnabled()) {
            return;
        }

        int snappedX = snap(originX);
        int snappedY = snap(originY);

        queuePlacement(schematic, snappedX, snappedY, false);

        if (style.mirrored) {
            int mirrorX = snap(snappedX + schematic.width + style.spacing);
            queuePlacement(schematic, mirrorX, snappedY, true);
        }
    }

    public void queueRow(Schematic schematic, int originX, int originY, int count) {
        int stepWidth = schematic.width + style.spacing;
        int cursor = originX;

        for (int i = 0; i < count; i++) {
            queueBuild(schematic, cursor, originY);
            cursor += style.mirrored ? stepWidth * 2 : stepWidth;
        }
    }

    private void queuePlacement(Schematic schematic, int originX, int originY, boolean mirror) {
        var unit = Vars.player.unit();

        if (unit == null) {
            return;
        }

        var plans = unit.team.data().plans;

        for (Stile tile : schematic.tiles) {
            int localX = mirror ? (schematic.width - 1 - tile.x) : tile.x;
            int worldX = originX + localX;
            int worldY = originY + tile.y;

            Block block = skin(tile.block);

            plans.addLast(new BuildPlan(worldX, worldY, tile.rotation, block, tile.config));
        }
    }

    private Block skin(Block block) {
        if (block instanceof Wall) {
            return style.skinWall;
        }

        return block;
    }

    private int snap(int coord) {
        if (style.spacing <= 0) {
            return coord;
        }

        return Math.round(coord / (float) style.spacing) * style.spacing;
    }

    private static ArchitecturalStyle readSavedStyle() {
        String saved = Core.settings.getString(STYLE_SETTING_KEY, ArchitecturalStyle.GRID.name());

        try {
            return ArchitecturalStyle.valueOf(saved);
        } catch (IllegalArgumentException e) {
            return ArchitecturalStyle.GRID;
        }
    }
}
