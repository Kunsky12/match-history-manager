package com.mekheainteracive.match_history_manager.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class FetchLeaderboardService {
    private final RestClient restClient = RestClient.create(); // local instance, no autowire

    @Value("${playfab.title-id}")
    private String titleId;

    @Value("${playfab.secret-key}")
    private String secretKey;

    @Getter
    private Map latestWeeklyLeaderboard;


    @Scheduled(fixedRate = 30000)
    public void fetchWeeklyLeaderboardAutomatically() {

        System.out.println("🔥 Scheduler Running at: " + LocalDateTime.now());

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("StatisticName", "RP");
            requestBody.put("StartPosition", 0);
            requestBody.put("MaxResultsCount", 20);

            // Fetch as String to avoid gzip issue
            String response = restClient.post()
                    .uri("https://" + titleId + ".playfabapi.com/Server/GetLeaderboard")
                    .header("X-SecretKey", secretKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            System.out.println("✅ Response: " + response);

        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
