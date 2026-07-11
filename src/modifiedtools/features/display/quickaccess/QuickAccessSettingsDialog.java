package modifiedtools.features.display.quickaccess;

import java.util.function.Consumer;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.Label;
import arc.scene.ui.Slider;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import mindustry.Vars;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import modifiedtools.features.Feature;
import modifiedtools.features.FeatureManager;
import modifiedtools.features.FeatureMetadata;

public class QuickAccessSettingsDialog extends BaseDialog {

    /** On phones, stack "label" above "slider + value" so nothing runs off-screen. */
    private static final boolean STACKED = Vars.mobile;

    public QuickAccessSettingsDialog(QuickAccessFeature quickAccessHud) {
        super("@settings");

        name = "quickAccessSettingDialog";
        addCloseButton();
        closeOnBack();

        Table table = new Table();

        // Cap the width to the visible screen (scaled) rather than a flat 800 so it
        // never has to be squeezed or clipped on a narrow Android portrait screen.
        float maxWidth = Math.min(800f, Core.graphics.getWidth() / Scl.scl() * 0.95f);

        cont.pane(table)
                .center()
                .maxWidth(maxWidth)
                .grow();

        table.defaults().growX();

        table.add("@settings").style(Styles.outlineLabel).left().pad(5).row();

        addSlider(table, "@opacity", 0.05f, 1f, 0.05f, QuickAccessConfig.opacity(), true,
                v -> QuickAccessConfig.opacity(v), quickAccessHud);

        addSlider(table, "@scale", 0.5f, 1.5f, 0.1f, QuickAccessConfig.scale(), true,
                v -> QuickAccessConfig.scale(v), quickAccessHud);

        addSlider(table, "@width", 0.5f, 2.0f, 0.1f, QuickAccessConfig.width(), true,
                v -> QuickAccessConfig.width(v), quickAccessHud);

        addSlider(table, "@columns", 1, 9, 1, QuickAccessConfig.cols(), false,
                v -> QuickAccessConfig.cols((int) (float) v), quickAccessHud);

        table.check("@collapsed", QuickAccessConfig.collapsed(), collapsed -> {
            QuickAccessConfig.collapsed(collapsed);
            quickAccessHud.rebuild();
        }).fillX().top().left().pad(Vars.mobile ? 8f : 5f).get().left();
        table.row();

        table.image().color(Color.gray).height(2).growX().pad(5).row();
        table.add("@features").style(Styles.outlineLabel).left().pad(5).row();

        Seq<Feature> features = FeatureManager.getInstance().getFeatures();
        for (Feature feature : features) {
            if (feature == quickAccessHud) {
                continue;
            }

            FeatureMetadata meta = feature.getMetadata();

            if (!meta.quickAccess()) {
                continue;
            }

            table.check(meta.name(), QuickAccessConfig.isFeatureVisible(meta.name()), visible -> {
                QuickAccessConfig.setFeatureVisible(meta.name(), visible);
                quickAccessHud.rebuild();
            }).fillX().top().left().pad(Vars.mobile ? 8f : 5f).get().left();

            table.row();
        }

        table.button("@reset", () -> {
            QuickAccessConfig.x(0);
            QuickAccessConfig.y(0);
            quickAccessHud.rebuild();
        }).fillX().top().left().pad(5).height(Vars.mobile ? 56f : 44f).get().left();
    }

    /**
     * Adds a labelled slider row that adapts to the available width: a compact
     * single line on desktop, and a wrapped label stacked above a full-width
     * slider on mobile so long translated strings never get clipped.
     */
    private void addSlider(Table table, String labelKey, float min, float max, float step,
            float initial, boolean percent, Consumer<Float> onChange, QuickAccessFeature quickAccessHud) {

        Table row = new Table();
        row.left();

        Slider slider = new Slider(min, max, step, false);
        slider.setValue(initial);

        Label valueLabel = new Label(formatValue(initial, percent));

        slider.changed(() -> {
            float v = slider.getValue();
            onChange.accept(v);
            valueLabel.setText(formatValue(v, percent));
            quickAccessHud.rebuild();
        });

        if (STACKED) {
            row.labelWrap(labelKey).left().growX().padBottom(4f).row();

            Table sliderRow = new Table();
            sliderRow.add(slider).growX();
            sliderRow.add(valueLabel).padLeft(10).minWidth(54f).labelAlign(Align.right);
            row.add(sliderRow).growX();
        } else {
            row.labelWrap(labelKey).left().padRight(10).width(110f);
            row.add(slider).growX();
            row.add(valueLabel).padLeft(10).minWidth(54f).labelAlign(Align.right);
        }

        table.add(row).growX().pad(5).row();
    }

    private static String formatValue(float value, boolean percent) {
        return percent ? String.format("%.0f%%", value * 100) : String.valueOf((int) value);
    }
}
