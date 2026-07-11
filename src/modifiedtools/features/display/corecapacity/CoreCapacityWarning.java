package modifiedtools.features.display.corecapacity;

import arc.Core;
import arc.Events;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Interval;
import mindustry.Vars;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.type.Item;
import mindustry.ui.Styles;
import mindustry.gen.Building;
import modifiedtools.features.Feature;
import modifiedtools.features.FeatureMetadata;

/**
 * Shows a small HUD badge listing items in the player's core that are close
 * to overflowing their storage cap, so it's noticeable before production
 * backs up or gets wasted.
 *
 * NOTE: this assumes the per-item storage limit is exposed as
 * {@code block.itemCapacity} on the core's Block definition, which is the
 * standard capacity field used by storage-type blocks in Mindustry. If your
 * game version exposes core capacity differently, update
 * {@code getCapacity(Building)} accordingly.
 */
public class CoreCapacityWarning extends Table implements Feature {
    private final Interval interval = new Interval();
    private final Seq<Item> nearFull = new Seq<>();

    public static final float WARNING_THRESHOLD = 0.9f;

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("feature.core-capacity-warning")
                .description("feature.core-capacity-warning.description")
                .icon(Icon.warning)
                .order(2)
                .quickAccess(true)
                .enabledByDefault(true)
                .build();
    }

    @Override
    public void init() {
        name = "core-capacity-warning";

        Events.run(WorldLoadEvent.class, () -> Core.app.post(this::rebuild));

        visible(() -> Vars.ui.hudfrag.shown && Vars.state.isGame());

        update(() -> {
            if (!visible) {
                return;
            }

            if (interval.get(60)) {
                rebuild();
            }
        });

        Core.app.post(this::rebuild);
    }

    @Override
    public void onEnable() {
        if (Vars.ui.hudGroup != null && parent == null) {
            Vars.ui.hudGroup.addChild(this);
            Core.app.post(this::rebuild);
        }
    }

    @Override
    public void onDisable() {
        remove();
    }

    private float getCapacity(Building core) {
        return core.block.itemCapacity;
    }

    private void rebuild() {
        if (!Vars.state.isGame()) {
            return;
        }

        var core = Vars.player.team() != null ? Vars.player.team().core() : null;

        nearFull.clear();
        clear();

        if (core == null) {
            return;
        }

        float capacity = getCapacity(core);
        if (capacity <= 0) {
            return;
        }

        for (Item item : Vars.content.items()) {
            int amount = core.items.get(item);
            if (amount / capacity >= WARNING_THRESHOLD) {
                nearFull.add(item);
            }
        }

        if (nearFull.isEmpty()) {
            return;
        }

        top().right();
        background(Tex.pane);

        Label title = add("@feature.core-capacity-warning.title").top().left().align(Align.left)
                .style(Styles.outlineLabel).pad(4).color(mindustry.graphics.Pal.accent).get();
        title.setFontScale(0.9f);
        row();

        Table itemsTable = new Table();
        add(itemsTable).growX().pad(4).row();

        int i = 0;
        for (Item item : nearFull) {
            int amount = core.items.get(item);
            itemsTable.image(item.uiIcon).size(16 * 1.5f).padRight(4);
            itemsTable.add(String.valueOf(amount)).style(Styles.outlineLabel).padRight(8);

            if (++i % 3 == 0) {
                itemsTable.row();
            }
        }

        pack();
    }
}
