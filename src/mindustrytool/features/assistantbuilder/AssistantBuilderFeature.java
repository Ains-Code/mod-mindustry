package mindustrytool.features.assistantbuilder;

import java.util.Optional;

import arc.Core;
import arc.Events;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.scene.event.Touchable;
import arc.scene.ui.Dialog;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.entities.units.BuildPlan;
import mindustry.game.EventType.Trigger;
import mindustry.game.Schematic;
import mindustry.game.Schematic.Stile;
import mindustry.game.Team;
import mindustry.gen.Icon;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.Build;
import mindustry.world.blocks.defense.Wall;
import mindustrytool.MdtKeybinds;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

public class AssistantBuilderFeature implements Feature {
    private static final String STYLE_SETTING_KEY = "mindustrytool.assistantbuilder.style";
    private static final String ROW_COUNT_SETTING_KEY = "mindustrytool.assistantbuilder.row-count";
    private static final int MAX_ROW_COUNT = 10;

    private ArchitecturalStyle style = readSavedStyle();
    private AssistantBuilderSettingDialog settingDialog;
    private AssistantBuilderPickDialog pickDialog;
    private Schematic armed;
    private Table armedIndicator;

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
    public void init() {
        MdtKeybinds.addFeatureKeyBind(this, MdtKeybinds.assistantBuilderKb);

        Events.run(Trigger.update, () -> {
            if (!isEnabled() || armed == null) {
                return;
            }

            if (!Vars.state.isGame() || Core.scene.hasField() || Core.scene.hasDialog()) {
                return;
            }

            if (Core.input.keyTap(KeyCode.escape) || Core.input.keyTap(KeyCode.back)) {
                disarm();
                return;
            }

            if (Core.input.justTouched()) {
                var world = Core.input.mouseWorld();
                int tileX = Mathf.round(world.x / Vars.tilesize);
                int tileY = Mathf.round(world.y / Vars.tilesize);

                boolean placed = getRowCount() > 1
                        ? queueRow(armed, tileX, tileY, getRowCount())
                        : queueBuild(armed, tileX, tileY);

                if (placed) {
                    disarm();
                    Vars.ui.showInfoToast("@assistantbuilder.toast.queued", 2f);
                } else {
                    Vars.ui.showInfoToast("@assistantbuilder.toast.invalid", 2f);
                }
            }
        });

        Events.run(Trigger.draw, this::drawGhost);
    }

    @Override
    public void onEnable() {
        if (armedIndicator == null) {
            armedIndicator = buildArmedIndicator();
            Vars.ui.hudGroup.addChild(armedIndicator);
        }
    }

    @Override
    public void onDisable() {
        disarm();
    }

    @Override
    public Optional<Dialog> setting() {
        if (settingDialog == null) {
            settingDialog = new AssistantBuilderSettingDialog(this);
        }
        return Optional.of(settingDialog);
    }

    private Table buildArmedIndicator() {
        Table table = new Table(Styles.black6);
        table.touchable = Touchable.enabled;
        table.visible(() -> isArmed());
        table.top();
        table.margin(6f);

        table.label(() -> armed != null ? Core.bundle.format("assistantbuilder.armed.label", armed.name()) : "")
                .padRight(10);

        table.button(Icon.cancelSmall, Styles.emptyi, this::disarm).size(28f);

        table.pack();
        table.update(() -> table.setPosition(Core.graphics.getWidth() / 2f, Core.graphics.getHeight() - 12f,
                arc.util.Align.top));

        return table;
    }

    public void openPicker() {
        if (pickDialog == null) {
            pickDialog = new AssistantBuilderPickDialog(this);
        }
        pickDialog.show();
    }

    public void arm(Schematic schematic) {
        armed = schematic;
        Vars.ui.showInfoToast("@assistantbuilder.toast.armed", 2f);
    }

    public void disarm() {
        armed = null;
    }

    public boolean isArmed() {
        return armed != null;
    }

    public ArchitecturalStyle getStyle() {
        return style;
    }

    public void setStyle(ArchitecturalStyle style) {
        this.style = style;
        Core.settings.put(STYLE_SETTING_KEY, style.name());
    }

    public int getRowCount() {
        return Mathf.clamp(Core.settings.getInt(ROW_COUNT_SETTING_KEY, 1), 1, MAX_ROW_COUNT);
    }

    public void setRowCount(int count) {
        Core.settings.put(ROW_COUNT_SETTING_KEY, Mathf.clamp(count, 1, MAX_ROW_COUNT));
    }

