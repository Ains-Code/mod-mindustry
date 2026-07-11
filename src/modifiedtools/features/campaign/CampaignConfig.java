package modifiedtools.features.campaign;

import arc.Core;

public class CampaignConfig {
    private static final String PREFIX = "modifiedtools.campaign.";

    public static float x() {
        return Core.settings.getFloat(PREFIX + "x", 10f);
    }

    public static void x(float value) {
        Core.settings.put(PREFIX + "x", value);
    }

    public static float y() {
        return Core.settings.getFloat(PREFIX + "y", Core.graphics.getHeight() - 10f);
    }

    public static void y(float value) {
        Core.settings.put(PREFIX + "y", value);
    }

    public static float opacity() {
        return Core.settings.getFloat(PREFIX + "opacity", 1f);
    }

    public static void opacity(float value) {
        Core.settings.put(PREFIX + "opacity", value);
    }

    public static boolean showWaveCountdown() {
        return Core.settings.getBool(PREFIX + "show-wave-countdown", true);
    }

    public static void showWaveCountdown(boolean value) {
        Core.settings.put(PREFIX + "show-wave-countdown", value);
    }

    public static boolean showResources() {
        return Core.settings.getBool(PREFIX + "show-resources", true);
    }

    public static void showResources(boolean value) {
        Core.settings.put(PREFIX + "show-resources", value);
    }

    public static boolean showExpansionTargets() {
        return Core.settings.getBool(PREFIX + "show-expansion", true);
    }

    public static void showExpansionTargets(boolean value) {
        Core.settings.put(PREFIX + "show-expansion", value);
    }

    /** Seconds before the next wave at which the countdown is highlighted as a warning. */
    public static float waveWarningSeconds() {
        return Core.settings.getFloat(PREFIX + "wave-warning-seconds", 15f);
    }

    public static void waveWarningSeconds(float value) {
        Core.settings.put(PREFIX + "wave-warning-seconds", value);
    }
}
