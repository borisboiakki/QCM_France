# Audio de fin d'examen

L'écran de résultat joue une musique à la fin de l'examen (si le son est activé
dans les Paramètres) :

| Fichier (`app/src/main/res/raw/`) | Cas | Musique |
|---|---|---|
| `marseillaise.ogg`   | Examen **réussi** (≥ 32/40) | La Marseillaise |
| `marche_funebre.ogg` | Examen **échoué** (< 32/40) | Chopin — Marche funèbre (Sonate n°2, op. 35, 3ᵉ mvt) |

## Sources et licences

Les compositions (La Marseillaise, la Marche funèbre de Chopin) sont dans le
**domaine public**. Les enregistrements utilisés proviennent de **Wikimedia
Commons** et sont eux aussi libres de droits (domaine public / licence libre) :

- `marseillaise.ogg` — extrait de « La Marseillaise » (Wikimedia Commons).
- `marche_funebre.ogg` — extrait de « Chopin – Piano Sonata No. 2 in B-flat minor,
  Op. 35, III. Marche funèbre » (Wikimedia Commons).

> Vérifier / compléter ici l'attribution exacte (interprète, page source, licence)
> telle qu'indiquée sur la page Wikimedia Commons de chaque enregistrement.

## Traitement appliqué

Chaque fichier a été réduit à un extrait court joué en fin d'examen :

```bash
# Extrait de 28 s depuis le début, fondu de sortie sur les 2 dernières secondes,
# réencodé en Ogg Vorbis (léger)
ffmpeg -i source.ogg -ss 0 -t 28 -af "afade=t=out:st=26:d=2" -c:a libvorbis -q:a 4 sortie.ogg
```

- Format : Ogg Vorbis, stéréo, ~28 s, ~300 Ko par fichier.
- Lecture gérée dans `ResultScreen.kt` via `MediaPlayer` (libéré par `DisposableEffect`).

## Remplacer un enregistrement

Déposer le nouveau fichier dans `app/src/main/res/raw/` sous le même nom
(`marseillaise.ogg` ou `marche_funebre.ogg`) — aucun changement de code nécessaire.
Penser à mettre à jour l'attribution ci-dessus.
