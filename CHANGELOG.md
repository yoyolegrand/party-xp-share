# Changelog

## 1.2.0 — Party share

- **Nearby teammates now get a share** (FTB Teams, new option `[teams] includeNearbyTeammates`, on by default):
  when a mob dies, the killer's teammates (or the top damage dealer's) within the share radius are added to the split, even if they did not hit it.
  - `EQUAL`: they get a full equal share.
  - `DAMAGE` / `HYBRID`: they split `teammateSharePercent` (25% by default).
  - A kill with a teammate nearby is no longer a solo kill.
  - In Wave Survivor hordes, this share replaces the support share for teammates.
- The minimum contribution only applies to players who hit the mob: a teammate who hit a little (or not at all) is never left out.
- New commands: `/partyxp set teammates true|false`, `/partyxp set teammateShare <0-100>`.
- `/partyxp` status is clearer: minimum contribution "of damage", a "teammates nearby share" line, and "0% = off" for the group bonus.

## 1.1.0 — Teams and summons

- **FTB Teams compatibility** (optional): new `[teams]` config section, `TEAM_ONLY` by default.
  - Only players in the same team as the killer share the XP; other players are ignored (their damage does not count).
  - A player without a team shares with nobody: the vanilla drop is kept.
  - If the killer is not a player (fire, arrow, pet…), the player who dealt the most damage is the team reference.
  - Orb sharing and the Wave Survivor support share follow the same rule.
  - `/partyxp set teamMode off|team_only`, `/partyxp set teams true|false`; `/partyxp` shows whether FTB Teams is detected.
- **Summons from other mods** now count for their caster: Iron's Spells summons (and any mob with a public `getSummoner()` or `getOwner()` returning a player) are credited.
  New option `killSharing.summonOwnerLookup` (on by default) to turn this off.
- Documentation: only vanilla experience is shared; skill XP from mods like Project MMO is not affected.

## 1.0.0 — First release

- **Kill sharing**: a mob's XP is split between every player who hurt it.
  - Three modes: `DAMAGE` (by share of damage), `EQUAL` (per head), `HYBRID` (killer bonus + the rest by damage).
  - Minimum contribution (5% by default), share radius, contribution timeout.
  - Optional group bonus, capped.
  - Pets and owned summons count for their owner; fire, poison and wither count for the last player who hit the mob.
  - Delivery as XP orbs on each player or as direct XP; Mending works in both cases.
  - Random rounding, so small mobs still give the right XP on average.
  - Mob blacklist / whitelist (entity ids or `#tags`).
  - Solo kills are untouched.
- **Orb sharing** (off by default): picked-up XP orbs from any source are split with nearby players (`SPLIT`) or give them a bonus (`BONUS`).
- **Configuration**: fully commented `config/partyxpshare-common.toml`, reloaded on save, plus the `/partyxp` command for operators.
- **Wave Survivor compatibility** (optional): horde mobs, bosses and Kingdom units get their own mode, radius, and a support share for nearby players who did not hit the mob.
- Server-side only: players without the mod can join.
