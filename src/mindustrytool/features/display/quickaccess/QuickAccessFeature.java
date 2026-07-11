package mindustrytool.features.display.quickaccess;

import java.util.Optional;

import arc.Core;
import arc.Events;
import arc.input.KeyCode;
import arc.math.Interp;
import arc.math.Mathf;
import arc.graphics.Color;
import arc.scene.actions.Actions;
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
 * A draggable bar with an anchor icon, a separator, and feature buttons.
 * Tapping the anchor (without dragging) toggles {@link QuickAccessConfig#collapsed()}:
 * collapsed shows only the anchor icon, expanded reveals the separator, the
 * feature button row, and a close button, with a short fade/scale-in tween.
 */
public class QuickAccessFeature extends Table implements Feature {
    private static final float DRAG_TAP_THRESHOLD = 6f;
    private static final float TOGGLE_ANIM_DURATION = 0.22f;

    // Matches the purple/cyan palette from the icon-expand-collapse mockup
    // instead of the plain default black6 + Pal.accent look.
    private static final Color BAR_BG = Color.valueOf("14121fee");
    private static final Color ACCENT_PURPLE = Color.valueOf("b48bffff");
    private static final Color ACCENT_CYAN = Color.valueOf("4dd0e1ff");

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
        container.background(Tex.pane);
        container.setColor(BAR_BG.r, BAR_BG.g, BAR_BG.b, BAR_BG.a * QuickAccessConfig.opacity());
        container.touchable = Touchable.enabled; // Container catches touches

        float scale = QuickAccessConfig.scale();
        float buttonSize = 48f * scale;
        float margin = 8f * scale;

        // 1. Anchor: draggable, and toggles collapsed/expanded on a plain tap
        // (i.e. a touch that didn't move past DRAG_TAP_THRESHOLD).
        boolean collapsedNow = QuickAccessConfig.collapsed();
        container.button(Icon.move, Styles.clearNonei, () -> {
        })
                .size(buttonSize)
                .margin(margin)
                .get()
                .addListener(new InputListener() {
                    float lastX, lastY;
                    float downX, downY;
                    boolean dragged;

                    @Override
                    public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                        lastX = x;
                        lastY = y;
                        downX = x;
                        downY = y;
                        dragged = false;
                        return true;
                    }

                    @Override
                    public void touchDragged(InputEvent event, float x, float y, int pointer) {
                        try {
                            if (Mathf.dst(x, y, downX, downY) > DRAG_TAP_THRESHOLD) {
                                dragged = true;
                            }

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

                    @Override
                    public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
                        if (!dragged) {
                            if (QuickAccessConfig.collapsed()) {
                                QuickAccessConfig.collapsed(false);
                                rebuild();
                            } else {
                                collapseWithAnimation();
                            }
                        }
                    }
                });

        float sw = Core.graphics.getWidth();
        float sh = Core.graphics.getHeight();

        QuickAccessFeature.this.x = Mathf.clamp(QuickAccessFeature.this.x, 0, sw - 40f);
        QuickAccessFeature.this.y = Mathf.clamp(QuickAccessFeature.this.y, 0, sh - 40f);

        QuickAccessConfig.x(QuickAccessFeature.this.x);
        QuickAccessConfig.y(QuickAccessFeature.this.y);

        if (!collapsedNow) {
            // 2. Separator (only when expanded)
            Image sep = new Image(Tex.whiteui);
            sep.setColor(ACCENT_PURPLE);
            container.add(sep).width(2f).fillY();

            // 3. Content: feature buttons + a close button that collapses the bar
            Table content = new Table();
            populateContent(content);
            content.button(b -> b.image(Icon.cancelSmall).color(ACCENT_CYAN), Styles.clearNonei,
                    this::collapseWithAnimation)
                    .size(buttonSize)
                    .margin(margin);
            container.add(content);

            // Short fade + scale-in so expanding doesn't just pop into existence.
            // Touch is disabled for the duration so a tap can't land on a button
            // before it's actually visible.
            float targetAlpha = BAR_BG.a * QuickAccessConfig.opacity();
            container.setOrigin(0f, container.getPrefHeight() / 2f);
            container.setTransform(true);
            container.setColor(BAR_BG.r, BAR_BG.g, BAR_BG.b, 0f);
            container.setScale(0.85f, 0.85f);
            container.touchable = Touchable.disabled;
            container.actions(
                    Actions.sequence(
                            Actions.parallel(
                                    Actions.alpha(targetAlpha, TOGGLE_ANIM_DURATION, Interp.fade),
                                    Actions.scaleTo(1f, 1f, TOGGLE_ANIM_DURATION, Interp.pow3Out)),
                            Actions.run(() -> container.touchable = Touchable.enabled)));
        }

        add(container).pad(0).margin(0);
        pack();
        keepInScreen();
    }

    /**
     * Fades and shrinks the currently-open panel out, then flips
     * {@link QuickAccessConfig#collapsed()} and rebuilds once the animation
     * finishes, so collapsing looks like the reverse of expanding instead of
     * just vanishing.
     */
    private void collapseWithAnimation() {
        if (getChildren().isEmpty()) {
            QuickAccessConfig.collapsed(true);
            rebuild();
            return;
        }

        Table currentContainer = (Table) getChildren().first();
        currentContainer.touchable = Touchable.disabled;
        currentContainer.setTransform(true);
        currentContainer.setOrigin(0f, currentContainer.getHeight() / 2f);
        currentContainer.actions(
                Actions.sequence(
                        Actions.parallel(
                                Actions.alpha(0f, TOGGLE_ANIM_DURATION, Interp.fade),
                                Actions.scaleTo(0.85f, 0.85f, TOGGLE_ANIM_DURATION, Interp.pow3In)),
                        Actions.run(() -> {
                            QuickAccessConfig.collapsed(true);
                            rebuild();
                        })));
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
        t.background(Tex.pane);
        t.setColor(BAR_BG.r, BAR_BG.g, BAR_BG.b, BAR_BG.a);

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
