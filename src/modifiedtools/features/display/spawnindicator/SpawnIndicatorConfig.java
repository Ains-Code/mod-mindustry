package modifiedtools.features.display.spawnindicator;

import arc.Core;

public class SpawnIndicatorConfig {
    public static float opacity = 1f;
    public static boolean showOnScreenMarker = true;
    public static boolean showOffScreenArrow = true;
    public static boolean showCountdown = true;

    public static void load() {
        opacity = Core.settings.getFloat("modifiedtools.spawn-indicator.opacity", 1f);
        showOnScreenMarker = Core.settings.getBool("modifiedtools.spawn-indicator.show-on-screen-marker", true);
        showOffScreenArrow = Core.settings.getBool("modifiedtools.spawn-indicator.show-off-screen-arrow", true);
        showCountdown = Core.settings.getBool("modifiedtools.spawn-indicator.show-countdown", true);
    }

    public static void save() {
        Core.settings.put("modifiedtools.spawn-indicator.opacity", opacity);
        Core.settings.put("modifiedtools.spawn-indicator.show-on-screen-marker", showOnScreenMarker);
        Core.settings.put("modifiedtools.spawn-indicator.show-off-screen-arrow", showOffScreenArrow);
        Core.settings.put("modifiedtools.spawn-indicator.show-countdown", showCountdown);
    }
}
