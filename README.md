# BedWars1058 Directional Fireball Suite

Custom fireball handling designed by **Hypnr** for competitive BedWars1058 networks. The suite replaces vanilla knockback with a physics-aware system that reacts to player movement, aim, and the exact blast offset—delivering reliable jump tech and consistent PvP outcomes.

## Feature Highlights
- **Dynamic Knockback Models** – Self, semi-side, and side impacts blend smoothly based on pitch, velocity, and explosion alignment. Backwards and stationary drops are treated with purpose-built rules for predictable launches.
- **Configurable Throwing Module** – Optional manual fireball throwing with cooldown, sound cues, and speed scaling.
- **Arena-Friendly Safeguards** – Fire placement, block protection, and BedWars1058 awareness keep gameplay legal and grief-free.
- **Hot Reload Ready** – `/fb reload` applies config changes without a reboot; status commands prevent double toggles.

## Installation & Compatibility
1. Install **BedWars1058** (tested on API 25.2) on a Spigot/Paper 1.8.8 network.
2. Drop `DirectionalFBAddon-1.4.2-shaded.jar` into `/plugins`.
3. Restart or reload the server.
4. Edit `plugins/BedWars1058-DirectionalFBAddon/config.yml` to match your ladder or tournament rules.

## Commands & Permissions
| Command | Description | Permission |
|---------|-------------|------------|
| `/fb reload` | Reloads config and reapplies tuning in-game. | `fbaddon.admin` |
| `/fb enable` | Activates directional knockback (no restart required). | `fbaddon.admin` |
| `/fb disable` | Temporarily pauses the custom knockback system. | `fbaddon.admin` |
| `/fb status` | Reports whether the suite is currently active. | `fbaddon.admin` |
| `/fb help` | Displays the administrative quick reference. | `fbaddon.admin` |

The command also responds to `/fbaddon` and `/fbaddons` aliases.

## Configuration Overview
The configuration file is heavily commented and reload-friendly. Key areas:

- **`fireball.throw`** – Enable/disable manual throws, control projectile speed, cooldown, and optional sound feedback.
- **`fireball.self`** – Governs knockback when players strike themselves (straight jumps, backward launches, stationary drops). Pitch, distance, and movement thresholds decide when horizontal is blended out.
- **`fireball.side`** – Handles semi-side and side hits with separate pitch bands and forward-facing tolerances.
- **`fireball.min-horizontal`** – Prevents tiny nudges from feeling unresponsive by enforcing a minimum horizontal magnitude.

Every value is documented in-line so customers can tune the suite without guesswork.

## Development
```bash
git clone https://github.com/hypnr/BedWars1058-DirectionalFBAddon.git
cd BedWars1058-DirectionalFBAddon
mvn clean package
```
The shaded artifact is published to `target/DirectionalFBAddon-1.4.2-shaded.jar`.

## Support & Credits
- Plugin author: **Hypnr**
- Core game: BedWars1058 by Andrei1058
- Tested against Spigot/Paper 1.8.8 with BedWars1058 API 25.2

Commercial licensing, bundle requests, or private support can be arranged directly with Hypnr.

## License
Released under the **MIT License**. See `LICENSE` for full terms.
