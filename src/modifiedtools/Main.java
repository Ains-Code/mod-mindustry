package modifiedtools;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.func.Prov;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Reflect;
import mindustry.Vars;
import mindustry.editor.MapResizeDialog;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.mod.Mods.LoadedMod;
import mindustry.net.Packet;
import mindustry.mod.Mod;
import modifiedtools.features.FeatureManager;
import modifiedtools.features.auth.AuthService;
import modifiedtools.features.browser.map.MapBrowserFeature;
import modifiedtools.features.browser.schematic.SchematicBrowserFeature;
import modifiedtools.features.campaign.CampaignFeature;
import modifiedtools.features.display.healthbar.HealthBarVisualizer;
import modifiedtools.features.display.pathfinding.PathfindingDisplay;
import modifiedtools.features.display.teamresource.TeamResourceFeature;
import modifiedtools.features.display.togglerendering.ToggleRenderingFeature;
import modifiedtools.features.display.range.RangeDisplay;
import modifiedtools.features.display.spawnindicator.SpawnIndicatorFeature;
import modifiedtools.features.display.progress.ProgressDisplay;
import modifiedtools.features.display.quickaccess.QuickAccessFeature;
import modifiedtools.features.settings.FeatureSettingDialog;
import modifiedtools.features.conveyormaker.ConveyorMakerFeature;
import modifiedtools.features.smartupgrade.SmartUpgradeFeature;
import modifiedtools.features.smartdeconstruct.SmartDeconstructFeature;
import modifiedtools.features.display.corecapacity.CoreCapacityWarning;
import modifiedtools.features.time.TimeControlFeature;
import modifiedtools.features.smartdrill.SmartDrillFeature;
import modifiedtools.services.ServerService;
import modifiedtools.services.TapListener;
import modifiedtools.services.CrashReportService;
import modifiedtools.services.UpdateService;
import modifiedtools.features.godmode.GodModeFeature;
import modifiedtools.features.autoplay.AutoplayFeature;
import modifiedtools.features.background.BackgroundFeature;
import modifiedtools.features.music.MusicFeature;
import modifiedtools.features.music.dto.MusicRegisterEvent;
import modifiedtools.features.display.wavepreview.WavePreviewFeature;
import modifiedtools.features.savesync.SaveSyncFeature;
import modifiedtools.features.auth.AuthService;
import modifiedtools.features.assistantbuilder.AssistantBuilderFeature;

public class Main extends Mod {
    public static LoadedMod self;

    public static Fi imageDir = Vars.dataDirectory.child("mindustry-tool-caches");
    public static Fi mapsDir = Vars.dataDirectory.child("mindustry-tool-maps");
    public static Fi schematicDir = Vars.dataDirectory.child("mindustry-tool-schematics");
    public static Fi backgroundsDir = Vars.dataDirectory.child("mindustry-tool-backgrounds");
    public static Fi musicsDir = Vars.dataDirectory.child("mindustry-tool-musics");

    private static ObjectMap<Class<?>, Prov<? extends Packet>> packetReplacements = new ObjectMap<>();

    public static FeatureSettingDialog featureSettingDialog;

    public static void registerPacketPlacement(Class<?> clazz, Prov<? extends Packet> prov) {
        packetReplacements.put(clazz, prov);
    }

    public Main() {
        Vars.maxSchematicSize = 4000;
        MapResizeDialog.maxSize = 4000;
    }

    @Override
    public void init() {
        self = Vars.mods.getMod(Main.class);

        if (self == null) {
            Core.app.post(() -> {
                Vars.ui.showErrorMessage("Mod cant find itself, please contact admin on Discord to fix the problem\n"
                        + Config.DISCORD_INVITE_URL);
            });
            return;
        }

        Events.on(ClientLoadEvent.class, e -> {
            try {
                featureSettingDialog = new FeatureSettingDialog();

                addCustomButtons();

                UpdateService.getInstance().checkForUpdate(() -> {
                    Core.app.post(() -> {
                        try {
                            setup();
                        } catch (Exception err) {
                            Core.app.post(() -> {
                                Vars.ui.showException(err);
                            });
                        }
                    });
                });
            } catch (Exception err) {
                Core.app.post(() -> {
                    Vars.ui.showException(err);
                });
            }
        });
    }

