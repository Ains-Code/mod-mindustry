package modifiedtools.features.display.spawnindicator;

import arc.Core;
import arc.graphics.Color;
import arc.scene.event.Touchable;
import arc.scene.ui.Label;
import arc.scene.ui.Slider;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

public class SpawnIndicatorSettingsDialog extends BaseDialog {
    public SpawnIndicatorSettingsDialog() {
        super("@spawn-indicator.settings.title");
        name = "spawnIndicatorSettingDialog";
        addCloseButton();

        Table container = cont;
        container.defaults().left().pad(5);

        Slider opacitySlider = new Slider(0f, 1f, 0.05f, false);
        opacitySlider.setValue(SpawnIndicatorConfig.opacity);

        Label opacityValue = new Label(
                String.format("%.0f%%", SpawnIndicatorConfig.opacity * 100),
                Styles.outlineLabel);
        opacityValue.setColor(Color.lightGray);

        Table opacityContent = new Table();
        opacityContent.touchable = Touchable.disabled;
        opacityContent.margin(3f, 33f, 3f, 33f);
        opacityContent.add("@opacity", Styles.outlineLabel).left().growX();
        opacityContent.add(opacityValue).padLeft(10f).right();

        opacitySlider.changed(() -> {
            SpawnIndicatorConfig.opacity = opacitySlider.getValue();
            opacityValue.setText(String.format("%.0f%%", SpawnIndicatorConfig.opacity * 100));
            SpawnIndicatorConfig.save();
        });

        float width = Math.min(Core.graphics.getWidth() / 1.2f, 460f);
        container.stack(opacitySlider, opacityContent).width(width).left().padTop(4f).row();

        addCheck(container, "@spawn-indicator.show-on-screen-marker", SpawnIndicatorConfig.showOnScreenMarker, v -> {
            SpawnIndicatorConfig.showOnScreenMarker = v;
            SpawnIndicatorConfig.save();
        });

        if (Vars.mobile) {
            container.row();
        }

        addCheck(container, "@spawn-indicator.show-off-screen-arrow", SpawnIndicatorConfig.showOffScreenArrow, v -> {
            SpawnIndicatorConfig.showOffScreenArrow = v;
            SpawnIndicatorConfig.save();
        });

        container.row();

        addCheck(container, "@spawn-indicator.show-countdown", SpawnIndicatorConfig.showCountdown, v -> {
            SpawnIndicatorConfig.showCountdown = v;
            SpawnIndicatorConfig.save();
        });

        if (Vars.mobile) {
            container.row();
        }
    }

    private void addCheck(Table table, String title, boolean checked, arc.func.Boolc changed) {
        table.check(title, checked, changed).left();
    }
}
