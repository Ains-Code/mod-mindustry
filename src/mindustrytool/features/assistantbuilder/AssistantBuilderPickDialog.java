package mindustrytool.features.assistantbuilder;

import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.BaseDialog;

public class AssistantBuilderPickDialog extends BaseDialog {
    private final AssistantBuilderFeature feature;

    public AssistantBuilderPickDialog(AssistantBuilderFeature feature) {
        super("@assistantbuilder.pick.title");
        this.feature = feature;
        addCloseButton();
        shown(this::rebuild);
    }

    private void rebuild() {
        cont.clear();

        cont.add("@assistantbuilder.pick.hint").left().padBottom(10).row();

        cont.add("@assistantbuilder.pick.builtin-label").left().padBottom(6).row();

        cont.pane(t -> {
            t.top();

            BuiltinSchematics.all().each(schematic -> {
                t.button(schematic.name(), Icon.hammer, () -> {
                    feature.arm(schematic);
                    hide();
                }).growX().pad(5).row();
            });
        }).growX().padBottom(14).row();

        cont.add("@assistantbuilder.pick.saved-label").left().padBottom(6).row();

        var schematics = Vars.schematics.all();

        if (schematics.isEmpty()) {
            cont.add("@assistantbuilder.pick.empty").row();
            return;
        }

        cont.pane(t -> {
            t.top();

            schematics.each(schematic -> {
                t.button(schematic.name(), Icon.paste, () -> {
                    feature.arm(schematic);
                    hide();
                }).growX().pad(5).row();
            });
        }).grow();
    }
}
