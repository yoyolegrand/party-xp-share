# Party XP Share

Forge 1.20.1 · server-side only (players without the mod can join) · by yoyolegrand

Share mob XP with everyone who helped kill it.

## Modes
| Mode | What happens (a mob worth 100 XP) |
|---|---|
| `DAMAGE` | Split by damage dealt: 20% of the damage → 20 XP |
| `EQUAL` | Same share for every participant: 4 players → 25 XP each |
| `HYBRID` | The killer gets `killerBonusPercent` (30% by default), the rest is split by damage |

- **Minimum contribution** (5% by default): one hit and wait gets nothing. It only applies to players who hit the mob (see FTB Teams for nearby teammates).
- **Share radius** and **contribution timeout**: you must stay in the fight.
- **Group bonus** (off by default): extra XP per additional participant.
- **Pets / summons** count for their owner, including summons from other mods (Iron's Spells…); fire, poison, etc. count for the last player who hit the mob.
- **Delivery**: XP orbs on each player, or direct XP. Mending works in both cases.
- **Random rounding**: a 5 XP zombie split between 4 players still gives 5 XP on average.
- **Orb sharing** (off by default): picked-up orbs from any source are split with nearby players (`SPLIT`) or give them a bonus (`BONUS`).
- Mob **blacklist / whitelist** (ids or `#tags`).
- Solo kills are untouched (vanilla drop).
- Only **vanilla experience** is shared. Skill XP from mods like Project MMO is not affected.

Damage tracking is one map lookup per hit, nothing runs every tick.

## Configuration
`config/partyxpshare-common.toml` — every option is commented and the file reloads when saved.

In game (operators):
```
/partyxp                                  status
/partyxp mode damage|equal|hybrid
/partyxp toggle
/partyxp set <option> <value>             e.g. /partyxp set minContribution 10
/partyxp blacklist add|remove|list <id or #tag>
/partyxp whitelist add|remove|list <id or #tag>
```
Options: `enabled language messages killSharing mode killerBonus minContribution radius timeout groupBonus groupBonusMax pets summonLookup indirectDamage delivery orbSharing orbMode orbBonus orbRadius teams teamMode teammates teammateShare ws wsMode wsRadius wsMinContribution wsSupport wsGroupBonus wsHideMessages`

## FTB Teams compatibility
Detected automatically, no hard dependency. Section `[teams]` of the config:
- `mode = TEAM_ONLY` (default): only players in the **same team as the killer** share the XP. Other players who hit the mob are ignored, and a player without a team shares with nobody (vanilla drop). If the killer is not a player (fire, arrow, pet…), the player who dealt the most damage is the reference.
- `mode = OFF`: everybody who helped shares, as without FTB Teams.
- Orb sharing and the Wave Survivor support share follow the same rule.
- `includeNearbyTeammates = true` (default) — **party share**: when a mob dies, the killer's teammates within the share radius get a share even if they did not hit it. In `EQUAL` mode each of them gets a full equal share; in `DAMAGE` / `HYBRID` they split `teammateSharePercent` (25% by default). The minimum contribution does not apply to them, and a kill with a teammate nearby is never a solo kill. In Wave Survivor hordes this replaces the support share for teammates.

In game: `/partyxp set teamMode off|team_only`, `/partyxp set teams true|false`, `/partyxp set teammates true|false`, `/partyxp set teammateShare <0-100>`. `/partyxp` shows whether FTB Teams is detected.

## Wave Survivor compatibility
Detected automatically, no hard dependency. Horde mobs, bosses and Kingdom units get their own settings:
- `mode`: own mode, or `GLOBAL` to follow the main one
- `shareRadius` (64 by default: arenas are big)
- `supportSharePercent` (10% by default): part of each horde mob's XP goes to nearby players who did not hit it (builders, healers…)
- own minimum contribution and group bonus
- action bar messages hidden during hordes (Wave Survivor uses the action bar)

XP bonuses from Wave Survivor (Shrine gear) are applied before the split.

## Changelog
See [CHANGELOG.md](CHANGELOG.md).

## Build
`./gradlew build` → `build/libs/partyxpshare-1.20.1-<version>.jar`
