# Project Structure

## Root folder: `src/modifiedtools`

- `Config.java`: Static configuration class.
- `Main.java`: Mod entry point.
- `Utils.java`: Static utility methods.
- `IconUtils.java`: Icon mapping utilities.
- `MdtInitEvent.java`: Init event fired after feature setup.
- `MdtKeybinds.java`: Key binding registration.
- `dto/`: Shared class resources.
- `ui/`: Shared UI components and dialogs.
- `utils/`: Shared utility helpers.
- `services/`: Service layer for business logic, data fetching.
- `features/`: Core functional modules.
   - `auth/`: Authentication.
   - `autoplay/`: Autoplay/AI unit control.
   - `background/`: Custom background.
   - `browser/`: Map and schematic browser.
      - `map/`: Map browser.
      - `schematic/`: Schematic browser.
   - `assistantbuilder/`: Auto-expanding schematic builder.
   - `campaign/`: Campaign sector/wave HUD panel.
   - `conveyormaker/`: Drag-to-place conveyor lines.
   - `smartdeconstruct/`: Bulk deconstruct connected block networks.
   - `smartdrill/`: Smart drill placement/management.
   - `smartupgrade/`: One-tap distribution chain upgrades.
   - `godmode/`: Sandbox/admin debug tools.
   - `music/`: Custom music import/management.
   - `savesync/`: Cloud save sync.
   - `settings/`: Settings.
   - `time/`: Time control.
   - `display/`: Display.
      - `healthbar/`: Health bar.
      - `itemvisualizer/`: Item flow visualizer.
      - `pathfinding/`: Enemy pathfinding.
      - `progress/`: Progress indicators.
      - `quickaccess/`: Quick access UI.
      - `range/`: Anything that has range.
      - `teamresource/`: Team resource tracking.
      - `togglerendering/`: Toggleable rendering elements.
      - `wavepreview/`: Incoming wave preview.
      - `corecapacity/`: Core storage capacity warning.
      - `spawnindicator/`: Enemy spawn point indicator.

**Group files into folders based on their functionality whenever possible.**

## Code Style

- Use standard Java naming conventions, only use meaningful names and avoid abbreviations.
- No magic number or string, use constant instead unless its for UI.
- Boolean variable should use `is` or `has` prefix.
- Apply DRY, KISS, and SOLID principles.
- Prefer immutability
- No wildcard imports (`*`)
- Avoid callback hell
- Single Responsibility Principle
- Prefer composition over inheritance
- Always use dependency injection (constructor injection)
- Clear exceptions & error handling
- Prefer Optional over null where appropriate
- Use java stream API, record and lambda expressions if applicable
- Limit to 4 method parameters
- Use/define components for UI elements whenever possible
- Avoid large nesting (3 levels or more), eary return whenever possible
- Avoid singleton
- Use translation key for text and add the key to bundle file

## Requirements

- Java 17
- Mindustry v8 (build 159)
