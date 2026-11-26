# Tests Party XP Share 1.1.0

Vérifié par Claude Code (sans jeu) : compilation javac OK, 14 tests unitaires OK (dont 4 sur le filtre d'équipe),
API FTB Teams 2001.3.2 et `getSummoner()` d'Iron's 3.16.3 trouvés par réflexion dans les vrais jars.
**À cocher en jeu par Yoann** (pack Grimoire of Arcana : FTB Teams, Iron's, PMMO déjà dedans) :

- [ ] 1. Deux joueurs **dans la même équipe FTB** tapent un zombie → XP partagée comme en 1.0 (message `+X XP (..% damage)`).
- [ ] 2. Deux joueurs **sans équipe commune** → seul le tueur garde l'XP, drop vanilla (orbes sur le mob, pas de message).
- [ ] 3. Invocations d'Iron's (vex, zombies, squelettes, ours, cheval) : leurs dégâts comptent pour le lanceur.
      Attention : si seules les invocations frappent le mob, Minecraft ne lâche aucune XP (règle vanilla : un joueur doit l'avoir touché dans les 5 dernières secondes).
- [ ] 4. Horde Wave Survivor, 2 coéquipiers + 1 joueur hors équipe : seuls les coéquipiers partagent ; part de soutien limitée aux coéquipiers proches.
- [ ] 5. `/partyxp set teamMode off` → comportement 1.0 (le joueur hors équipe partage à nouveau).
- [ ] 6. Sans FTB Teams installé → aucun crash ; `/partyxp` affiche « FTB Teams: not installed ».
- [ ] 7. Avec PMMO : l'XP de compétence est la même que Party XP Share soit actif ou non (`/partyxp toggle`).

En plus :
- [ ] Le log au démarrage affiche « FTB Teams detected: team sharing available ».
- [ ] Partage des orbes activé (`/partyxp set orbSharing true`) : un joueur hors équipe ne reçoit rien.
- [ ] `/partyxp set summonLookup false` → les invocations d'Iron's ne comptent plus.
