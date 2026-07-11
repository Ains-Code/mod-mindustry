package modifiedtools;

import arc.Core;
import arc.Events;
import arc.input.KeyBind;
import arc.input.KeyCode;
import mindustry.game.EventType.Trigger;
import modifiedtools.features.Feature;
import modifiedtools.features.FeatureManager;

public class MdtKeybinds {

    public static KeyBind mapBrowserKb = KeyBind.add("mapBrowser", KeyCode.unset, "ModifiedTools"),
            schematicBrowserKb = KeyBind.add("schematicBrowser", KeyCode.unset, "ModifiedTools"),
            autoPlay = KeyBind.add("autoPlay", KeyCode.unset, "ModifiedTools"),
            assistantBuilderKb = KeyBind.add("assistantBuilder", KeyCode.unset, "ModifiedTools"),
            chatKb = KeyBind.add("chatOverlay", KeyCode.unset, "ModifiedTools");

    public static void addFeatureKeyBind(Feature feature, KeyBind keyBind) {
        Events.run(Trigger.update, () -> {
            boolean noInputFocused = !Core.scene.hasField();

            if (noInputFocused && Core.input.keyRelease(keyBind)) {
                Core.app.post(() -> FeatureManager.getInstance().toggle(feature));
            }
        });
    }
}
