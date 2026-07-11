package modifiedtools.features.display.range;

import arc.Core;

public class RangeDisplayConfig {
    public static float opacity = 1f;
    
    // ally
    public static boolean drawBlockRangeAlly = true;
    public static boolean drawUnitRangeAlly = true;
    public static boolean drawTurretRangeAlly = true;
    // enemy    
    public static boolean drawBlockRangeEnemy = true;
    public static boolean drawUnitRangeEnemy = true;
    public static boolean drawTurretRangeEnemy = true;
    //other
    // public static boolean drawPlayerRange = true;
    public static boolean drawSpawnerRange = true;

    public static void load() {
        opacity = Core.settings.getFloat("modifiedtools.range.opacity", 1f);
        drawBlockRangeAlly = Core.settings.getBool("modifiedtools.range.draw-block-range-ally", true);
        drawBlockRangeEnemy = Core.settings.getBool("modifiedtools.range.draw-block-range-enemy", true);
        drawUnitRangeAlly = Core.settings.getBool("modifiedtools.range.draw-unit-range-ally", true);
        drawUnitRangeEnemy = Core.settings.getBool("modifiedtools.range.draw-unit-range-enemy", true);
        drawTurretRangeAlly = Core.settings.getBool("modifiedtools.range.draw-turret-range-ally", true);
        drawTurretRangeEnemy = Core.settings.getBool("modifiedtools.range.draw-turret-range-enemy", true);
        // drawPlayerRange = Core.settings.getBool("modifiedtools.range.draw-player-range", true);
        drawSpawnerRange = Core.settings.getBool("modifiedtools.range.draw-spawner-range", true);
    }

    public static void save() {
        Core.settings.put("modifiedtools.range.opacity", opacity);
        Core.settings.put("modifiedtools.range.draw-block-range-ally", drawBlockRangeAlly);
        Core.settings.put("modifiedtools.range.draw-block-range-enemy", drawBlockRangeEnemy);
        Core.settings.put("modifiedtools.range.draw-unit-range-ally", drawUnitRangeAlly);
        Core.settings.put("modifiedtools.range.draw-unit-range-enemy", drawUnitRangeEnemy);
        Core.settings.put("modifiedtools.range.draw-turret-range-ally", drawTurretRangeAlly);
        Core.settings.put("modifiedtools.range.draw-turret-range-enemy", drawTurretRangeEnemy);
        // Core.settings.put("modifiedtools.range.draw-player-range", drawPlayerRange);
        Core.settings.put("modifiedtools.range.draw-spawner-range", drawSpawnerRange);
    }
}
