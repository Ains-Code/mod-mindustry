// ==========================================
// Ore Scanner Tool - main.js
// Isang "tool" item na kapag hawak ng player
// (o ginamit bilang consumable), nag-re-reveal
// ng lahat ng ore/resource tiles sa loob ng
// isang radius sa paligid niya.
// ==========================================

// --- 1. Gumawa tayo ng bagong Item: "ore-scanner" ---
const scannerItem = new Item("ore-scanner");
scannerItem.description = "Isang portable scanner na nagre-reveal ng mga ore deposit sa paligid mo. I-tap lang malapit sa isang core o drill para gumana.";
scannerItem.localizedName = "Ore Scanner";
scannerItem.color = Color.valueOf("ffd37f");
scannerItem.hardness = 0;
scannerItem.cost = 1; // gaano "kabigat" ito sa mga recipe/build cost

// --- 2. Config ng scan radius (sa tiles) ---
const SCAN_RADIUS = 12;
const SCAN_COOLDOWN = 60 * 8; // 8 segundo (60 ticks = 1 sec)

// Bawat player, i-store natin yung huling oras na nag-scan sila
// (simpleng cooldown para di ito abusuhin nang walang limitasyon)
const lastScanTime = {};

// --- 3. Function na nagre-reveal ng mga ore tile sa paligid ---
function scanForOres(player) {
  const team = player.team();
  const px = player.tileX();
  const py = player.tileY();

  let found = 0;

  for (let dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
    for (let dy = -SCAN_RADIUS; dy <= SCAN_RADIUS; dy++) {
      // bilugang radius, hindi kwadrado
      if (dx * dx + dy * dy > SCAN_RADIUS * SCAN_RADIUS) continue;

      const tile = Vars.world.tile(px + dx, py + dy);
      if (tile == null) continue;

      const ore = tile.overlay();
      // check kung ang overlay ng tile ay isang ore/drop-able resource
      if (ore != null && ore.itemDrop != null) {
        found++;

        // Visual indicator: gumawa ng temporary effect marker
        Call.effect(Fx.mineImpact, tile.worldx(), tile.worldy(), 0, ore.itemDrop.color);
      }
    }
  }

  if (found > 0) {
    Call.sendMessage("[accent]Ore Scanner:[] " + found + " na ore tile ang natagpuan sa paligid mo!");
  } else {
    Call.sendMessage("[accent]Ore Scanner:[] Walang nakitang ore sa loob ng " + SCAN_RADIUS + " tiles.");
  }
}

// --- 4. I-trigger ang scan kapag nag-tap ang player habang may hawak na scanner ---
Events.on(TapEvent, e => {
  const player = e.player;
  if (player == null) return;

  const unit = player.unit();
  if (unit == null) return;

  // Tignan kung ang item na pinili/naka-hawak ng player ay ang scanner
  // (dito natin gamit ang player's "mineral" o kung anong stack ang naka-select
  // - halimbawa lang: kung ang unit ay may dalang ore-scanner sa cargo nito)
  const stack = unit.stack;
  if (stack != null && stack.item == scannerItem && stack.amount > 0) {
    const now = Time.millis();
    const last = lastScanTime[player.id] || 0;

    if (now - last < (SCAN_COOLDOWN / 60) * 1000) {
      const secondsLeft = Math.ceil(((SCAN_COOLDOWN / 60) * 1000 - (now - last)) / 1000);
      Call.sendMessage("[scarlet]Ore Scanner:[] Naka-cooldown pa (" + secondsLeft + "s).");
      return;
    }

    lastScanTime[player.id] = now;
    scanForOres(player);
  }
});

// ==========================================
// Builder Assistant Drone
// Isang item na kapag ginamit, magde-deploy
// ng isang "helper drone" na awtomatikong
// tutulong mag-build/assist ng mga structure
// sa paligid ng player (gamit ang existing
// support-unit AI ng Mindustry, tulad ng
// ginagawa ng Mono/Poly units).
// ==========================================

const assistantItem = new Item("builder-assistant");
assistantItem.description = "Isang core na kapag na-activate, magde-deploy ng helper drone na awtomatikong tutulong mag-construct ng mga building sa paligid mo.";
assistantItem.localizedName = "Builder Drone Core";
assistantItem.color = Color.valueOf("8cc8ff");
assistantItem.hardness = 0;
assistantItem.cost = 1;

// --- Config ---
const MAX_ASSISTANTS_PER_PLAYER = 3;   // max sabay-sabay na drone bawat player
const ASSISTANT_UNIT_TYPE = UnitTypes.mono; // gumagamit tayo ng existing "mono" support unit
                                              // dahil built-in na ang "auto-assist build" AI nito

// playerId -> array ng mga na-deploy na drone (Unit objects)
const activeAssistants = {};

function cleanupDeadAssistants(list) {
  for (let i = list.length - 1; i >= 0; i--) {
    if (list[i] == null || list[i].dead) {
      list.splice(i, 1);
    }
  }
}

function deployAssistant(player) {
  const unit = player.unit();
  if (unit == null) return;

  const stack = unit.stack;
  if (stack == null || stack.item != assistantItem || stack.amount <= 0) {
    Call.sendMessage("[scarlet]Builder Assistant:[] Wala kang Builder Drone Core na dala.");
    return;
  }

  const list = activeAssistants[player.id] || (activeAssistants[player.id] = []);
  cleanupDeadAssistants(list);

  if (list.length >= MAX_ASSISTANTS_PER_PLAYER) {
    Call.sendMessage("[scarlet]Builder Assistant:[] Max drones na naka-deploy (" +
      MAX_ASSISTANTS_PER_PLAYER + "/" + MAX_ASSISTANTS_PER_PLAYER + "). Hintayin munang mawala ang isa.");
    return;
  }

  // konsumo ng 1 item bilang "cost" ng pag-deploy
  stack.amount -= 1;

  // konting random offset para di magka-overlap ang mga drone
  const offsetX = Mathf.range(24);
  const offsetY = Mathf.range(24);

  const drone = ASSISTANT_UNIT_TYPE.create(player.team());
  drone.set(unit.x + offsetX, unit.y + offsetY);
  drone.add();

  list.push(drone);

  Call.sendMessage("[accent]Builder Assistant:[] Helper drone deployed! (" +
    list.length + "/" + MAX_ASSISTANTS_PER_PLAYER + ") Awtomatiko na siyang tutulong mag-build/repair malapit sa iyo.");
}

// I-deploy ang assistant kapag naka-hold ng "builder-assistant" item ang player
// at nag-double indicator (dito, gamit natin ulit ang tap, pero ibang item check)
Events.on(TapEvent, e => {
  const player = e.player;
  if (player == null) return;
  const unit = player.unit();
  if (unit == null) return;

  const stack = unit.stack;
  if (stack != null && stack.item == assistantItem && stack.amount > 0) {
    deployAssistant(player);
  }
});

// --- (Opsyonal) I-log sa console pag na-load ang mod, para malaman mong gumagana ---
Log.info("[Ore Scanner Tool] Mod loaded successfully!");
Log.info("[Builder Assistant] Drone deployment system ready!");
