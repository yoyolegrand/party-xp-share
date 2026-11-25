# Party XP Share — publication 1.0.0

Fiche pour créer les pages CurseForge et Modrinth (textes publics en anglais).

## Réglages des pages

| Champ | Valeur |
|---|---|
| Nom | Party XP Share |
| Slug | `party-xp-share` |
| Résumé (≤ 100 caractères) | Share mob XP with everyone who helped: by damage, per head, or killer bonus. Server-side only. |
| Icône | `src/main/resources/partyxpshare_logo.png` (512×512) |
| Loader / version | Forge, Minecraft 1.20.1 |
| Environnement | Serveur : requis · Client : optionnel (inutile côté client) |
| Licence | MIT |
| Source | https://github.com/yoyolegrand/party-xp-share |
| Issues | https://github.com/yoyolegrand/party-xp-share/issues |
| Catégories CurseForge | Server Utility, Miscellaneous (option : Adventure and RPG) |
| Catégories Modrinth | Utility, Game Mechanics, Management |
| Dépendance optionnelle | Wave Survivor (« optional dependency » sur les deux sites) |
| Fichier | `build/libs/partyxpshare-1.20.1-1.0.0.jar` |
| Type de version | Release (ou Beta tant que le test multijoueur n'est pas fait) |
| Nom de version | `Party XP Share 1.0.0` |
| Changelog de la version | section `1.0.0` de `CHANGELOG.md` |

Modrinth demande aussi les déclarations d'usage d'IA (comme pour Wave Survivor).

## Description (CurseForge et Modrinth, Markdown)

```markdown
# Party XP Share

**Fight together, level together.** Party XP Share splits a mob's XP between every player who helped kill it, instead of giving everything to whoever landed the last hit.

Server-side only: install it on the server, players without the mod can still join.

## Three sharing modes

| Mode | A mob worth 100 XP |
|---|---|
| **DAMAGE** (default) | Split by damage dealt: 20% of the damage → 20 XP |
| **EQUAL** | Same share for every participant: 4 players → 25 XP each |
| **HYBRID** | The killer gets a bonus (30% by default), the rest is split by damage |

## Fair by design
- **Minimum contribution** (5% by default): one hit and wait gets nothing, the share goes to the others.
- **Share radius** and **contribution timeout**: you have to stay in the fight.
- **Damage is capped at the mob's remaining health**: a 500-damage hit on a 2 HP mob counts as 2.
- **Pets and owned summons** count for their owner; fire, poison and wither count for the last player who hit the mob.
- **Random rounding**: a 5 XP zombie split between 4 players still gives 5 XP on average.
- **Solo kills are untouched**: vanilla drop, nothing changes.

## Also included
- **Group bonus** (off by default): extra XP per additional participant, capped.
- **Orb sharing** (off by default): XP orbs picked up from any source (mining, smelting, fishing…) are split with nearby players, or give them a bonus.
- **Delivery**: XP orbs on each player, or direct XP. **Mending works in both cases.**
- **Mob blacklist / whitelist** with entity ids or `#tags`.
- Action bar message: `+12 XP (35% damage)`, in English or French.

## Easy to configure
Every option is commented in `config/partyxpshare-common.toml`, and the file reloads when you save it.
Operators can also change everything in game:

    /partyxp                          status
    /partyxp mode damage|equal|hybrid
    /partyxp toggle
    /partyxp set <option> <value>     e.g. /partyxp set minContribution 10
    /partyxp blacklist add|remove|list <id or #tag>
    /partyxp whitelist add|remove|list <id or #tag>

## Wave Survivor compatibility
Detected automatically (optional, no hard dependency). Horde mobs, bosses and Kingdom units get their own settings:
- their own mode and a larger share radius (64 blocks: arenas are big);
- a **support share** (10% by default) for nearby players who did not hit the mob: builders, healers, defenders;
- action bar messages hidden during hordes.

XP bonuses from Wave Survivor gear are applied before the split.

## Performance
One map lookup per hit; nothing runs every tick except a cleanup every 30 seconds.
```

## Avant de publier
- [ ] Build Gradle OK sur ton PC (`.\gradlew.bat build`, 10 tests JUnit compris)
- [ ] Test à 2 joueurs ou plus : DAMAGE / EQUAL / HYBRID, kill solo, seuil minimum, Mending
- [ ] Test dans une horde Wave Survivor 2.1 : log « horde compatibility enabled », part de soutien, messages masqués
- [ ] Dépôt GitHub créé et poussé, release `v1.0.0` avec le jar
- [ ] Pages CurseForge et Modrinth créées, jar envoyé
- [ ] Captures pour la galerie (facultatif : message `+XP` en barre d'action, fichier de config, `/partyxp`)
