package modifiedtools.features.conveyormaker;

import arc.Core;
import arc.Events;
import arc.math.geom.Vec2;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Scaling;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.core.GameState.State;
import mindustry.entities.units.BuildPlan;
import mindustry.game.EventType.StateChangeEvent;
import mindustry.game.EventType.TapEvent;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.distribution.Conveyor;
import modifiedtools.features.Feature;
import modifiedtools.features.FeatureMetadata;
import modifiedtools.services.TapListener;

/**
 * Lets the player hold down on an empty tile, pick a conveyor type from a
 * popup, then tap a second tile to automatically queue a straight (or
 * L-shaped) line of conveyors connecting the two points.
 */
public class ConveyorMakerFeature implements Feature {
    private static final int MAX_LENGTH = 300;

    private Table currentMenu;
    private Tile startTile;

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("feature.conveyor-maker")
                .description("feature.conveyor-maker.description")
                .icon(Icon.right)
                .quickAccess(true)
                .build();
    }

    @Override
    public void init() {
        TapListener.getInstance().registerHoldListener(100, 60, null, (tile, data) -> {
            if (!isEnabled() || tile == null || tile.build != null) {
                return;
            }

            if (currentMenu == null && startTile == null) {
                showTypeMenu(tile);
            }
        });

        Events.on(TapEvent.class, e -> {
            if (!isEnabled()) {
                return;
            }

            if (currentMenu != null && e.tile != startTile) {
                closeMenu();
            }
        });

        Events.on(StateChangeEvent.class, e -> {
            if (e.to == State.menu) {
                reset();
            }
        });
    }

    @Override
    public void onDisable() {
        reset();
    }

    private void reset() {
        closeMenu();
        startTile = null;
    }

    private void closeMenu() {
        if (currentMenu != null) {
            currentMenu.remove();
            currentMenu = null;
        }
    }

    private void showTypeMenu(Tile tile) {
        closeMenu();

        startTile = tile;
        currentMenu = new Table();
        currentMenu.visible(() -> Vars.ui.hudfrag != null && Vars.ui.hudfrag.shown);
        currentMenu.touchable = Touchable.enabled;

        currentMenu.update(() -> {
            if (startTile == null) {
                closeMenu();
                return;
            }

            Vec2 pos = Core.camera.project(startTile.worldx(), startTile.worldy());
            currentMenu.setPosition(pos.x, pos.y, Align.center);
        });

        int i = 0;
        var conveyors = Vars.content.blocks().select(this::isValidConveyor);

        for (Block block : conveyors) {
            currentMenu.button(b -> b.image(block.uiIcon).scaling(Scaling.fit), Styles.clearNonei, () -> {
                closeMenu();
                selectEndpoint(tile, block);
            }).size(48f).pad(4);

            if (++i % 4 == 0) {
                currentMenu.row();
            }
        }

        currentMenu.button(Icon.cancel, this::reset).size(48f).pad(4);

        if (i == 0) {
            currentMenu.add("@none").pad(8);
        }

        Vars.ui.hudGroup.addChild(currentMenu);
        Timer.schedule(() -> {
            if (currentMenu != null) {
                currentMenu.toFront();
            }
        }, 0.1f);
        currentMenu.pack();
    }

    private void selectEndpoint(Tile start, Block conveyor) {
        Vars.ui.showInfoFade("@feature.conveyor-maker.select-end");

        TapListener.getInstance().select((worldX, worldY) -> {
            Tile end = Vars.world.tileWorld(worldX, worldY);
            reset();

            if (end == null || end == start) {
                return;
            }

            Core.app.post(() -> placeConveyorLine(start, end, conveyor));
        });
    }

    private boolean isValidConveyor(Block block) {
        return unlocked(block) && block instanceof Conveyor;
    }

    private boolean unlocked(Block block) {
        return block.unlockedNowHost() && block.placeablePlayer && block.environmentBuildable() &&
                block.supportsEnv(Vars.state.rules.env);
    }

    private void placeConveyorLine(Tile start, Tile end, Block conveyor) {
        Seq<Tile> path = buildPath(start, end);

        if (path == null || path.size < 2) {
            return;
        }

        if (path.size > MAX_LENGTH) {
            Vars.ui.showInfoFade("@feature.conveyor-maker.too-far");
            return;
        }

        int lastRotation = 0;

        for (int i = 0; i < path.size; i++) {
            Tile tile = path.get(i);
            int rotation = i < path.size - 1 ? rotationTo(tile, path.get(i + 1)) : lastRotation;
            lastRotation = rotation;

            BuildPlan plan = new BuildPlan(tile.x, tile.y, rotation, conveyor);

            if (plan.placeable(Vars.player.team())) {
                Vars.player.unit().addBuild(plan);
            }
        }
    }

    /**
     * Builds a tile path from start to end: a straight line if they share a
     * row or column, otherwise an L-shaped path (horizontal first, then
     * vertical).
     */
    private Seq<Tile> buildPath(Tile start, Tile end) {
        Seq<Tile> path = new Seq<>();
        path.add(start);

        int x = start.x;
        int y = start.y;

        int stepX = Integer.signum(end.x - start.x);
        int stepY = Integer.signum(end.y - start.y);

        while (x != end.x) {
            x += stepX;
            Tile next = Vars.world.tile(x, y);

            if (next == null) {
                return null;
            }

            path.add(next);
        }

        while (y != end.y) {
            y += stepY;
            Tile next = Vars.world.tile(x, y);

            if (next == null) {
                return null;
            }

            path.add(next);
        }

        return path;
    }

    private int rotationTo(Tile from, Tile to) {
        int dx = to.x - from.x;
        int dy = to.y - from.y;

        if (dx > 0) {
            return 0;
        }
        if (dy > 0) {
            return 1;
        }
        if (dx < 0) {
            return 2;
        }
        return 3;
    }
}
