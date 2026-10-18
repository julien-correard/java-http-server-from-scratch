# Serveur HTTP artisanal en Java

Une petite API REST de gestion de clients, écrite **sans framework** : uniquement les sockets de Java, un parseur HTTP et un parseur JSON faits à la main, puis une persistance PostgreSQL en JDBC pur.

## Pourquoi ce projet

Je travaille avec Spring Boot et JPA, et je voulais comprendre ce que le framework fait réellement à ma place : lecture d'une requête HTTP brute, routage, JSON, accès à la base de données. Ce dépôt reprend ces mécanismes un par un, à la main.

Cette expérience me permet aussi de comprendre de l'intérieur les échanges HTTP et les erreurs qui y sont liées, ce qui est utile pour ma reconversion professionnelle orientée infrastructure.

Ce n'est **pas** un serveur destiné à la production (voir [Limites connues](#limites-connues)).

## Ce que fait le projet

- Un serveur TCP sur le port **8080** qui lit et interprète les requêtes HTTP
- Un CRUD complet sur la ressource `/clients`
- Deux implémentations interchangeables du stockage, derrière une interface `ClientRepository` :
  - `InMemoryClientRepository` : données en mémoire (perdues à l'arrêt)
  - `JdbcClientRepository` : données dans PostgreSQL, via JDBC
- Un choix du stockage au lancement, par variables d'environnement

## Structure

| Fichier | Rôle |
|---|---|
| `Main.java` | Boucle d'acceptation des connexions, routage, choix du repository |
| `HttpRequest.java` | Lecture de la ligne de requête, des en-têtes et du corps |
| `HttpResponse.java` | Écriture de la réponse HTTP (statut, en-têtes, corps) |
| `JsonParser.java` | Lecture d'un objet JSON simple (voir limites) |
| `Client.java` | Le modèle : `id`, `name`, `phone` |
| `ClientRepository.java` | L'interface d'accès aux données |
| `InMemoryClientRepository.java` | Implémentation en mémoire |
| `JdbcClientRepository.java` | Implémentation PostgreSQL (JDBC, `PreparedStatement`) |
| `BadRequestException.java` | Erreur de requête invalide (réponse 400) |
| `JsonParserTest.java` | Tests unitaires du parseur JSON (dans `src/test/java`) |
| `HttpRequestTest.java` | Tests unitaires de la lecture des requêtes HTTP |
| `InMemoryClientRepositoryTest.java` | Tests unitaires du repository en mémoire |

## Prérequis

- Java 17
- Maven
- Docker (uniquement pour le mode PostgreSQL)

## Lancer le projet

### Mode mémoire (le plus simple)

Lancer la classe `Main` depuis l'IDE (IntelliJ, VS Code). Sans variable d'environnement, le serveur utilise le stockage en mémoire et l'indique au démarrage.

### Mode PostgreSQL

1. Démarrer PostgreSQL avec Docker :

```bash
docker run --name pg-clients \
  -e POSTGRES_PASSWORD=secret \
  -e POSTGRES_DB=clients_db \
  -p 5433:5432 -d postgres:16
```

Le port 5433 de la machine est redirigé vers le 5432 du conteneur, pour éviter un conflit avec un PostgreSQL déjà installé en local.

2. Créer la table :

```bash
docker exec -it pg-clients psql -U postgres -d clients_db
```

```sql
CREATE TABLE clients (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name TEXT NOT NULL,
    phone TEXT NOT NULL
);
```

3. Définir les variables d'environnement, puis lancer `Main` :

| Variable | Rôle | Exemple |
|---|---|---|
| `DB_URL` | URL JDBC. Si elle est absente, le stockage se fait en mémoire | `jdbc:postgresql://localhost:5433/clients_db` |
| `DB_USER` | Utilisateur PostgreSQL | `postgres` |
| `DB_PASSWORD` | Mot de passe | `secret` |

Si `DB_URL` est définie sans `DB_USER` et `DB_PASSWORD`, le serveur refuse de démarrer avec un message explicite.

## Tests

Les tests unitaires utilisent JUnit 5 et se lancent avec :

```bash
mvn test
```

Ils couvrent :

- `JsonParser` : objet simple, objet vide, guillemet échappé, JSON incomplet, valeur qui n'est pas une chaîne
- `HttpRequest` : lecture d'une requête `POST` avec corps (à partir d'un texte en mémoire, sans réseau), requête vide, `Content-Length` négatif
- `InMemoryClientRepository` : création et lecture, modification partielle, suppression d'un identifiant inexistant

