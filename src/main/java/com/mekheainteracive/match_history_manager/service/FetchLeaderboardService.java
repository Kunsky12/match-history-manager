package com.mekheainteracive.match_history_manager.service;

import com.mekheainteracive.match_history_manager.entity.LeaderboardEntry;
import com.mekheainteracive.match_history_manager.repository.LeaderboardRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class FetchLeaderboardService {

    private final RestClient restClient = RestClient.create();
    private final LeaderboardRepository repository;

    public FetchLeaderboardService(LeaderboardRepository repository) {
        this.repository = repository;
    }

    @Value("${playfab.title-id}")
    private String titleId;

    @Value("${playfab.secret-key}")
    private String secretKey;

    @Getter
    private List<LeaderboardEntry> latestLeaderboard = new ArrayList<>();

    // ✅ RUN ON STARTUP
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        System.out.println("🚀 Fetch leaderboard on startup...");
        fetchWeeklyLeaderboardAutomatically();
    }

    // ✅ RUN EVERY MONDAY 8 AM (CAMBODIA TIME)
    @Transactional
    @Scheduled(cron = "0 0 8 ? * MON", zone = "Asia/Phnom_Penh")
    public void fetchWeeklyLeaderboardAutomatically() {

        System.out.println("🔥 Fetching leaderboard at: " + LocalDateTime.now());

        try {
            // ✅ REQUEST BODY
            Map<String, Object> requestBody = new HashMap<>();

            requestBody.put("StatisticName", "LP");
            requestBody.put("StartPosition", 1);
            requestBody.put("MaxResultsCount", 20);

            // 🔥 IMPORTANT: INCLUDE PROFILE DATA
            requestBody.put("ProfileConstraints", Map.of(
                    "ShowDisplayName", true,
                    "ShowAvatarUrl", true,
                    "ShowLocations", true
            ));

            // ✅ CALL PLAYFAB
            Map<String, Object> response = restClient.post()
                    .uri("https://" + titleId + ".playfabapi.com/Server/GetLeaderboard")
                    .header("X-SecretKey", secretKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> data = (Map<String, Object>) response.get("data");

            List<Map<String, Object>> leaderboardList =
                    (List<Map<String, Object>>) data.get("Leaderboard");

            if (leaderboardList == null || leaderboardList.isEmpty()) {
                System.out.println("❌ Leaderboard empty");
                return;
            }

            List<LeaderboardEntry> entities = new ArrayList<>();

            for (Map<String, Object> entry : leaderboardList) {

                String playFabId = (String) entry.get("PlayFabId");

                Number statValue = (Number) entry.get("StatValue");
                Number position = (Number) entry.get("Position");

                // PROFILE DATA
                Map<String, Object> profile =
                        (Map<String, Object>) entry.get("Profile");

                String displayName = "Unknown";
                String avatarUrl = "";
                String country = "UNKNOWN";

                if (profile != null) {

                    displayName = (String) profile.getOrDefault("DisplayName", "Unknown");
                    avatarUrl = (String) profile.getOrDefault("AvatarUrl", "");

                    List<Map<String, Object>> locations =
                            (List<Map<String, Object>>) profile.get("Locations");

                    if (locations != null && !locations.isEmpty()) {
                        country = (String) locations.get(0).get("CountryCode");
                    }
                }

                LeaderboardEntry lb = new LeaderboardEntry();

                lb.setPlayfabId(playFabId);
                lb.setDisplayName(displayName);
                lb.setFacebookId(avatarUrl);
                lb.setCountryCode(country);
                lb.setLeaguePoints(statValue.intValue());
                lb.setPosition(position.intValue());

                entities.add(lb);
            }

            // ✅ SAVE TO DB (FAST)
            repository.deleteAll();
            repository.saveAll(entities);

            latestLeaderboard = entities;

            System.out.println("✅ Leaderboard saved successfully");

        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }


}
