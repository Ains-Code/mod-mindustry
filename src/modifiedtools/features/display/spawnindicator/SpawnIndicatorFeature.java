package modifiedtools.features.display.spawnindicator;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Rect;
import arc.scene.ui.Dialog;
import arc.util.Align;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Icon;
import mindustry.graphics.Layer;
import mindustry.ui.Fonts;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.Tile;
import modifiedtools.features.Feature;
import modifiedtools.features.FeatureMetadata;

import java.util.Optional;

/**
 * Marks active enemy spawn points on the map with a pulsing indicator, and
 * draws an arrow at the edge of the screen pointing towards any spawn point
 * that is currently off-screen, so incoming waves are never a surprise.
 */
public class SpawnIndicatorFeature implements Feature {
    private static final float MARKER_RADIUS = 7f;
    private static final float EDGE_MARGIN = 28f;
    private static final float ARROW_SIZE = 9f;

    private BaseDialog dialog;
    private final Rect viewBounds = new Rect();

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("@feature.spawn-indicator")
                .description("@feature.spawn-indicator.description")
                .icon(Icon.warning)
                .order(6)
                .enabledByDefault(true)
                .quickAccess(true)
                .build();
    }

    @Override
    public void init() {
        SpawnIndicatorConfig.load();
        Events.run(Trigger.draw, this::draw);
    }

    @Override
    public Optional<Dialog> setting() {
        if (dialog == null) {
            dialog = new SpawnIndicatorSettingsDialog();
        }
        return Optional.of(dialog);
    }

    private void draw() {
        if (!isEnabled() || !Vars.state.isGame() || Vars.ui.hudfrag == null || !Vars.ui.hudfrag.shown) {
            return;
        }

        if (!Vars.state.rules.waves || Vars.spawner.getSpawns() == null) {
            return;
        }

        float cx = Core.camera.position.x;
        float cy = Core.camera.position.y;
        float cw = Core.camera.width;
        float ch = Core.camera.height;

        Core.camera.bounds(viewBounds);

        Color color = Vars.state.rules.waveTeam.color;
        float secondsLeft = Math.max(0f, Vars.state.wavetime / 60f);
        float pulse = Mathf.absin(Time.time, 6f, 1f);

        Draw.z(Layer.overlayUI);

        for (Tile tile : Vars.spawner.getSpawns()) {
            if (tile == null) {
                continue;
            }

            float x = tile.worldx();
            float y = tile.worldy();

            if (viewBounds.contains(x, y)) {
                if (SpawnIndicatorConfig.showOnScreenMarker) {
                    drawOnScreenMarker(x, y, color, pulse, secondsLeft);
                }
            } else if (SpawnIndicatorConfig.showOffScreenArrow) {
                drawOffScreenArrow(cx, cy, cw, ch, x, y, color, pulse, secondsLeft);
            }
        }

        Draw.reset();
    }

    private void drawOnScreenMarker(float x, float y, Color color, float pulse, float secondsLeft) {
        Draw.color(color, SpawnIndicatorConfig.opacity);
        Lines.stroke(2f);
        Lines.circle(x, y, MARKER_RADIUS + pulse * 3f);

        Fill.poly(x, y, 3, MARKER_RADIUS * 0.6f, 90f);
        Draw.reset();

        if (SpawnIndicatorConfig.showCountdown && secondsLeft > 0) {
            String text = String.format("%.0fs", secondsLeft);
            Fonts.outline.draw(text, x, y + MARKER_RADIUS + 10f, Color.white, 0.3f, false, Align.center);
        }
    }

    private void drawOffScreenArrow(float cx, float cy, float cw, float ch, float x, float y, Color color,
            float pulse, float secondsLeft) {
        float hw = cw / 2f - EDGE_MARGIN;
        float hh = ch / 2f - EDGE_MARGIN;

        float dx = x - cx;
        float dy = y - cy;

        if (Mathf.zero(dx) && Mathf.zero(dy)) {
            return;
        }

        float scaleX = Mathf.zero(dx) ? Float.MAX_VALUE : Math.abs(hw / dx);
        float scaleY = Mathf.zero(dy) ? Float.MAX_VALUE : Math.abs(hh / dy);
        float scale = Math.min(scaleX, scaleY);

        float ex = cx + dx * scale;
        float ey = cy + dy * scale;

        float angle = Angles.angle(cx, cy, x, y);

        Draw.color(color, SpawnIndicatorConfig.opacity * (0.7f + pulse * 0.3f));

        float tipX = ex + Mathf.cosDeg(angle) * ARROW_SIZE;
        float tipY = ey + Mathf.sinDeg(angle) * ARROW_SIZE;
        float baseX = ex - Mathf.cosDeg(angle) * ARROW_SIZE * 0.6f;
        float baseY = ey - Mathf.sinDeg(angle) * ARROW_SIZE * 0.6f;
        float leftX = baseX + Mathf.cosDeg(angle + 90f) * ARROW_SIZE * 0.6f;
        float leftY = baseY + Mathf.sinDeg(angle + 90f) * ARROW_SIZE * 0.6f;
        float rightX = baseX + Mathf.cosDeg(angle - 90f) * ARROW_SIZE * 0.6f;
        float rightY = baseY + Mathf.sinDeg(angle - 90f) * ARROW_SIZE * 0.6f;

        Fill.tri(tipX, tipY, leftX, leftY, rightX, rightY);
        Draw.reset();

        if (SpawnIndicatorConfig.showCountdown && secondsLeft > 0) {
            String text = String.format("%.0fs", secondsLeft);
            Fonts.outline.draw(text, ex, ey - ARROW_SIZE - 6f, Color.white, 0.3f, false, Align.center);
        }
    }
}
