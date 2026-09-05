# HologramViewFix

HologramViewFix is a Paper plugin that fixes hologram visibility, tracking,
and flicker issues across the most common hologram systems used on
Minecraft servers — regardless of which plugin created the hologram.

It targets the underlying cause: Minecraft's client-side entity tracking
can silently drop holograms when a player moves far away, changes chunks,
teleports, respawns, or reconnects, causing holograms to appear invisible,
gray, blank, delayed, or flickering. HologramViewFix detects these gaps and
restores visibility safely, without duplicating entities, without spamming
packets, and without interfering with normal server entities.

## Supported Systems

- Vanilla ArmorStand-based holograms (invisible + marker + no-gravity + custom name)
- TextDisplay / ItemDisplay / BlockDisplay entities
- Citizens NPCs and NPC-attached holograms
- DecentHolograms
- FancyHolograms
- CMI holograms
- HolographicDisplays
- Packet-based hologram systems (via an optional ProtocolLib hook)

HologramViewFix never forces every entity to be visible — it uses a
multi-layer detection system to identify entities that are genuinely
holograms, and leaves normal players, mobs, animals, items, decorative
ArmorStands, and other plugins' entities completely untouched.

## Requirements

- **Paper** 1.21 or newer (built against the 1.21.1 API; targets Paper's
  `api-version: '1.21'` for forward compatibility with newer 1.21.x releases)
- **Java 21**

All of the following are optional soft-dependencies — the plugin runs
fine without any of them installed, simply skipping that integration:

- Citizens
- DecentHolograms
- FancyHolograms
- CMI
- HolographicDisplays
- ProtocolLib (enables packet-only hologram support)

## Installation

1. Download `HologramViewFix-1.0.0.jar` from the Releases page (or from a
   GitHub Actions build artifact).
2. Place it in your server's `plugins/` folder.
3. Restart or reload your server.
4. A default `config.yml` will be generated in `plugins/HologramViewFix/`.

## Configuration

```yaml
enabled: true

view-distance:
  enabled: true
  distance: 128

minimum-distance: 0
maximum-distance: 128

check-interval: 5       # ticks between safety-net visibility sweeps

chunk-check: true       # refresh holograms when a player crosses a chunk boundary

fix-flicker: true
fix-missing-holograms: true
fix-display-entities: true
fix-armorstand-holograms: true
fix-citizens: true
fix-packet-holograms: true

integrations:
  citizens: true
  decent-holograms: true
  fancy-holograms: true
  cmi: true
  holographic-displays: true
  protocol-lib: true

performance:
  max-checks-per-cycle: 100   # caps work done per sweep to protect TPS

debug: false
```

## Commands

| Command | Description |
|---|---|
| `/hologramviewfix reload` | Reloads the configuration without a restart |
| `/hologramviewfix status` | Shows plugin status and hooked integrations |
| `/hologramviewfix scan` | Manually scans nearby chunks for holograms |
| `/hologramviewfix debug` | Toggles debug logging |

Alias: `/hvf`

## Permissions

| Permission | Default | Description |
|---|---|---|
| `hologramviewfix.admin` | op | Grants all sub-permissions |
| `hologramviewfix.reload` | op | Allows `/hologramviewfix reload` |
| `hologramviewfix.status` | op | Allows `/hologramviewfix status` |
| `hologramviewfix.scan` | op | Allows `/hologramviewfix scan` |
| `hologramviewfix.debug` | op | Allows `/hologramviewfix debug` |

## How Detection Works

HologramViewFix uses a layered detection pipeline so it only ever targets
entities that are confidently holograms:

1. Hologram plugin API / registered integration match
2. Plugin-specific namespaced identifiers (PersistentDataContainer)
3. Generic PDC markers
4. Legacy Bukkit entity metadata
5. Citizens API
6. Display entity characteristics
7. ArmorStand characteristics
8. Combination heuristic (invisible + marker + no-gravity + visible custom name)
9. Safe fallback — if nothing matches confidently, it is **not** treated as a hologram

A bare, unmarked ArmorStand is never automatically assumed to be a
hologram — at least three of the four classic signals must be present
alongside a visible custom name.

## Building from Source

This project uses the Gradle Wrapper, so no local Gradle installation is
required.

```bash
./gradlew build
```

On Windows:

```bat
gradlew.bat build
```

The compiled plugin JAR will be produced at:

```
build/libs/HologramViewFix-1.0.0.jar
```

## GitHub Actions

Every push and pull request triggers `.github/workflows/build.yml`, which:

1. Checks out the repository
2. Sets up Java 21
3. Generates the Gradle Wrapper automatically if it isn't already present
   (so the repository never needs a manually-uploaded wrapper binary)
4. Runs `./gradlew build`
5. Verifies a JAR was produced
6. Uploads the JAR as a workflow artifact named `HologramViewFix`

Pushing a version tag (e.g. `v1.0.0`) triggers
`.github/workflows/release.yml`, which builds the plugin and publishes the
resulting JAR as a GitHub Release asset automatically.

## Development Notes

- DecentHolograms, FancyHolograms, CMI, and HolographicDisplays do not
  publish stable public Maven artifacts, so their integrations are
  implemented defensively at runtime (metadata/PDC/class-name detection
  plus safe visibility refresh calls) rather than as compile-time
  dependencies. This keeps the build 100% reproducible on GitHub Actions
  without requiring any locally-uploaded jar files.
- Citizens and ProtocolLib integrations compile against their real public
  APIs as `compileOnly` soft-dependencies.
- The plugin never force-spawns, force-despawns, or duplicates entities,
  never touches Citizens NPC state (location, skin, name, traits,
  pathfinding, equipment), and performs no per-tick full-world entity
  scans — visibility checks are batched and capped via
  `performance.max-checks-per-cycle`.

## License

You are free to use, modify, and distribute this plugin for your own
server(s).
