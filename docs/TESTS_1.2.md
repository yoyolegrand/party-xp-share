# Tests Party XP Share 1.2.0

Vérifié par Claude Code (sans jeu) : compilation javac OK, 19 tests unitaires OK (dont 5 sur le partage avec les coéquipiers proches).
**À cocher en jeu par Yoann** (2 joueurs dans la même équipe FTB) :

- [ ] 1. EQUAL, A tue un zombie d'un coup, B (coéquipier) à ~10 blocs sans frapper → XP moitié-moitié, B voit `(equal share, team)`.
- [ ] 2. DAMAGE, même chose → A ≈ 75 %, B ≈ 25 %, B voit `(team)`.
- [ ] 3. B hors du rayon (`shareRadius`, 32 blocs par défaut) → drop vanilla pour A (orbes sur le mob, pas de message).
- [ ] 4. B quitte l'équipe (`/ftbteams leave`) → B ne reçoit rien (TEAM_ONLY) ; drop vanilla si A est seul.
- [ ] 5. EQUAL, minContribution 5, B frappe une seule fois (< 5 %) → B reçoit quand même sa part égale.
- [ ] 6. `/partyxp set teammates false` → comportement 1.1 (B sans frapper ne reçoit rien).
- [ ] 7. `/partyxp` : lignes « Min contribution: 5% of damage (non-hitters: see Teams) », « Teams: teammates nearby share: on · 25% » (ou « equal share » en EQUAL), « Group bonus … (0% = off) ».
- [ ] 8. Horde Wave Survivor : un coéquipier qui ne frappe pas reçoit la part d'équipe (25 %) au lieu du soutien (10 %).
