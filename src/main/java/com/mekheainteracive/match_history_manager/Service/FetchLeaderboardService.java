package com.mekheainteracive.match_history_manager.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mekheainteracive.match_history_manager.Entity.Leaderboard_Entry;
import com.mekheainteracive.match_history_manager.Repository.LeaderboardRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import redis.clients.jedis.UnifiedJedis;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class FetchLeaderboardService {

    private final RestClient restClient = RestClient.create();
    private final LeaderboardRepo repository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UnifiedJedis jedis;

    private static final String REDIS_KEY = "leaderboard:global";
    private static final int TTL_SECONDS = 3600; // 1 hour

    @Value("${playfab.title-id}")
    private String titleId;

    @Value("${playfab.secret-key}")
    private String secretKey;

    @Autowired
    public FetchLeaderboardService(LeaderboardRepo repository, UnifiedJedis jedis) {
        this.repository = repository;
        this.jedis = jedis;
    }

    // Latest leaderboard in memory (optional)
    private List<Leaderboard_Entry> latestLeaderboard = new ArrayList<>();

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        System.out.println("ℹ️ Fetch leaderboard on startup...");
        fetchWeeklyLeaderboardAutomatically();
    }

    @Transactional
    @Scheduled(fixedRate = 3600000) // every 1 hour in milliseconds
    public void fetchWeeklyLeaderboardAutomatically() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.plusHours(1);
        System.out.println("ℹ️ Fetching leaderboard at: " + LocalDateTime.now());
        System.out.println("ℹ️ Next leaderboard fetch scheduled at: " + nextRun);

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("StatisticName", "LP");
            requestBody.put("StartPosition", 1);
            requestBody.put("MaxResultsCount", 20);
            requestBody.put("ProfileConstraints", Map.of(
                    "ShowDisplayName", true,
                    "ShowAvatarUrl", true,
                    "ShowLocations", true
            ));

            Map response = restClient.post()
                    .uri("https://" + titleId + ".playfabapi.com/Server/GetLeaderboard")
                    .header("X-SecretKey", secretKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            List<Map<String, Object>> leaderboardList = (List<Map<String, Object>>) data.get("Leaderboard");

            if (leaderboardList == null || leaderboardList.isEmpty()) {
                System.out.println("❌ Leaderboard empty");
                return;
            }

            List<Leaderboard_Entry> entities = new ArrayList<>();

            for (Map<String, Object> entry : leaderboardList) {
                Leaderboard_Entry lb = new Leaderboard_Entry();
                lb.setPlayfabId((String) entry.get("PlayFabId"));
                lb.setLeaguePoints(((Number) entry.get("StatValue")).intValue());
                lb.setPosition(((Number) entry.get("Position")).intValue());

                Map<String, Object> profile = (Map<String, Object>) entry.get("Profile");
                if (profile != null) {
                    lb.setDisplayName((String) profile.getOrDefault("DisplayName", "Unknown"));
                    lb.setFacebookId((String) profile.getOrDefault("AvatarUrl", ""));
                    List<Map<String, Object>> locations = (List<Map<String, Object>>) profile.get("Locations");
                    if (locations != null && !locations.isEmpty()) {
                        lb.setCountryCode((String) locations.get(0).get("CountryCode"));
                    }
                }

                entities.add(lb);
            }

            // Save to DB (backup)
            repository.deleteAll();
            repository.saveAll(entities);

            // Save to Redis (primary)
            String json = objectMapper.writeValueAsString(entities);
            jedis.setex(REDIS_KEY, TTL_SECONDS, json);

            latestLeaderboard = entities;
            System.out.println("✅ Leaderboard saved to DB and Redis");
            //System.out.println("ℹ️ Next leaderboard fetch scheduled at: " + LocalDateTime.now().plusHours(1));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Player fetch endpoint
    public List<Leaderboard_Entry> getLeaderboard() {
        try {
            String cached = jedis.get(REDIS_KEY);
            if (cached != null) {
                System.out.println("ℹ️ Leaderboard from Redis");
                return objectMapper.readValue(cached, new TypeReference<>() {});
            }

            System.out.println("⚠️ Redis empty, fallback to DB");
            List<Leaderboard_Entry> leaderboard = repository.findAllByOrderByPositionAsc();

            // Rebuild Redis cache
            jedis.setex(REDIS_KEY, TTL_SECONDS, objectMapper.writeValueAsString(leaderboard));

            return leaderboard;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
