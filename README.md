# Party XP Share

Forge 1.20.1 · server-side only (players without the mod can join) · by yoyolegrand

Share mob XP with everyone who helped kill it.

## Modes
| Mode | What happens (a mob worth 100 XP) |
|---|---|
| `DAMAGE` | Split by damage dealt: 20% of the damage → 20 XP |
| `EQUAL` | Same share for every participant: 4 players → 25 XP each |
| `HYBRID` | The killer gets `killerBonusPercent` (30% by default), the rest is split by damage |

- **Minimum contribution** (5% by default): one hit and wait gets nothing.
- **Share radius** and **contribution timeout**: you must stay in the fight.
- **Group bonus** (off by default): extra XP per additional participant.
- **Pets / summons** count for their owner; fire, poison, etc. count for the last player who hit the mob.
- **Delivery**: XP orbs on each player, or direct XP. Mending works in both cases.
- **Random rounding**: a 5 XP zombie split between 4 players still gives 5 XP on average.
- **Orb sharing** (off by default): picked-up orbs from any source are split with nearby players (`SPLIT`) or give them a bonus (`BONUS`).
- Mob **blacklist / whitelist** (ids or `#tags`).
- Solo kills are untouched (vanilla drop).

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
Options: `enabled language messages killSharing mode killerBonus minContribution radius timeout groupBonus groupBonusMax pets indirectDamage delivery orbSharing orbMode orbBonus orbRadius ws wsMode wsRadius wsMinContribution wsSupport wsGroupBonus wsHideMessages`

## Wave Survivor compatibility
Detected automatically, no hard dependency. Horde mobs, bosses and Kingdom units get their own settings:
- `mode`: own mode, or `GLOBAL` to follow the main one
- `shareRadius` (64 by default: arenas are big)
- `supportSharePercent` (10% by default): part of each horde mob's XP goes to nearby players who did not hit it (builders, healers…)
- own minimum contribution and group bonus
- action bar messages hidden during hordes (Wave Survivor uses the action bar)

XP bonuses from Wave Survivor (Shrine gear) are applied before the split.

## Build
`./gradlew build` → `build/libs/partyxpshare-1.20.1-<version>.jar`
