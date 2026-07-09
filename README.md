# Ore Scanner Tool — Mindustry Mod

Isang simpleng **tool item** para sa Mindustry na nagre-reveal ng mga ore
deposit sa paligid ng player kapag ginamit.

## Paano gumagana

### 1. Ore Scanner (gold/yellow chip)
- Kapag ang unit mo ay may dalang Ore Scanner sa cargo, at nag-tap ka sa
  screen, mag-a-activate ang scan.
- Ire-reveal nito (visually mamarkahan) ang lahat ng ore tiles sa loob ng
  12-tile radius sa paligid mo.
- May 8-second cooldown bawat scan para hindi ito abusuhin.

### 2. Builder Drone Core (blue/cyan core) — "Assistant Builder"
- Kapag may dala kang Builder Drone Core at nag-tap ka, magde-deploy ito ng
  isang **helper drone** (gamit ang `mono` unit type) malapit sa iyo.
- Ang drone ay awtomatikong tutulong mag-**build/repair** ng mga structure
  sa paligid gamit ang built-in na "support unit AI" ng Mindustry — ganito
  rin ang ginagawa ng mono/poly units sa vanilla game.
- Bawat deploy, gumagastos ng 1 Builder Drone Core mula sa cargo mo.
- May limitasyon: max **3 drone** sabay-sabay bawat player. Kapag namatay o
  nawala ang isang drone, pwede ka na ulit mag-deploy ng bago.
- May message na ipapakita sa chat kapag successful ang deploy, kapag naka-
  max limit, o kapag walang dalang core.

## Paano i-install
1. I-download at i-extract (o direktang gamitin) ang `OreScannerMod` folder
   / zip.
2. Buksan ang Mindustry.
3. Pumunta sa **Mods** menu (sa main menu o sa settings).
4. I-click ang **"Import mod"** (o ilagay ang buong folder/zip sa
   Mindustry mods directory, karaniwan ay:
   - Windows: `%appdata%/Mindustry/mods`
   - Android: `/storage/emulated/0/Android/data/mindustry/files/mods`
   - Linux: `~/.local/share/Mindustry/mods`
5. I-restart ang Mindustry, i-enable ang mod sa Mods list.

## Ideas para paunlarin pa
- Gawing craftable/buildable item (idagdag sa isang factory recipe).
- Palitan ang visual marker ng permanenteng minimap overlay habang naka-hold
  ang scanner.
- Idagdag ang parehong logic bilang isang **Block** (hal. "Scanner Tower")
  imbes na item, kung gusto mo itong structure-based sa halip na
  inventory-based.

## Note
Ang script (`scripts/main.js`) ay gamit ang Mindustry JS modding API
(`Item`, `Events`, `Call`, `Vars`, `UnitTypes`). Kung may pagbabago sa API
sa bagong Mindustry version, baka kailanganin ayusin ang ilang function
calls (hal. `tile.overlay()`, `unit.stack`, `UnitTypes.mono.create()`)
depende sa exact na version ng laro.

Para sa **Builder Drone**, ginamit natin ang `mono` bilang base unit type
dahil built-in na dito ang "auto-assist nearby construction" na AI behavior
sa vanilla Mindustry — kaya hindi na kailangan pang isulat mula zero ang
buong pathfinding/build logic. Kung gusto mo ng ibang itsura o mas malakas
na drone, pwede palitan ang `UnitTypes.mono` ng ibang unit type (hal.
`UnitTypes.poly` para sa mas malaking build-assist unit).
