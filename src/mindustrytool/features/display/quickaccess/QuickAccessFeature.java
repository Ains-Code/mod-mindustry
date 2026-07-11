package mindustrytool.features.display.quickaccess;

import java.util.Optional;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.input.KeyCode;
import arc.math.Interp;
import arc.math.Mathf;
import arc.scene.Element;
import arc.scene.actions.Actions;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
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
 * A floating, draggable launcher button that expands into a horizontal pill
 * of feature shortcuts, mirroring the "icon expand/collapse" mockup: a round
 * launcher icon that reveals a dark rounded panel of feature buttons plus a
 * close button when tapped.
 */
public class QuickAccessFeature extends Table implements Feature {
    /** Duration of the collapse/expand cross-fade, in seconds. */
    private static final float TOGGLE_FADE_OUT = 0.12f;
    private static final float TOGGLE_FADE_IN = 0.2f;
    /** Duration of the button hover "pop" effect, in seconds. */
    private static final float HOVER_DURATION = 0.12f;
    /** How far the launcher must move before a touch counts as a drag, not a tap. */
    private static final float DRAG_THRESHOLD = 6f;

    private static final Color BAR_BG = Color.valueOf("1c1f26ff");
    private static final Color ACCENT = Color.valueOf("4dd0e1ff");

    private BaseDialog settingsDialog;
    private boolean animating = false;

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
     * armed indicator). On failure we log and fall back to an empty widget
     * instead of propagating the exception.
     */
    void rebuild() {
        try {
            rebuildUnsafe();
        } catch (Exception e) {
            Log.err("[QuickAccessFeature] rebuild() failed, showing empty widget instead of crashing", e);
            try {
                clear();
                pack();
            } catch (Exception ignored) {
                // Even the fallback failed; nothing more we can safely do here.
            }
        }
    }

    private void rebuildUnsafe() {
        clear();

        boolean collapsed = QuickAccessConfig.collapsed();
        float scale = QuickAccessConfig.scale();
        float buttonSize = 40f * scale;
        float margin = 6f * scale;
        float launcherSize = 64f * scale;

        // Row container that will be dragged; holds the [panel][launcher] pair.
        Table container = new Table();
        container.touchable = Touchable.enabled;

        // Launcher: round always-visible icon button, fixed on the left (matches
        // the "icon expand/collapse" mockup). Tap toggles the panel; dragging
        // (past a small threshold) moves the whole widget instead.
        Table launcherWrap = new Table();
        launcherWrap.background(Tex.pane);
        launcherWrap.setColor(BAR_BG.r, BAR_BG.g, BAR_BG.b, 1f);

        Button launcher = launcherWrap.button(b -> b.image(collapsed ? Icon.menu : Icon.left)
                .scaling(Scaling.fit)
                .size(26f * scale)
                .color(collapsed ? Color.white : ACCENT), Styles.clearNonei, () -> {
                })
                .size(launcherSize)
                .tooltip(collapsed ? "@quickaccess.expand" : "@quickaccess.collapse")
                .get();
        addHoverPop(launcher);

        launcher.addListener(new InputListener() {
            float lastX, lastY, startX, startY;
            boolean dragged = false;

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                lastX = x;
                lastY = y;
                startX = x;
                startY = y;
                dragged = false;
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                if (Mathf.dst(x, y, startX, startY) > DRAG_THRESHOLD) {
                    dragged = true;
                }
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

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
                if (!dragged) {
                    toggleCollapsed();
                }
            }
        });

        container.add(launcherWrap).size(launcherSize);

        if (!collapsed) {
            // Expanded panel: rounded dark pill holding feature shortcuts + close
            // btn, growing out to the RIGHT from behind the fixed launcher icon.
            Table panel = new Table();
            panel.background(Tex.pane);
            panel.setColor(BAR_BG.r, BAR_BG.g, BAR_BG.b, QuickAccessConfig.opacity());

            populateContent(panel, buttonSize, margin);

            Button closeBtn = panel
                    .button(Icon.cancel, Styles.clearNonei, this::toggleCollapsed)
                    .size(28f * scale)
                    .margin(4f * scale)
                    .tooltip("@quickaccess.collapse")
                    .get();
            addHoverPop(closeBtn);

            container.add(panel).padLeft(8f * scale);
        }

        add(container).pad(0).margin(0);
        pack();
        keepInScreen();
    }

    /** Smoothly hides or reveals the button grid via a short cross-fade. */
    private void toggleCollapsed() {
        if (animating) {
            return;
        }

        boolean newState = !QuickAccessConfig.collapsed();
        QuickAccessConfig.collapsed(newState);

        animating = true;
        clearActions();
        addAction(Actions.sequence(
                Actions.fadeOut(TOGGLE_FADE_OUT, Interp.pow2In),
                Actions.run(() -> {
                    rebuild();
                    color.a = 0f;
                }),
                Actions.fadeIn(TOGGLE_FADE_IN, Interp.pow2Out),
                Actions.run(() -> animating = false)));
    }

    /** Adds a subtle scale-up "pop" on hover/press for extra tactile feedback. */
    private void addHoverPop(Button element) {
        element.setTransform(true);
        element.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Element fromActor) {
                element.clearActions();
                element.addAction(Actions.scaleTo(1.12f, 1.12f, HOVER_DURATION, Interp.pow2Out));
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Element toActor) {
                element.clearActions();
                element.addAction(Actions.scaleTo(1f, 1f, HOVER_DURATION, Interp.pow2Out));
            }
        });
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

    private void populateContent(Table t, float buttonSize, float margin) {
        Seq<Feature> features = FeatureManager.getInstance().getFeatures();

        for (Feature f : features) {
            if (f == this) {
                continue;
            }

            try {
                addFeatureButton(t, f, buttonSize, margin);
            } catch (Exception e) {
                // Isolate per-feature failures so one broken button doesn't take
                // down the whole panel (and, transitively, the rest of the HUD).
                Log.err("[QuickAccessFeature] failed to add button for " + f.getClass().getSimpleName(), e);
            }
        }

        Button settingsBtn = t
                .button(b -> b.image(Utils.scalable(Icon.settings)).scaling(Scaling.fit), Styles.clearNonei, () -> {
                    Main.featureSettingDialog.show();
                })
                .size(buttonSize)
                .margin(margin)
                .get();
        addHoverPop(settingsBtn);
    }

    private void addFeatureButton(Table t, Feature f, float buttonSize, float margin) {
        FeatureMetadata meta = f.getMetadata();

        if (!meta.quickAccess()) {
            return;
        }

        if (!QuickAccessConfig.isFeatureVisible(meta.name())) {
            return;
        }

        Button[] btnRef = new Button[1];
        long[] pressTime = { -1 };
        boolean[] longPressed = { false };

        btnRef[0] = t.button(b -> {
            b.image(meta.icon())
                    .scaling(Scaling.fit)
                    .update(l -> l.setColor(f.isEnabled() ? ACCENT : Pal.gray));
        }, Styles.clearNonei, () -> {
            if (!longPressed[0]) {
                FeatureManager.getInstance().toggle(f);
            }
        })
                .size(buttonSize)
                .margin(margin)
                .tooltip(meta.name())
                .get();

        addHoverPop(btnRef[0]);

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
                    // Smooth fade-in the first time the panel appears.
                    color.a = 0f;
                    clearActions();
                    addAction(Actions.fadeIn(0.3f, Interp.pow2Out));
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
