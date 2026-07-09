package mindustrytool.features.campaign;

import arc.Core;
import mindustry.ui.dialogs.BaseDialog;

public class CampaignSettingDialog extends BaseDialog {

    public CampaignSettingDialog() {
        super(Core.bundle.get("feature.campaign.settings", "Campaign Settings"));
        addCloseButton();
        setup();
    }

    private void setup() {
        cont.clear();
        cont.table(t -> {
            t.defaults().pad(6).left();

            t.check(Core.bundle.get("feature.campaign.show-wave-countdown", "Show wave countdown"),
                    CampaignConfig.showWaveCountdown(), val -> CampaignConfig.showWaveCountdown(val)).row();

            t.check(Core.bundle.get("feature.campaign.show-resources", "Show core resource snapshot"),
                    CampaignConfig.showResources(), val -> CampaignConfig.showResources(val)).row();

            t.check(Core.bundle.get("feature.campaign.show-expansion", "Show nearby expansion targets"),
                    CampaignConfig.showExpansionTargets(), val -> CampaignConfig.showExpansionTargets(val)).row();

            t.table(warn -> {
                warn.left();
                warn.add(Core.bundle.get("feature.campaign.wave-warning-seconds", "Wave warning (seconds)"))
                        .left().padRight(10);
                warn.label(() -> String.valueOf((int) CampaignConfig.waveWarningSeconds())).padRight(10).width(30);
                warn.slider(5, 60, 1, CampaignConfig.waveWarningSeconds(),
                        value -> CampaignConfig.waveWarningSeconds(value)).growX();
            }).growX().padTop(10).row();

            t.table(opacity -> {
                opacity.left();
                opacity.add(Core.bundle.get("feature.campaign.opacity", "Opacity")).left().padRight(10);
                opacity.slider(0.1f, 1f, 0.05f, CampaignConfig.opacity(),
                        value -> CampaignConfig.opacity(value)).growX();
            }).growX().padTop(10).row();
        }).grow();
    }
}
