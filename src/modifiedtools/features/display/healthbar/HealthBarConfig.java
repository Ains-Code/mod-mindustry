package modifiedtools.features.display.healthbar;

import arc.Core;

public class HealthBarConfig {
    public static float zoomThreshold;
    public static float opacity;
    public static float scale;
    public static float width;

    public static void load() {
        zoomThreshold = Core.settings.getFloat("modifiedtools.health-bar.zoom-threshold", 0.5f);
        opacity = Core.settings.getFloat("modifiedtools.health-bar.opacity", 1f);
        scale = Core.settings.getFloat("modifiedtools.health-bar.scale", 1f);
        width = Core.settings.getFloat("modifiedtools.health-bar.width", 1f);
    }

    public static void save() {
        Core.settings.put("modifiedtools.health-bar.zoom-threshold", zoomThreshold);
        Core.settings.put("modifiedtools.health-bar.opacity", opacity);
        Core.settings.put("modifiedtools.health-bar.scale", scale);
        Core.settings.put("modifiedtools.health-bar.width", width);
    }

    public static void reset() {
        zoomThreshold = 0.5f;
        opacity = 1f;
        scale = 1f;
        width = 1f;
        save();
    }
}