**Ces tests n'ont pas été écrits par moi** : ils ont été rédigés avec l'aide de Claude (Anthropic), à ma demande, pour apprendre à tester. Je m'en sers comme support pour comprendre la démarche (préparer, agir, vérifier) et je les fais évoluer au fil de mon apprentissage.

Le repository PostgreSQL et le routage de `Main` ne sont pas encore couverts : ils demandent une vraie base de données et une extraction du routage hors de la boucle réseau.

## L'API

| Méthode | Chemin | Rôle | Réponses |
|---|---|---|---|
| `POST` | `/clients` | Créer un client | 201, 400 |
| `GET` | `/clients` | Lister les clients | 200 |
| `GET` | `/clients/{id}` | Lire un client | 200, 400, 404 |
| `PATCH` | `/clients/{id}` | Modifier un client (champs optionnels) | 200, 400, 404 |
| `DELETE` | `/clients/{id}` | Supprimer un client | 204, 400, 404 |

Les erreurs de format ou de protocole renvoient 400, un dépassement du délai de lecture (10 s) renvoie 408, une erreur interne renvoie 500.

### Exemples avec `curl`

```bash
# Créer un client
curl -i -X POST localhost:8080/clients \
  -d '{"name":"Alice","phone":"0612345678"}'

# Lister les clients
curl -i localhost:8080/clients

# Lire le client 1
curl -i localhost:8080/clients/1

# Modifier uniquement le téléphone du client 1
curl -i -X PATCH localhost:8080/clients/1 \
  -d '{"phone":"0700000000"}'

# Supprimer le client 1
curl -i -X DELETE localhost:8080/clients/1
```

Un client est représenté ainsi :

```json
{"id":1,"name":"Alice","phone":"0612345678"}
```

## Ce que ce projet m'a appris

- Le format d'une requête HTTP brute : ligne de requête, en-têtes, ligne vide, corps, et le rôle de `Content-Length` (compté en **octets**, pas en caractères)
- Se protéger des entrées : limite de taille du corps, validation des en-têtes, timeout de socket
- Produire du JSON valide : l'échappement des caractères spéciaux, et pourquoi la concaténation de chaînes est dangereuse
- Séparer les responsabilités et dépendre d'une interface, pour changer de stockage sans toucher au reste du code
- JDBC : `Connection`, `PreparedStatement`, `ResultSet`, `try-with-resources`, requêtes paramétrées contre l'injection SQL, `RETURNING` de PostgreSQL
- Les bases des tests unitaires avec JUnit 5 : structure préparer / agir / vérifier, `assertEquals`, `assertThrows`, tester les cas d'erreur autant que les cas normaux (à partir de tests écrits avec l'aide d'une IA, voir [Tests](#tests))

## Limites connues

Ces choix sont volontaires : ils gardent le code lisible et centré sur la compréhension.

- **Serveur mono-thread** : une requête à la fois, un client lent bloque les autres (jusqu'au timeout de 10 s)
- **Une connexion PostgreSQL par requête**, sans pool de connexions
- **Parseur JSON minimal** : uniquement des objets à valeurs de type chaîne, et seuls les échappements `\"` et `\\` sont gérés (pas de nombres, tableaux, `\n` ni `\uXXXX`)
- **Lecture du corps** : le décompte des octets ne gère pas correctement les caractères hors plan de base (par exemple les emojis)
- **Pas de query string** : `GET /clients/1?x=2` est rejeté
- **Une requête par connexion** : pas de keep-alive, pas de HTTPS
- **Pas de transactions** : chaque opération est une requête SQL unique, donc déjà atomique
- **Couverture de tests partielle** : pas de test du `JdbcClientRepository` ni de `Main`, ces vérifications se font à la main avec `curl`

Le `JsonParser` a été écrit avec l'aide d'un outil d'IA (Opencode), puis relu et adapté.

## Pistes pour la suite

- Écrire moi-même de nouveaux tests, puis un test d'intégration du `JdbcClientRepository` (avec Testcontainers) et un script de test de bout en bout avec `curl`
- Pool de connexions (HikariCP) et traitement des requêtes en parallèle
- Une seconde table liée à `clients` (clé étrangère, jointures, transactions)
- Refaire la même API avec Spring Boot et JPA, pour comparer ligne à ligne avec cette version artisanale
