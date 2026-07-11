package modifiedtools.features.assistantbuilder;

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
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType.Trigger;
import mindustry.game.Schematic;
import mindustry.game.Schematic.Stile;
import mindustry.game.Team;
import mindustry.game.Teams.BlockPlan;
import mindustry.gen.Groups;
import mindustry.gen.Icon;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.Build;
import mindustry.world.blocks.defense.Wall;
import modifiedtools.MdtKeybinds;
import modifiedtools.features.Feature;
import modifiedtools.features.FeatureMetadata;

public class AssistantBuilderFeature implements Feature {
    private static final String STYLE_SETTING_KEY = "modifiedtools.assistantbuilder.style";
    private static final String ROW_COUNT_SETTING_KEY = "modifiedtools.assistantbuilder.row-count";
    private static final String AUTO_BUILD_SETTING_KEY = "modifiedtools.assistantbuilder.auto-build";
    private static final String AUTO_REPAIR_SETTING_KEY = "modifiedtools.assistantbuilder.auto-repair";
    private static final int MAX_ROW_COUNT = 10;

    private static final float AUTO_BUILD_INTERVAL = 30f;
    private static final float AUTO_REPAIR_INTERVAL = 60f;
    private static final int MAX_AUTO_BUILD_RADIUS_TILES = 250;
    private static final float AUTO_REPAIR_RADIUS_TILES = 80f;
    private static final int AUTO_BUILD_ATTEMPTS_PER_TICK = 8;

    private ArchitecturalStyle style = readSavedStyle();
    private AssistantBuilderSettingDialog settingDialog;
    private AssistantBuilderPickDialog pickDialog;

    private Schematic armed;
    private Schematic selectedSchematic;

    private Table armedIndicator;

    private boolean autoBuild = Core.settings.getBool(AUTO_BUILD_SETTING_KEY, false);
    private boolean autoRepair = Core.settings.getBool(AUTO_REPAIR_SETTING_KEY, false);
    private float autoBuildCooldown;
    private float autoRepairCooldown;

    private int spiralX, spiralY, spiralDX = 1, spiralDY = 0, spiralLegLength = 1, spiralLegProgress, spiralTurns;

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
            if (!isEnabled() || !Vars.state.isGame()) {
                return;
            }

