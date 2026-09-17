# TourGuide — Découpage en microservices

> Branche `feature/microservices-split` — Projet de certification DWWM (P8), application **TourGuide** pour l'entreprise fictive **TripMaster**.

## 🎯 Contexte

TourGuide est une application Spring Boot permettant aux utilisateurs de découvrir des attractions touristiques à proximité, d'obtenir des récompenses, et de recevoir des offres de voyage personnalisées. Ce projet reprend une base existante souffrant de problèmes de performance critiques (impossible de traiter 100 000 utilisateurs dans les délais impartis) et la fait évoluer en deux temps :

1. **Stabilisation du monolithe** (branche `feature/perf-100k-multithreading`) — correction des bugs, optimisation des performances via le multithreading.
2. **Découpage en microservices** (cette branche) — extraction des trois librairies externes en services indépendants, communiquant en HTTP.

## 🏗️ Architecture

Le projet est composé de **4 microservices totalement indépendants**, chacun étant son propre projet Gradle autonome (son propre Gradle Wrapper, son propre `settings.gradle`, ses propres dépendances) :

```
.
├── TourGuide-ms/            → Application principale (orchestrateur), port 8080
├── gpsutil-service/         → Service de géolocalisation, port 8081
├── rewardscentral-service/  → Service de calcul des récompenses, port 8082
└── trippricer-service/      → Service de tarification des offres, port 8083
```

Chaque service peut être cloné, construit et exécuté **seul**, sans dépendre d'aucun autre dossier du repo — c'est le principe fondamental d'une architecture microservices.

### Qui appelle qui

```
                    ┌─────────────────┐
     Client HTTP →  │   TourGuide-ms   │  (port 8080)
                    │  (orchestrateur)  │
                    └────────┬─────────┘
                             │ WebClient (HTTP)
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
     ┌────────────────┐ ┌──────────────────┐ ┌───────────────────┐
     │ gpsutil-service │ │rewardscentral-svc│ │ trippricer-service│
     │   (port 8081)   │ │   (port 8082)    │ │    (port 8083)    │
     └────────────────┘ └──────────────────┘ └───────────────────┘
```

`TourGuide-ms` est le seul point d'entrée public. Il communique avec les 3 microservices via des classes `WebClient` dédiées (`GpsUtilWebClient`, `RewardsWebClient`, `TripPricerWebClient`), qui remplacent les appels Java directs du monolithe d'origine.

## 🛠️ Stack technique

- **Java 8**
- **Spring Boot** 2.1.6.RELEASE
- **Gradle** 4.8.1 (avec Gradle Wrapper — pas besoin de Gradle installé sur la machine)
- **Spring WebFlux** (`WebClient`) pour la communication inter-services
- **jsoniter** pour la sérialisation/désérialisation JSON
- **JUnit 4** + **JaCoCo** pour les tests et la couverture de code

## 🚀 Démarrer le projet en local

Chaque service se lance indépendamment. **Il faut démarrer les 3 microservices avant `TourGuide-ms`**, sans quoi les appels réseau échoueront.

```bash
# Terminal 1
cd gpsutil-service
./gradlew bootRun

# Terminal 2
cd rewardscentral-service
./gradlew bootRun

# Terminal 3
cd trippricer-service
./gradlew bootRun

# Terminal 4 — une fois les 3 précédents démarrés
cd TourGuide-ms
./gradlew bootRun
```

> ⚠️ Le projet cible **Java 8**. Si votre `JAVA_HOME` par défaut pointe vers une version plus récente (Java 17+), Gradle 4.8.1 refusera de démarrer. Réglez temporairement `JAVA_HOME` vers un JDK 8 avant de lancer les commandes.

## 📡 Principaux endpoints (`TourGuide-ms`, port 8080)

| Méthode | Route | Description |
|---|---|---|
| `GET` | `/getLocation?userName=...` | Position actuelle d'un utilisateur |
| `GET` | `/getNearbyAttractions?userName=...` | 5 attractions les plus proches, avec distance et points de récompense |
| `GET` | `/getRewards?userName=...` | Récompenses obtenues par un utilisateur |
| `GET` | `/getAllCurrentLocations` | Position actuelle de tous les utilisateurs |
| `GET` | `/getAllUsers` | Liste de tous les utilisateurs (id + nom) |
| `GET` | `/getTripDeals?userName=...` | Offres de voyage personnalisées |
| `PUT` | `/updatePreferences?userName=...&...` | Mise à jour des préférences de voyage d'un utilisateur |

## ✅ Bugs corrigés et fonctionnalités ajoutées

- **`addUserReward`** : une comparaison de types incorrecte empêchait tout utilisateur de recevoir plus d'une récompense. Corrigé, et les listes concernées passées en `CopyOnWriteArrayList` pour la sécurité en environnement concurrent.
- **Préférences de voyage ignorées** : aucun mécanisme ne permettait de définir les préférences réelles d'un utilisateur avant le calcul des offres. Ajout de l'endpoint `PUT /updatePreferences`.
- **5 attractions les plus proches** : l'ancienne implémentation filtrait par un rayon fixe (risque de zéro résultat). Réécrite pour toujours renvoyer les 5 plus proches, avec distance et points de récompense.
- **`getAllCurrentLocations`** et **`getAllUsers`** : nouvelles fonctionnalités, absentes du projet d'origine.
- **Tests instables (flaky)** : résolus comme effet de bord du passage en `CopyOnWriteArrayList`.

## 📊 Performance

| Test | Objectif | Résultat |
|---|---|---|
| `highVolumeTrackLocation` (100 000 utilisateurs) | ≤ 15 min | **22 secondes** |
| `highVolumeGetRewards` (100 000 utilisateurs) | ≤ 20 min | **45 secondes** |

Optimisations clés : parallélisation via `ExecutorService`/`CompletableFuture` (pool de 150 threads, dimensionné pour des tâches I/O-bound), mise en cache de la liste des attractions côté `RewardsService` (évite un appel réseau redondant par utilisateur).

> ℹ️ Ces mesures sont effectuées avec les 3 microservices tournant en local (`localhost`) — elles ne reflètent donc pas la latence réseau d'un déploiement distribué réel.

## 🚧 En cours

- [ ] Dockerisation de chaque service (`Dockerfile` multi-stage)
- [ ] `docker-compose.yml` pour orchestrer les 4 services ensemble
- [ ] Nettoyage `.gitignore` (retrait de `.gradle/` et `build/` du suivi Git)

## 🌿 Branches du projet

| Branche | Contenu |
|---|---|
| `main` | Version originale du projet, non modifiée |
| `feature/perf-100k-multithreading` | Monolithe stabilisé et optimisé (bugs + performance) |
| `feature/microservices-split` | *(cette branche)* Découpage en 4 microservices indépendants |
