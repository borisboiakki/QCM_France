# Audio de fin d'examen

L'écran de résultat joue une musique à la fin de l'examen (si le son est activé
dans les Paramètres) :

| Fichier (`app/src/main/res/raw/`) | Cas | Musique |
|---|---|---|
| `marseillaise.ogg`   | Examen **réussi** (≥ 32/40) | La Marseillaise |
| `marche_funebre.ogg` | Examen **échoué** (< 32/40) | Chopin — Marche funèbre (Sonate n°2, op. 35) |

## ⚠️ État actuel : fichiers placeholders (silencieux)

Les deux fichiers `.ogg` présents dans le dépôt sont des **placeholders silencieux**
(1 s de silence). Ils permettent au projet de compiler et à la fonctionnalité d'être
entièrement câblée, mais ne produisent aucun son.

Ils doivent être remplacés par de vrais enregistrements **du domaine public**.
Ils n'ont pas pu être téléchargés automatiquement : la politique réseau de
l'environnement de développement distant bloque l'accès aux hébergeurs audio
(Wikimedia Commons, archive.org, Musopen).

## Comment remplacer par de vrais enregistrements

### 1. Choisir une source libre de droits

Attention : la *composition* de La Marseillaise et de la Marche funèbre est dans le
domaine public, mais un **enregistrement précis** possède ses propres droits voisins.
Il faut donc un enregistrement explicitement domaine public ou sous licence libre.

- **La Marseillaise** — enregistrement de l'**US Navy Band** (œuvre du gouvernement
  américain, domaine public), sur Wikimedia Commons :
  `File:United States Navy Band - La Marseillaise.ogg`
- **Marche funèbre (Chopin)** — enregistrement domaine public / CC0 sur
  [Musopen](https://musopen.org) ou Wikimedia Commons (rechercher
  « Chopin Marche funèbre op. 35 »).

### 2. Découper un extrait ~25-30 s et convertir en `.ogg`

```bash
# Extrait de 28 s à partir de 0 s, réencodé en Ogg Vorbis (léger)
ffmpeg -i source_marseillaise.mp3 -ss 0 -t 28 -c:a libvorbis -q:a 4 marseillaise.ogg
ffmpeg -i source_marche_funebre.mp3 -ss 0 -t 28 -c:a libvorbis -q:a 4 marche_funebre.ogg
```

### 3. Remplacer les fichiers

Déposer `marseillaise.ogg` et `marche_funebre.ogg` dans
`app/src/main/res/raw/` (mêmes noms — aucun changement de code nécessaire).

### 4. Mettre à jour ce fichier

Documenter ici la source exacte, l'auteur/interprète et la licence de chaque
enregistrement retenu, pour la traçabilité.
