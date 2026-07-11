package mindustrytool.features.display.quickaccess;

import java.util.Optional;

import arc.Core;
import arc.Events;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.graphics.Color;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.scene.ui.Image;
import arc.scene.ui.Button;
import arc.scene.ui.Dialog;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Scaling;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.Main;
import mindustrytool.Utils;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.FeatureMetadata;

/**
 * Matches the original upstream (MindustryTool/MindustryToolMod) design:
 * a simple draggable bar with an anchor, a separator, and always-visible
 * feature buttons. No collapse/expand, no custom styling on top.
 */
public class QuickAccessFeature extends Table implements Feature {
    private BaseDialog settingsDialog;

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("@feature.quick-access-hud")
                .description("@feature.quick-access-hud.description")
                .icon(Icon.menu)
                .build();
    }

    @Override
    public void init() {
        // Ensure we are not catching input for the whole screen if we were filling it
        // (we aren't)
        touchable = Touchable.childrenOnly;

        // Initial position
        setPosition(QuickAccessConfig.x(), QuickAccessConfig.y());

        // Build UI

        Events.on(EventType.ResizeEvent.class, event -> {
            this.rebuild();
            keepInScreen();
        });

        Core.app.post(() -> rebuild());
    }

    /**
     * Safe entry point: never lets a layout bug escape and take down the whole
     * game (which would trip the mod's crash detector and auto-disable every
     * other feature, including unrelated HUD elements like the health bar and
     * armed indicator). On failure we log and show a visible error badge
     * instead of silently leaving nothing on screen.
     */
    void rebuild() {
        try {
            rebuildUnsafe();
        } catch (Throwable e) {
            Log.err("[QuickAccessFeature] rebuild() failed, showing error badge instead of crashing", e);
            try {
                clear();
                Table errorBadge = new Table();
                errorBadge.background(Tex.pane);
                errorBadge.setColor(0.6f, 0.1f, 0.1f, 0.95f);
                errorBadge.add("QuickAccess error: " + e.getClass().getSimpleName())
                        .pad(6f)
                        .color(Color.white);
                add(errorBadge);
                pack();
            } catch (Exception ignored) {
                // Even the fallback failed; nothing more we can safely do here.
            }
        }
    }

    private void rebuildUnsafe() {
        clear();

        // Main container table that will be dragged
        Table container = new Table();
        container.background(Styles.black6);
        container.setColor(1f, 1f, 1f, QuickAccessConfig.opacity());
        container.touchable = Touchable.enabled; // Container catches touches

        float scale = QuickAccessConfig.scale();
        float buttonSize = 48f * scale;
        float margin = 8f * scale;

        // 1. Anchor (Draggable only)
        container.button(Icon.move, Styles.clearNonei, () -> {
        })
                .size(buttonSize)
                .margin(margin)
                .get()
                .addListener(new InputListener() {
                    float lastX, lastY;

                    @Override
                    public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                        lastX = x;
                        lastY = y;
                        return true;
                    }

                    @Override
                    public void touchDragged(InputEvent event, float x, float y, int pointer) {
                        try {
                            moveBy(x - lastX, y - lastY);

                            float sw = Core.graphics.getWidth();
                            float sh = Core.graphics.getHeight();

                            QuickAccessFeature.this.x = Mathf.clamp(QuickAccessFeature.this.x, 0, sw - 40f);
                            QuickAccessFeature.this.y = Mathf.clamp(QuickAccessFeature.this.y, 0, sh - 40f);

                            QuickAccessConfig.x(QuickAccessFeature.this.x);
                            QuickAccessConfig.y(QuickAccessFeature.this.y);
                            keepInScreen();
                        } catch (Exception e) {
                            Log.err(e);
                        }
                    }
                });

        float sw = Core.graphics.getWidth();
        float sh = Core.graphics.getHeight();

        QuickAccessFeature.this.x = Mathf.clamp(QuickAccessFeature.this.x, 0, sw - 40f);
        QuickAccessFeature.this.y = Mathf.clamp(QuickAccessFeature.this.y, 0, sh - 40f);

        QuickAccessConfig.x(QuickAccessFeature.this.x);
        QuickAccessConfig.y(QuickAccessFeature.this.y);

        // 2. Separator
        Image sep = new Image(Tex.whiteui);
        sep.setColor(Pal.accent);
        container.add(sep).width(2f).fillY();

        // 3. Content (Always visible)
        Table content = new Table();
        populateContent(content);
        container.add(content);

        add(container).pad(0).margin(0);
        pack();
        keepInScreen();
    }

    public void keepInScreen() {
        if (getScene() == null)
            return;

        float w = getWidth();
        float h = getHeight();
        float sw = getScene().getWidth();
        float sh = getScene().getHeight();

        if (x < 0)
            x = 0;
        if (y < 0)
            y = 0;
        if (x + w > sw)
            x = sw - w;
        if (y + h > sh)
            y = sh - h;
    }

    private void populateContent(Table t) {
        t.background(Styles.black6);

        Seq<Feature> features = FeatureManager.getInstance().getFeatures();
        int i = 0;
        int cols = QuickAccessConfig.cols();
        float scale = QuickAccessConfig.scale();
        float buttonSize = 48f * scale;
        float margin = 8f * scale;

        for (Feature f : features) {
            if (f == this) {
                continue;
            }

            try {
                i = addFeatureButton(t, f, buttonSize, margin, i, cols);
            } catch (Exception e) {
                // Isolate per-feature failures so one broken button doesn't take
                // down the whole panel (and, transitively, the rest of the HUD).
                Log.err("[QuickAccessFeature] failed to add button for " + f.getClass().getSimpleName(), e);
            }
        }

        t
                .button(b -> b.image(Utils.scalable(Icon.settings)).scaling(Scaling.fit), Styles.clearNonei, () -> {
                    Main.featureSettingDialog.show();
                })
                .size(buttonSize)
                .margin(margin)
                .get();
    }

    private int addFeatureButton(Table t, Feature f, float buttonSize, float margin, int i, int cols) {
        FeatureMetadata meta = f.getMetadata();

        if (!meta.quickAccess()) {
            return i;
        }

        if (!QuickAccessConfig.isFeatureVisible(meta.name())) {
            return i;
        }

        Button[] btnRef = new Button[1];
        long[] pressTime = { -1 };
        boolean[] longPressed = { false };

        btnRef[0] = t.button(b -> {
            b.image(meta.icon())
                    .scaling(Scaling.fit)
                    .update(l -> l.setColor(f.isEnabled() ? Color.white : Pal.gray));
        }, Styles.clearNonei, () -> {
            if (!longPressed[0]) {
                FeatureManager.getInstance().toggle(f);
            }
        })
                .size(buttonSize)
                .margin(margin)
                .tooltip(meta.name())
                .get();

        btnRef[0].update(() -> {
            if (btnRef[0].isPressed()) {
                if (pressTime[0] == -1) {
                    pressTime[0] = Time.millis();
                    longPressed[0] = false;
                } else if (!longPressed[0] && Time.timeSinceMillis(pressTime[0]) >= 300) {
                    longPressed[0] = true;
                    f.setting().ifPresent(Dialog::show);
                }
            } else {
                pressTime[0] = -1;
            }
        });

        i++;
        if (i % cols == 0) {
            t.row();
        }
        return i;
    }

    @Override
    public void onEnable() {
        if (Vars.ui != null && Vars.ui.hudGroup != null) {
            // Remove existing if any
            remove();

            name = "quick-access-hud";
            visible(() -> {
                try {
                    // Note: we intentionally don't gate on Vars.ui.hudfrag.shown here.
                    // Mindustry sets that to false in Command Mode (and a few other
                    // "clean view" states), which used to make this widget vanish
                    // even though the mod itself was working fine. We only care
                    // whether we're actually in a game.
                    return Vars.state.isGame();
                } catch (Exception e) {
                    // Never let a visibility check crash the frame; default to hidden.
                    return false;
                }
            });

            Core.app.post(() -> {
                try {
                    Vars.ui.hudGroup.addChild(this);
                    keepInScreen();
                } catch (Exception e) {
                    Log.err("[QuickAccessFeature] failed to attach to hudGroup", e);
                }
            });
        }
    }

    @Override
    public void onDisable() {
        remove();
    }

    @Override
    public Optional<Dialog> setting() {
        if (settingsDialog == null) {
            settingsDialog = new QuickAccessSettingsDialog(this);
        }
        return Optional.of(settingsDialog);
    }
}
