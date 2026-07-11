package modifiedtools.features.campaign;

import java.util.Optional;

import arc.Core;
import arc.Events;
import arc.scene.event.Touchable;
import arc.scene.ui.Dialog;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Interval;
import mindustry.Vars;
import mindustry.core.UI;
import mindustry.game.EventType.ResizeEvent;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.Planet;
import mindustry.type.Sector;
import mindustry.ui.Styles;
import modifiedtools.features.Feature;
import modifiedtools.features.FeatureMetadata;

/**
 * Campaign-only HUD panel: tracks how many sectors have been captured on the
 * current planet, warns before the next wave lands, shows a quick snapshot of
 * stored resources, and points out nearby unclaimed sectors worth expanding
 * into next.
 */
public class CampaignFeature extends Table implements Feature {
    private static final int MAX_RESOURCE_ROWS = 6;

    private final Interval interval = new Interval();
    private final ObjectSet<Sector> expansionTargets = new ObjectSet<>();

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("@feature.campaign")
                .description("@feature.campaign.description")
                .icon(Icon.planet)
                .order(2)
                .quickAccess(true)
                .enabledByDefault(true)
                .build();
    }

    @Override
    public void init() {
        name = "campaign-overlay";
        touchable = Touchable.disabled;

        visible(() -> Vars.state.isGame() && Vars.state.isCampaign() && Vars.ui.hudfrag != null
                && Vars.ui.hudfrag.shown);

        Events.run(WorldLoadEvent.class, () -> Core.app.post(this::rebuild));
        Events.on(ResizeEvent.class, e -> rebuild());

        update(() -> {
            if (!visible) {
                return;
            }

            if (interval.get(0, 60f)) {
                rebuild();
            }
        });

        Core.app.post(this::rebuild);
    }

    @Override
    public void onEnable() {
        remove();

        name = "campaign-overlay";

        Vars.ui.hudGroup.addChild(this);

        setPosition(CampaignConfig.x(), CampaignConfig.y(), Align.bottomLeft);

        Core.app.post(this::rebuild);
    }

    @Override
    public void onDisable() {
        remove();
    }

    @Override
    public Optional<Dialog> setting() {
        return Optional.of(new CampaignSettingDialog());
    }

    private void rebuild() {
        clear();

        if (!Vars.state.isGame() || !Vars.state.isCampaign()) {
            return;
        }

        Planet planet = Vars.state.getPlanet();
        Sector sector = Vars.state.getSector();

        if (planet == null || sector == null) {
            return;
        }

        top().left();
        background(Styles.black6);
        setColor(1f, 1f, 1f, CampaignConfig.opacity());
        margin(8f);

        // Header: planet name.
        table(header -> {
            header.left();
            header.image(Icon.planet).size(20).padRight(6);
            header.add(planet.localizedName).style(Styles.outlineLabel).color(Pal.accent);
        }).growX().left().row();

        image().color(arc.graphics.Color.gray).growX().height(2f).pad(4).row();

        // Current sector + capture status.
        add(sector.name()).style(Styles.outlineLabel).left().padBottom(2).row();
        add(sectorStatusText(sector)).style(Styles.outlineLabel).color(sectorStatusColor(sector)).left().padBottom(6)
                .row();

        // Progression: how many sectors on this planet have been captured.
        int captured = 0;
        int total = planet.sectors.size;

        for (Sector s : planet.sectors) {
            if (s.hasBase()) {
                captured++;
            }
        }

        add(Core.bundle.format("feature.campaign.captured", captured, total)).style(Styles.outlineLabel).left()
                .padBottom(6).row();

        // Wave countdown warning.
        if (CampaignConfig.showWaveCountdown() && Vars.state.rules.waves) {
            float secondsLeft = Vars.state.wavetime / 60f;
            boolean warning = secondsLeft <= CampaignConfig.waveWarningSeconds();

            String text = Core.bundle.format("feature.campaign.next-wave", Vars.state.wave, UI.formatTime(Vars.state.wavetime));

            add(text).style(Styles.outlineLabel)
                    .color(warning ? Pal.remove : arc.graphics.Color.white)
                    .left().padBottom(6).row();
        }

        // Nearby unclaimed sectors worth expanding into.
        if (CampaignConfig.showExpansionTargets()) {
            expansionTargets.clear();

            for (Sector s : planet.sectors) {
                if (!s.hasBase()) {
                    continue;
                }

                for (Sector near : s.near()) {
                    if (near != null && !near.hasBase()) {
                        expansionTargets.add(near);
                    }
                }
            }

            add(Core.bundle.format("feature.campaign.expansion-targets", expansionTargets.size))
                    .style(Styles.outlineLabel).left().padBottom(6).row();
        }

        // Core resource snapshot.
        if (CampaignConfig.showResources()) {
            var core = Vars.player.team() != null ? Vars.player.team().core() : null;

            if (core != null) {
                Table resourceTable = new Table();
                add(resourceTable).growX().left();

                Seq<ItemStack> stacks = new Seq<>();

                for (Item item : Vars.content.items()) {
                    int amount = core.items.get(item);
                    if (amount > 0) {
                        stacks.add(new ItemStack(item, amount));
                    }
                }

                stacks.sort((a, b) -> Integer.compare(b.amount, a.amount));

                int shown = 0;
                for (ItemStack stack : stacks) {
                    if (shown >= MAX_RESOURCE_ROWS) {
                        break;
                    }

                    resourceTable.image(stack.item.uiIcon).size(16).padRight(4);
                    resourceTable.add(String.valueOf(stack.amount)).style(Styles.outlineLabel).padRight(10);

                    shown++;
                    if (shown % 3 == 0) {
                        resourceTable.row();
                    }
                }
            }
        }

        pack();
    }

    private String sectorStatusText(Sector sector) {
        if (sector.isCaptured()) {
            return Core.bundle.get("feature.campaign.status.captured");
        }
        if (sector.isAttacked()) {
            return Core.bundle.get("feature.campaign.status.under-attack");
        }
        return Core.bundle.get("feature.campaign.status.in-progress");
    }

    private arc.graphics.Color sectorStatusColor(Sector sector) {
        if (sector.isCaptured()) {
            return Pal.accent;
        }
        if (sector.isAttacked()) {
            return Pal.remove;
        }
        return arc.graphics.Color.lightGray;
    }
}
