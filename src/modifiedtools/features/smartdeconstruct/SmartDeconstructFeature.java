package modifiedtools.features.smartdeconstruct;

import arc.Core;
import arc.math.geom.Point2;
import arc.math.geom.Vec2;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.core.GameState.State;
import mindustry.entities.units.BuildPlan;
import mindustry.game.EventType.StateChangeEvent;
import mindustry.game.EventType.TapEvent;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.distribution.BufferedItemBridge;
import mindustry.world.blocks.distribution.Conveyor;
import mindustry.world.blocks.distribution.Duct;
import mindustry.world.blocks.distribution.DuctRouter;
import mindustry.world.blocks.distribution.ItemBridge;
import mindustry.world.blocks.distribution.Junction;
import mindustry.world.blocks.distribution.OverflowDuct;
import mindustry.world.blocks.distribution.OverflowGate;
import mindustry.world.blocks.distribution.Router;
import mindustry.world.blocks.distribution.Sorter;
import mindustry.world.blocks.distribution.StackConveyor;
import mindustry.world.blocks.distribution.DuctBridge.DuctBridgeBuild;
import mindustry.world.blocks.liquid.Conduit;
import mindustry.world.blocks.liquid.LiquidBlock;
import mindustry.world.blocks.liquid.LiquidBridge;
import mindustry.world.blocks.liquid.LiquidJunction;
import mindustry.world.blocks.liquid.LiquidRouter;
import mindustry.world.blocks.defense.Wall;
import mindustry.world.blocks.production.Drill;
import mindustry.world.blocks.production.BeamDrill;
import modifiedtools.features.Feature;
import modifiedtools.features.FeatureMetadata;
import modifiedtools.services.TapListener;

/**
 * Companion to SmartUpgradeFeature: hold-tap a distribution/wall/drill/conduit
 * block to bring up a single "Deconstruct chain" button that walks the
 * connected network (same traversal rules as Smart Upgrade) and queues every
 * linked block of the same group for removal in one action.
 */
