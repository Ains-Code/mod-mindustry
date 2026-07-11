package modifiedtools.features.display.wavepreview;

import arc.Core;

public class WavePreviewConfig {
    public static boolean enabled() {
        return Core.settings.getBool("modifiedtools.wave-preview.enabled", true);
    }

    public static void enabled(boolean val) {
        Core.settings.put("modifiedtools.wave-preview.enabled", val);
    }

    public static float x() {
        return Core.settings.getFloat("modifiedtools.wave-preview.x", 100f);
    }

    public static void x(float val) {
        Core.settings.put("modifiedtools.wave-preview.x", val);
    }

    public static float y() {
        return Core.settings.getFloat("modifiedtools.wave-preview.y", Core.graphics.getHeight() - 200f);
    }

    public static void y(float val) {
        Core.settings.put("modifiedtools.wave-preview.y", val);
    }
    
    public static float scale() {
        return Core.settings.getFloat("modifiedtools.wave-preview.scale", 1f);
    }

    public static void scale(float val) {
        Core.settings.put("modifiedtools.wave-preview.scale", val);
    }

    public static float opacity() {
        return Core.settings.getFloat("modifiedtools.wave-preview.opacity", 1f);
    }

    public static void opacity(float val) {
        Core.settings.put("modifiedtools.wave-preview.opacity", val);
    }
}
