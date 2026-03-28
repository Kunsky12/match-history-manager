# Match-History-Manager

A Spring Boot microservice for **com.mekheainteractive** that stores per-player match history and serves a live global leaderboard for **Kun Khmer Mobile**. Match records are persisted in PostgreSQL (capped at 20 per player), and the leaderboard is fetched from PlayFab every hour and cached in Redis for fast reads.

---

## How It Works

```
Game Client
    │
    ├─► POST /api/matches   { match result payload }
    │   Authorization: Bearer <JWT>
    │       │
    │       ├─► Validate JWT → extract playFabId
    │       ├─► Save match record to PostgreSQL
    │       └─► Trim excess → keep only last 20 matches per player
    │
    ├─► GET /api/matches
    │   Authorization: Bearer <JWT>
    │       │
    │       ├─► Validate JWT → extract playFabId
    │       └─► Return all match records for that player
    │
    └─► GET /api/leaderboard  (public)
            │
            ├─► Check Redis  (key: "leaderboard:global", TTL: 1h)
            │       └─ Cache hit  → return immediately
            └─► Cache miss → query PostgreSQL → rebuild Redis cache


Background (every 1 hour + on startup)
    │
    ├─► POST PlayFab /Server/GetLeaderboard  (StatisticName: "LP", top 20)
    ├─► Parse entries (PlayFabId, DisplayName, LP, Position, CountryCode)
    ├─► DELETE all rows + INSERT fresh rows → PostgreSQL (Top_20_Leaderboard)
    └─► SETEX leaderboard:global 3600 → Redis
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 4.0.2 |
| Language | Java 17 |
| HTTP Client | Spring `RestClient` |
| Auth | JJWT 0.13.0 (HS256) |
| ORM | Spring Data JPA + Hibernate |
| Database | PostgreSQL |
| Cache | Redis (Jedis 7.3.0) |
| Utilities | Lombok, Jackson |

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL instance
- Redis instance
- PlayFab title with Server API access

### Installation

```bash
git clone https://github.com/your-org/match-history-manager.git
cd match-history-manager
mvn install
```

### Environment Variables

| Variable | Description |
|---|---|
| `PLAYFAB_TITLE_ID` | PlayFab title ID |
| `PLAYFAB_SECRET_KEY` | PlayFab server secret key |
| `JWT_SECRET_KEY` | Shared JWT signing secret (must match `authenticator-service`) |
| `DB_URL` | PostgreSQL JDBC URL |
| `DB_USER` | PostgreSQL username |
| `DB_PASSWORD` | PostgreSQL password |
| `REDIS_HOST` | Redis host (default: `redis`) |
| `REDIS_PORT` | Redis port (default: `6379`) |

### Running

```bash
mvn spring-boot:run
```

Or build and run the JAR:

```bash
mvn package
java -jar target/match-history-manager-0.0.1-SNAPSHOT.jar
```

The service starts on **port 8080**.

---

## API Reference

All endpoints are under `/api`. Match endpoints require a valid JWT issued by `authenticator-service`.

---

### `POST /api/matches`

Save a match result for the authenticated player.

**Headers:**
```
Authorization: Bearer <JWT>
```

**Request body — `MatchHistory_Entry`:**
```json
{
  "selectedFighter": 3,
  "matchSummary":    "Player won by KO in round 2",
  "leaguePoints":    25,
  "exp":             120,
  "result":          "WIN",
  "matchType":       "Ranked",
  "currentRound":    "2",
  "roundTimer":      "1:42",
  "gameMode":        "VersusMen_Online",
  "date":            "2025-01-01",
  "time":            "14:30:00"
}
```

> `playFabId` is injected server-side from the JWT — it is never trusted from the request body.

**Response:** The saved `MatchHistory_Entry` object including generated `id` and `createdAt`.

**Notes:**
- After saving, the service automatically trims the player's history to the **20 most recent matches** (oldest are deleted).

---

### `GET /api/matches`

Retrieve all match records for the authenticated player.

**Headers:**
```
Authorization: Bearer <JWT>
```

**Response:**
```json
[
  {
    "id": 1,
    "playfabId": "ABC123",
    "result": "WIN",
    "leaguePoints": 25,
    "gameMode": "VersusMen_Online",
    "createdAt": "2025-01-01T14:30:00"
  }
]
```

---

### `GET /api/leaderboard`

Returns the top 20 global leaderboard ranked by League Points (`LP`).

**Access:** Public — no authentication required.

**Response:**
```json
[
  {
    "id": 1,
    "playfabId": "ABC123",
    "displayName": "Hero123",
    "leaguePoints": 2400,
    "position": 0,
    "facebookId": "https://...",
    "countryCode": "KH"
  }
]
```

---

## Database Schema

### `Match_Records`

| Column | Type | Description |
|---|---|---|
| `id` | BIGINT | Auto-increment PK |
| `playfab_Id` | VARCHAR | Player's PlayFab ID |
| `selectedFighter` | INT | Fighter index chosen |
| `matchSummary` | VARCHAR | Text summary of the match |
| `leaguePoints` | INT | LP delta for this match |
| `exp` | INT | EXP earned |
| `result` | VARCHAR | `WIN`, `LOSS`, etc. |
| `matchType` | VARCHAR | `Ranked`, `Casual`, etc. |
| `currentRound` | VARCHAR | Round the match ended on |
| `roundTimer` | VARCHAR | Time remaining in round |
| `gameMode` | VARCHAR | Game mode identifier |
| `date` | VARCHAR | Match date string |
| `time` | VARCHAR | Match time string |
| `timestamp` | TIMESTAMP | Client-supplied timestamp |
| `created_at` | TIMESTAMP | Server-assigned, immutable |

> **Retention policy:** Only the 20 most recent records per player are kept. On each `POST /api/matches`, older records beyond the 20th are deleted via a native SQL trim query.

### `Top_20_Leaderboard`

| Column | Type | Description |
|---|---|---|
| `id` | BIGINT | Auto-increment PK |
| `playfab_Id` | VARCHAR | Player's PlayFab ID |
| `display_name` | VARCHAR | In-game display name |
| `leaguePoints` | INT | Total LP score |
| `position` | INT | Rank position (0-indexed) |
| `facebookId` | VARCHAR | Avatar URL from PlayFab profile |
| `countryCode` | VARCHAR | Player's country code |

> This table is **fully replaced** on every hourly sync — all rows are deleted and re-inserted from PlayFab.

---

## Leaderboard Cache Strategy

| Layer | Key | TTL | Source |
|---|---|---|---|
| Redis (primary) | `leaderboard:global` | 1 hour | PlayFab API |
| PostgreSQL (fallback) | `Top_20_Leaderboard` | Persistent | Copied from Redis on miss |

On startup (`ApplicationReadyEvent`) and every hour (`@Scheduled(fixedRate = 3600000)`), the service fetches the top 20 players by `LP` from PlayFab, rebuilds the PostgreSQL table, and refreshes the Redis cache.

If Redis is empty on a read, the service falls back to PostgreSQL and rebuilds the cache automatically.

---

## Project Structure

```
src/main/java/com/mekheainteracive/match_history_manager/
├── Config/
│   └── RedisConfig.java                  # UnifiedJedis bean (JedisPooled)
├── Controller/
│   └── PlayerController.java             # POST /api/matches, GET /api/matches, GET /api/leaderboard
├── Entity/
│   ├── MatchHistory_Entry.java           # Match record JPA entity
│   └── Leaderboard_Entry.java            # Leaderboard JPA entity
├── Repository/
│   ├── MatchRecordRepo.java              # JPA repo + trimExcess() native query
│   └── LeaderboardRepo.java              # JPA repo + findAllByOrderByPositionAsc()
├── Security/
│   └── JwtService.java                   # JWT validation & PlayFabId extraction
└── Service/
    ├── MatchRecordService.java           # Save match + trim to 20
    └── FetchLeaderboardService.java      # Hourly PlayFab sync → Redis + PostgreSQL
```

---

## Related Services

| Service | Role |
|---|---|
| `authenticator-service` | Issues JWTs consumed by this service |
| `Transaction-Microservice` | Shares the same PlayFab title and JWT secret |

---

## License

ISC