    private void setup() {
        imageDir.mkdirs();
        mapsDir.mkdirs();
        backgroundsDir.mkdirs();
        musicsDir.mkdirs();
        schematicDir.mkdirs();

        checkDirVersion(imageDir, 1);
        checkDirVersion(mapsDir, 1);
        checkDirVersion(schematicDir, 1);

        AuthService.getInstance().init();
        ServerService.getInstance().init();
        TapListener.getInstance().init();

        FeatureManager.getInstance().register(//
                new MapBrowserFeature(), //
                new SchematicBrowserFeature(), //
                new HealthBarVisualizer(), //
                new TeamResourceFeature(),
                new PathfindingDisplay(), //
                new RangeDisplay(), //
                new SpawnIndicatorFeature(), //
                new QuickAccessFeature(), //
                new AutoplayFeature(),
                new WavePreviewFeature(),
                new SaveSyncFeature(),
                // new ItemVisualizerFeature(),
                new GodModeFeature(),
                new SmartDrillFeature(),
                new SmartUpgradeFeature(),
                new SmartDeconstructFeature(),
                new ConveyorMakerFeature(),
                new BackgroundFeature(),
                new MusicFeature(),
                new ProgressDisplay(),
                new CoreCapacityWarning(),
                new ToggleRenderingFeature(),
                new TimeControlFeature(),
                new CampaignFeature(),
                new AssistantBuilderFeature());

        boolean hasCrashed = new CrashReportService().checkForCrashes();
        if (hasCrashed) {
            // Try to disable all feature
            FeatureManager.getInstance().disableAll();
        }
        initFeatures();

        Events.fire(new MusicRegisterEvent());
        Events.fire(new MdtInitEvent());
    }

    private void initFeatures() {
        FeatureManager.getInstance().init();

        Seq<Prov<? extends Packet>> packetProvs = Reflect.get(Vars.net, "packetProvs");

        packetProvs.replace(packet -> {
            Class<?> clazz = packet.get().getClass();
            if (packetReplacements.containsKey(clazz)) {
                Prov<? extends Packet> replacement = packetReplacements.remove(clazz);
                Log.info("Replace packet @ to @", clazz.getSimpleName(),
                        replacement.get().getClass().getSimpleName());
                return replacement;
            }

            return packet;
        });

        for (Class<?> clazz : packetReplacements.keys()) {
            Log.info("Packet @ not found", clazz.getSimpleName());
        }
    }

    private void addCustomButtons() {
        Core.app.post(() -> {
            try {
                Vars.ui.menufrag.addButton("Mindustry Tool", Utils.icons("mod.png"), () -> featureSettingDialog.show());
            } catch (Exception err) {
                Vars.ui.showException(err);
            }
        });
    }

    private int readDirVersion(Fi dir) {
        try {
            Fi versionFile = dir.child("version.txt");
            if (versionFile.exists()) {
                return Integer.parseInt(versionFile.readString());
            } else {
                return -1;
            }
        } catch (Exception err) {
            return 0;
        }
    }

    private void writeDirVersion(Fi dir, int version) {
        try {
            dir.emptyDirectory(false);
            Fi versionFile = dir.child("version.txt");
            versionFile.writeString(version + "");
        } catch (Exception err) {
            Core.app.post(() -> {
                Vars.ui.showException(err);
            });
        }
    }

    private void checkDirVersion(Fi dir, int expectedVersion) {
        try {
            int version = readDirVersion(dir);
            if (version == -1) {
                dir.mkdirs();
            }

            if (version != expectedVersion) {
                writeDirVersion(dir, expectedVersion);
            }
        } catch (Exception err) {
            Log.err("Check dir version failed", err);
        }
    }
}