public class SmartDeconstructFeature implements Feature {
    private Table currentMenu;
    private Tile selectedTile;

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("feature.smart-deconstruct")
                .description("feature.smart-deconstruct.description")
                .icon(Icon.trash)
                .quickAccess(true)
                .build();
    }

    @Override
    public void init() {
        TapListener.getInstance().registerHoldListener(300, 10, null, (tile, data) -> {
            if (!isEnabled() || tile == null) {
                return;
            }
            if (currentMenu == null) {
                if (getGroup(tile.block()) != BlockGroup.NONE) {
                    showMenu(tile);
                }
            }
        });

        arc.Events.on(TapEvent.class, e -> {
            if (!isEnabled()) {
                return;
            }

            if (currentMenu != null) {
                if (e.tile == selectedTile) {
                    closeMenu();
                    return;
                }

                closeMenu();
            }
        });

        arc.Events.on(StateChangeEvent.class, e -> {
            if (e.to == State.menu) {
                closeMenu();
            }
        });
    }

    @Override
    public void onDisable() {
        closeMenu();
    }

    private void closeMenu() {
        if (currentMenu != null) {
            currentMenu.remove();
            currentMenu = null;
            selectedTile = null;
        }
    }

    private enum BlockGroup {
        CONVEYOR, WALL, DRILL, CONDUIT, NONE
    }

    private BlockGroup getGroup(Block block) {
        if (block == null)
            return BlockGroup.NONE;
        if (block instanceof Conveyor || block instanceof StackConveyor || block instanceof Duct)
            return BlockGroup.CONVEYOR;
        if (block instanceof Wall)
            return BlockGroup.WALL;
        if (block instanceof Drill || block instanceof BeamDrill)
            return BlockGroup.DRILL;
        if (block instanceof Conduit)
            return BlockGroup.CONDUIT;

        return BlockGroup.NONE;
    }

    private void showMenu(Tile tile) {
        selectedTile = tile;
        currentMenu = new Table(Styles.black6);
        currentMenu.visible(() -> Vars.ui.hudfrag != null && Vars.ui.hudfrag.shown);
        currentMenu.touchable = Touchable.enabled;

        currentMenu.update(() -> {
            if (selectedTile == null || selectedTile.block() == null
                    || getGroup(selectedTile.block()) == BlockGroup.NONE) {
                closeMenu();
                return;
            }

            Vec2 pos = Core.camera.project(selectedTile.worldx(), selectedTile.worldy());

            currentMenu.setPosition(pos.x, pos.y + selectedTile.block().size * Vars.tilesize * 3,
                    Align.bottom | Align.center);
        });

        currentMenu.button(b -> b.image(Icon.cancel).scaling(arc.util.Scaling.fit), Styles.clearNonei, () -> {
            Vars.control.input.isBuilding = false;
            Core.app.post(() -> deconstructChain(tile));
            closeMenu();
        }).size(48f).pad(4);

        Vars.ui.hudGroup.addChild(currentMenu);
        Timer.schedule(() -> {
            if (currentMenu != null) {
                currentMenu.toFront();
            }
        }, 5f);
        currentMenu.pack();
    }

    private void deconstructChain(Tile startTile) {
        if (Vars.player.unit() == null) {
            return;
        }

        ObjectSet<Tile> visited = new ObjectSet<>();
        Seq<Tile> queue = new Seq<>();

        queue.add(startTile);
        visited.add(startTile);

        int maxUpdates = 500;
        int updates = 0;
        BlockGroup group = getGroup(startTile.block());

        while (!queue.isEmpty() && updates < maxUpdates) {
            Tile current = queue.pop();

            if (getGroup(current.block()) == group) {
                var plan = new BuildPlan(current.x, current.y);
                plan.breaking = true;
                Vars.player.unit().plans.add(plan);
                updates++;
            }

            Building build = current.build;

            if (build == null) {
                continue;
            }

            Block block = current.block();

            if (group == BlockGroup.CONVEYOR) {
                if (block instanceof Conveyor || block instanceof StackConveyor || block instanceof Duct) {
                    checkAndAdd(queue, visited, build.front(), group);
                    checkAndAdd(queue, visited, build.back(), group);
                } else if (block instanceof BufferedItemBridge || block instanceof ItemBridge) {
                    Object c = build.config();

                    if (c instanceof Point2 conf) {
                        Tile link = Vars.world.tile(current.x + conf.x, current.y + conf.y);
                        if (link != null && link.build != null) {
                            for (int i = 0; i < 4; i++) {
                                var nearby = link.nearby(i);
                                if (nearby != null && nearby.build != null) {
                                    checkAndAdd(queue, visited, nearby.build, group);
                                }
                            }
                        }
                    }

                } else if (build instanceof DuctBridgeBuild ductBridge) {
                    var linked = ductBridge.findLink();

                    if (linked != null) {
                        checkAndAdd(queue, visited, linked, group);
                    }
                } else if (block instanceof Sorter || block instanceof Router || block instanceof OverflowGate
                        || block instanceof DuctRouter || block instanceof OverflowDuct || block instanceof Junction) {
                    for (int i = 0; i < 4; i++) {
                        var nearby = current.nearby(i);
                        if (nearby != null && nearby.build != null) {
                            checkAndAdd(queue, visited, current.nearby(i).build, group);
                        }
                    }
                }
            } else if (group == BlockGroup.WALL || group == BlockGroup.DRILL) {
                if (build.proximity != null) {
                    for (Building next : build.proximity) {
                        checkAndAdd(queue, visited, next, group);
                    }
                }
            } else if (group == BlockGroup.CONDUIT) {
                if (block instanceof Conduit) {
                    checkAndAdd(queue, visited, build.front(), group);
                    checkAndAdd(queue, visited, build.back(), group);
                } else if (block instanceof LiquidBridge) {
                    Object c = build.config();

                    if (c instanceof Point2 conf) {
                        Tile link = Vars.world.tile(current.x + conf.x, current.y + conf.y);
                        if (link != null && link.build != null) {
                            for (int i = 0; i < 4; i++) {
                                var nearby = link.nearby(i);
                                if (nearby != null && nearby.build != null) {
                                    checkAndAdd(queue, visited, link.nearby(i).build, group);
                                }
                            }
                        }
                    }
                } else if (block instanceof LiquidRouter || block instanceof LiquidJunction) {
                    for (int i = 0; i < 4; i++) {
                        var nearby = current.nearby(i);
                        if (nearby != null && nearby.build != null) {
                            checkAndAdd(queue, visited, current.nearby(i).build, group);
                        }
                    }
                }
            }
        }
    }

    private boolean checkAndAdd(Seq<Tile> queue, ObjectSet<Tile> visited, Building target, BlockGroup group) {
        if (target == null) {
            return false;
        }

        Tile tile = target.tile;

        if (tile == null) {
            return false;
        }

        if (visited.contains(tile)) {
            return false;
        }

        Block block = tile.block();

        if (group == BlockGroup.CONVEYOR) {
            if (getGroup(block) == BlockGroup.CONVEYOR || block instanceof Junction || block instanceof ItemBridge ||
                    block instanceof Router || block instanceof Sorter || block instanceof OverflowGate ||
                    block instanceof DuctRouter || block instanceof OverflowDuct) {

                visited.add(tile);
                queue.add(tile);
                return true;
            }
        } else if (group == BlockGroup.WALL || group == BlockGroup.DRILL) {
            if (getGroup(block) == group) {
                visited.add(tile);
                queue.add(tile);
                return true;
            }
        } else if (group == BlockGroup.CONDUIT) {
            if (getGroup(block) == group || block instanceof Conduit || block instanceof LiquidBlock) {
                visited.add(tile);
                queue.add(tile);
                return true;
            }
        }

        return false;
    }
}