    /** Queues a single (optionally mirrored) placement. Returns false and queues nothing if any tile is invalid. */
    public boolean queueBuild(Schematic schematic, int originX, int originY) {
        if (!isEnabled()) {
            return false;
        }

        var unit = Vars.player.unit();
        if (unit == null) {
            Vars.ui.showInfoToast("@assistantbuilder.toast.no-unit", 2f);
            return false;
        }

        int snappedX = snap(originX);
        int snappedY = snap(originY);

        Seq<BuildPlan> collected = new Seq<>();
        if (!collectPlacement(schematic, snappedX, snappedY, false, unit.team(), collected)) {
            return false;
        }

        if (style.mirrored) {
            int mirrorX = snap(snappedX + schematic.width + style.spacing);
            if (!collectPlacement(schematic, mirrorX, snappedY, true, unit.team(), collected)) {
                return false;
            }
        }

        collected.each(unit.plans::addLast);
        return true;
    }

    /** Queues {@code count} copies in a row, stopping (and rolling back) if any copy would be invalid. */
    public boolean queueRow(Schematic schematic, int originX, int originY, int count) {
        if (!isEnabled()) {
            return false;
        }

        var unit = Vars.player.unit();
        if (unit == null) {
            Vars.ui.showInfoToast("@assistantbuilder.toast.no-unit", 2f);
            return false;
        }

        int stepWidth = schematic.width + style.spacing;
        int step = style.mirrored ? stepWidth * 2 : stepWidth;

        Seq<BuildPlan> collected = new Seq<>();

        for (int i = 0; i < count; i++) {
            int cursorX = snap(originX + i * step);
            int snappedY = snap(originY);

            if (!collectPlacement(schematic, cursorX, snappedY, false, unit.team(), collected)) {
                return false;
            }

            if (style.mirrored) {
                int mirrorX = snap(cursorX + schematic.width + style.spacing);
                if (!collectPlacement(schematic, mirrorX, snappedY, true, unit.team(), collected)) {
                    return false;
                }
            }
        }

        collected.each(unit.plans::addLast);
        return true;
    }

    /**
     * Validates every tile of one schematic copy (bounds + {@link Build#validPlace}) and appends
     * matching {@link BuildPlan}s to {@code out} if the whole footprint is buildable.
     *
     * @return true if the copy was fully valid and appended; false if any tile was invalid (nothing appended)
     */
    private boolean collectPlacement(Schematic schematic, int originX, int originY, boolean mirror,
            Team team, Seq<BuildPlan> out) {
        for (Stile tile : schematic.tiles) {
            int localX = mirror ? (schematic.width - 1 - tile.x) : tile.x;
            int worldX = originX + localX;
            int worldY = originY + tile.y;

            if (worldX < 0 || worldY < 0 || worldX >= Vars.world.width() || worldY >= Vars.world.height()) {
                return false;
            }

            Block block = skin(tile.block);
            int rotation = mirror ? mirrorRotation(tile.rotation) : tile.rotation;

            if (!Build.validPlace(block, team, worldX, worldY, rotation)) {
                return false;
            }

            out.add(new BuildPlan(worldX, worldY, rotation, block, tile.config));
        }

        return true;
    }

    private boolean isPlacementValid(Schematic schematic, int originX, int originY, boolean mirror,
            Team team) {
        return collectPlacement(schematic, originX, originY, mirror, team, new Seq<>());
    }

    private int mirrorRotation(int rotation) {
        // Horizontal mirror: flip left/right facing rotations (1 = right becomes 3 = left, and vice versa).
        if (rotation == 1) {
            return 3;
        }
        if (rotation == 3) {
            return 1;
        }
        return rotation;
    }

    /** Only reskins blocks of the same footprint size, to avoid multi-tile blocks overlapping neighbours. */
    private Block skin(Block block) {
        if (block instanceof Wall && block.size == style.skinWall.size) {
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

    private void drawGhost() {
        if (armed == null || !Vars.state.isGame() || Core.scene.hasField() || Core.scene.hasDialog()) {
            return;
        }

        var unit = Vars.player.unit();
        if (unit == null) {
            return;
        }

        var world = Core.input.mouseWorld();
        int snappedX = snap(Mathf.round(world.x / Vars.tilesize));
        int snappedY = snap(Mathf.round(world.y / Vars.tilesize));

        Draw.z(Layer.overlayUI);

        drawGhostCopy(armed, snappedX, snappedY, false, unit.team());

        if (style.mirrored) {
            int mirrorX = snap(snappedX + armed.width + style.spacing);
            drawGhostCopy(armed, mirrorX, snappedY, true, unit.team());
        }

        Draw.reset();
    }

    private void drawGhostCopy(Schematic schematic, int originX, int originY, boolean mirror,
            Team team) {
        boolean valid = isPlacementValid(schematic, originX, originY, mirror, team);

        Lines.stroke(1.5f, valid ? Pal.accent : Pal.remove);

        float worldX = (originX - 0.5f) * Vars.tilesize;
        float worldY = (originY - 0.5f) * Vars.tilesize;

        Lines.rect(worldX, worldY, schematic.width * Vars.tilesize, schematic.height * Vars.tilesize);
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