            updateManualArm();
            updateAutoBuild();
            updateAutoRepair();
        });

        Events.run(Trigger.draw, this::drawGhost);
    }

    private void updateManualArm() {
        if (armed == null) {
            return;
        }

        if (Core.scene.hasField() || Core.scene.hasDialog()) {
            return;
        }

        if (Core.input.keyTap(KeyCode.escape) || Core.input.keyTap(KeyCode.back)) {
            armed = null;
            return;
        }

        if (Core.input.justTouched()) {
            var world = Core.input.mouseWorld();
            int tileX = Mathf.round(world.x / Vars.tilesize);
            int tileY = Mathf.round(world.y / Vars.tilesize);

            boolean placed = getRowCount() > 1
                    ? queueRow(armed, tileX, tileY, getRowCount())
                    : queueBuild(armed, tileX, tileY);

            armed = null;

            Vars.ui.showInfoToast(placed ? "@assistantbuilder.toast.queued" : "@assistantbuilder.toast.invalid", 2f);
        }
    }

    private void updateAutoBuild() {
        if (!autoBuild || selectedSchematic == null) {
            return;
        }

        autoBuildCooldown -= Time.delta;
        if (autoBuildCooldown > 0) {
            return;
        }
        autoBuildCooldown = AUTO_BUILD_INTERVAL;

        var core = Vars.player.team().core();
        if (core == null) {
            return;
        }

        int stepW = selectedSchematic.width + Math.max(style.spacing, 1);
        int stepH = selectedSchematic.height + Math.max(style.spacing, 1);

        for (int attempt = 0; attempt < AUTO_BUILD_ATTEMPTS_PER_TICK; attempt++) {
            int cellX = spiralX;
            int cellY = spiralY;
            advanceSpiral();

            if (Math.abs(cellX) * (long) stepW > MAX_AUTO_BUILD_RADIUS_TILES
                    || Math.abs(cellY) * (long) stepH > MAX_AUTO_BUILD_RADIUS_TILES) {
                continue;
            }

            int originX = core.tileX() + cellX * stepW;
            int originY = core.tileY() + cellY * stepH;

            if (queueBuild(selectedSchematic, originX, originY)) {
                return;
            }
        }
    }

    private void updateAutoRepair() {
        if (!autoRepair) {
            return;
        }

        autoRepairCooldown -= Time.delta;
        if (autoRepairCooldown > 0) {
            return;
        }
        autoRepairCooldown = AUTO_REPAIR_INTERVAL;

        var core = Vars.player.team().core();
        if (core == null) {
            return;
        }

        Team team = Vars.player.team();
        var plans = team.data().plans;
        float radiusSq = AUTO_REPAIR_RADIUS_TILES * AUTO_REPAIR_RADIUS_TILES;

        for (var building : Groups.build) {
            if (building.team != team || building.healthf() >= 0.999f) {
                continue;
            }

            float dx = building.tileX() - core.tileX();
            float dy = building.tileY() - core.tileY();
            if (dx * dx + dy * dy > radiusSq) {
                continue;
            }

            int bx = building.tileX();
            int by = building.tileY();

            plans.remove(p -> p.x == bx && p.y == by);
            plans.add(new BlockPlan(bx, by, (short) building.rotation, building.block, building.config()));
        }
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
        armed = null;
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

        table.button(Icon.cancelSmall, Styles.emptyi, () -> armed = null).size(28f);

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
        setSelectedSchematic(schematic);
        Vars.ui.showInfoToast("@assistantbuilder.toast.armed", 2f);
    }

    public void setSelectedSchematic(Schematic schematic) {
        this.selectedSchematic = schematic;
        resetAutoBuildProgress();
    }

    public Schematic getSelectedSchematic() {
        return selectedSchematic;
    }

    public void resetAutoBuildProgress() {
        spiralX = 0;
        spiralY = 0;
        spiralDX = 1;
        spiralDY = 0;
        spiralLegLength = 1;
        spiralLegProgress = 0;
        spiralTurns = 0;
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
        resetAutoBuildProgress();
    }

    public int getRowCount() {
        return Mathf.clamp(Core.settings.getInt(ROW_COUNT_SETTING_KEY, 1), 1, MAX_ROW_COUNT);
    }

    public void setRowCount(int count) {
        Core.settings.put(ROW_COUNT_SETTING_KEY, Mathf.clamp(count, 1, MAX_ROW_COUNT));
    }

    public boolean isAutoBuild() {
        return autoBuild;
    }

    public void setAutoBuild(boolean value) {
        this.autoBuild = value;
        Core.settings.put(AUTO_BUILD_SETTING_KEY, value);
    }

    public boolean isAutoRepair() {
        return autoRepair;
    }

    public void setAutoRepair(boolean value) {
        this.autoRepair = value;
        Core.settings.put(AUTO_REPAIR_SETTING_KEY, value);
    }

    public boolean queueBuild(Schematic schematic, int originX, int originY) {
        if (!isEnabled() || Vars.player == null) {
            return false;
        }

        Team team = Vars.player.team();
        int snappedX = snap(originX);
        int snappedY = snap(originY);

        Seq<BlockPlan> collected = new Seq<>();
        if (!collectPlacement(schematic, snappedX, snappedY, false, team, collected)) {
            return false;
        }

        if (style.mirrored) {
            int mirrorX = snap(snappedX + schematic.width + style.spacing);
            if (!collectPlacement(schematic, mirrorX, snappedY, true, team, collected)) {
                return false;
            }
        }

        var plans = team.data().plans;
        collected.each(plans::add);
        return true;
    }

    public boolean queueRow(Schematic schematic, int originX, int originY, int count) {
        if (!isEnabled() || Vars.player == null) {
            return false;
        }

        Team team = Vars.player.team();
        int stepWidth = schematic.width + style.spacing;
        int step = style.mirrored ? stepWidth * 2 : stepWidth;

        Seq<BlockPlan> collected = new Seq<>();

        for (int i = 0; i < count; i++) {
            int cursorX = snap(originX + i * step);
            int snappedY = snap(originY);

            if (!collectPlacement(schematic, cursorX, snappedY, false, team, collected)) {
                return false;
            }

            if (style.mirrored) {
                int mirrorX = snap(cursorX + schematic.width + style.spacing);
                if (!collectPlacement(schematic, mirrorX, snappedY, true, team, collected)) {
                    return false;
                }
            }
        }

        var plans = team.data().plans;
        collected.each(plans::add);
        return true;
    }

    private boolean collectPlacement(Schematic schematic, int originX, int originY, boolean mirror,
            Team team, Seq<BlockPlan> out) {
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

            out.add(new BlockPlan(worldX, worldY, (short) rotation, block, tile.config));
        }

        return true;
    }

    private boolean isPlacementValid(Schematic schematic, int originX, int originY, boolean mirror, Team team) {
        return collectPlacement(schematic, originX, originY, mirror, team, new Seq<>());
    }

    private int mirrorRotation(int rotation) {
        if (rotation == 1) {
            return 3;
        }
        if (rotation == 3) {
            return 1;
        }
        return rotation;
    }

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

    private void advanceSpiral() {
        spiralX += spiralDX;
        spiralY += spiralDY;
        spiralLegProgress++;

        if (spiralLegProgress == spiralLegLength) {
            spiralLegProgress = 0;

            int newDX = -spiralDY;
            int newDY = spiralDX;
            spiralDX = newDX;
            spiralDY = newDY;

            spiralTurns++;
            if (spiralTurns % 2 == 0) {
                spiralLegLength++;
            }
        }
    }

    private void drawGhost() {
        if (armed == null || !Vars.state.isGame() || Core.scene.hasField() || Core.scene.hasDialog()) {
            return;
        }

        Team team = Vars.player.team();
        var world = Core.input.mouseWorld();
        int snappedX = snap(Mathf.round(world.x / Vars.tilesize));
        int snappedY = snap(Mathf.round(world.y / Vars.tilesize));

        Draw.z(Layer.overlayUI);

        drawGhostCopy(armed, snappedX, snappedY, false, team);

        if (style.mirrored) {
            int mirrorX = snap(snappedX + armed.width + style.spacing);
            drawGhostCopy(armed, mirrorX, snappedY, true, team);
        }

        Draw.reset();
    }

    private void drawGhostCopy(Schematic schematic, int originX, int originY, boolean mirror, Team team) {
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
