package mindustrytool.features.display.quickaccess;

import java.util.Optional;

import arc.Core;
import arc.Events;
import arc.input.KeyCode;
import arc.math.Interp;
import arc.math.Mathf;
import arc.graphics.Color;
import arc.scene.Element;
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

public class QuickAccessFeature extends Table implements Feature {
    /** Duration of the collapse/expand cross-fade, in seconds. */
    private static final float TOGGLE_FADE_OUT = 0.12f;
    private static final float TOGGLE_FADE_IN = 0.2f;
    /** Duration of the button hover "pop" effect, in seconds. */
    private static final float HOVER_DURATION = 0.12f;

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

    void rebuild() {
        clear();

        boolean collapsed = QuickAccessConfig.collapsed();

        // Main container table that will be dragged
        Table container = new Table();
        container.background(Tex.pane);
        container.setColor(1f, 1f, 1f, QuickAccessConfig.opacity());
        container.touchable = Touchable.enabled; // Container catches touches

        float scale = QuickAccessConfig.scale();
        float buttonSize = 48f * scale;
        float margin = 8f * scale;

        // 1. Anchor (Draggable only)
        Button anchor = container.button(Icon.move, Styles.clearNonei, () -> {
        })
                .size(buttonSize)
                .margin(margin)
                .tooltip("@quickaccess.drag")
                .get();

        anchor.addListener(new InputListener() {
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

        // 2. Collapse / expand toggle - always visible so the panel can be
        // recovered even when its content is hidden. Tinted like an on/off
        // switch: accent = expanded, gray = collapsed.
        Button toggle = container.button(b -> b.image(collapsed ? Icon.right : Icon.left)
                .scaling(Scaling.fit)
                .color(collapsed ? Pal.gray : Pal.accent), Styles.clearNonei, this::toggleCollapsed)
                .size(buttonSize)
                .margin(margin)
                .tooltip(collapsed ? "@quickaccess.expand" : "@quickaccess.collapse")
                .get();
        addHoverPop(toggle);

        if (!collapsed) {
            // 3. Separator
            Image sep = new Image(Tex.whiteui);
            sep.setColor(Pal.accent);
            container.add(sep).width(2f).fillY().pad(2f);

            // 4. Content (feature buttons + settings)
            Table content = new Table();
            populateContent(content);
            container.add(content);
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

    private void populateContent(Table t) {
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

            FeatureMetadata meta = f.getMetadata();

            if (!meta.quickAccess()) {
                continue;
            }

            if (!QuickAccessConfig.isFeatureVisible(meta.name())) {
                continue;
            }

            Button[] btnRef = new Button[1];
            long[] pressTime = { -1 };
            boolean[] longPressed = { false };

            btnRef[0] = t.button(b -> {
                b.image(meta.icon())
                        .scaling(Scaling.fit)
                        .update(l -> l.setColor(f.isEnabled() ? Pal.accent : Pal.gray));
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

            if (++i % cols == 0)
                t.row();
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

    @Override
    public void onEnable() {
        if (Vars.ui != null && Vars.ui.hudGroup != null) {
            // Remove existing if any
            remove();

            name = "quick-access-hud";
            visible(() -> Vars.ui.hudfrag.shown && Vars.state.isGame());

            Core.app.post(() -> {
                Vars.ui.hudGroup.addChild(this);
                // Smooth fade-in the first time the panel appears.
                color.a = 0f;
                clearActions();
                addAction(Actions.fadeIn(0.3f, Interp.pow2Out));
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
