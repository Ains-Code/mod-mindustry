package modifiedtools.features.assistantbuilder;

import arc.Core;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

public class AssistantBuilderSettingDialog extends BaseDialog {
    private final AssistantBuilderFeature feature;

    public AssistantBuilderSettingDialog(AssistantBuilderFeature feature) {
        super("@assistantbuilder.settings.title");
        this.feature = feature;
        addCloseButton();
        shown(this::rebuild);
    }

    private void rebuild() {
        cont.clear();

        if (!feature.isEnabled()) {
            cont.add("@assistantbuilder.settings.disabled").row();
            return;
        }

        cont.button("@assistantbuilder.settings.pick-button", Icon.paste, feature::openPicker)
                .growX().pad(5).row();

        cont.table(row -> {
            row.left();
            row.add("@assistantbuilder.settings.row-count-label").left().padRight(10);
            row.label(() -> String.valueOf(feature.getRowCount())).padRight(10).width(20);
            row.button(Icon.left, Styles.emptyi, () -> feature.setRowCount(feature.getRowCount() - 1))
                    .size(32f);
            row.button(Icon.right, Styles.emptyi, () -> feature.setRowCount(feature.getRowCount() + 1))
                    .size(32f);
        }).growX().pad(5).row();

        cont.image().color(arc.graphics.Color.gray).growX().height(2f).pad(8).row();

        cont.check("@assistantbuilder.settings.auto-build", feature.isAutoBuild(), feature::setAutoBuild)
                .left().pad(5).row();
        cont.check("@assistantbuilder.settings.auto-repair", feature.isAutoRepair(), feature::setAutoRepair)
                .left().pad(5).row();

        cont.add("@assistantbuilder.settings.auto-note").left().wrap().width(360f).pad(5).row();

        cont.image().color(arc.graphics.Color.gray).growX().height(2f).pad(8).row();

        cont.pane(t -> {
            t.top();
            t.add("@assistantbuilder.settings.style-label").left().padBottom(10).row();

            for (ArchitecturalStyle style : ArchitecturalStyle.values()) {
                boolean selected = feature.getStyle() == style;

                t.table(selected ? Tex.buttonDown : Tex.button, c -> {
                    c.left();
                    c.check("", selected, checked -> {
                        if (checked) {
                            feature.setStyle(style);
                            rebuild();
                        }
                    }).padRight(10);
                    c.label(() -> Core.bundle.get(style.nameKey)).left();
                }).growX().pad(5).row();
            }
        }).grow();
    }
}
