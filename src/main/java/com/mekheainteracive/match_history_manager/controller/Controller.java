package com.mekheainteracive.match_history_manager.controller;

import com.mekheainteracive.match_history_manager.entity.LeaderboardEntry;
import com.mekheainteracive.match_history_manager.entity.MatchRecord;
import com.mekheainteracive.match_history_manager.service.FetchLeaderboardService;
import com.mekheainteracive.match_history_manager.service.MatchRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin
public class Controller {

    private final MatchRecordService matchRecordService;
    private final FetchLeaderboardService leaderboardService;

    @PostMapping("/matches/{playfabId}")
    public MatchRecord saveMatch(
            @PathVariable String playfabId,
            @RequestBody MatchRecord record) {

        return matchRecordService.save(playfabId, record);
    }

    @GetMapping("/matches/{playfabId}")
    public List<MatchRecord> getHistory(@PathVariable String playfabId) {
        return matchRecordService.getHistory(playfabId); // use the instance
    }

    @GetMapping("/leaderboard")
    public List<LeaderboardEntry> getLeaderboard() {
        // If you want to always fetch fresh data, uncomment:
        // leaderboardService.fetchWeeklyLeaderboardAutomatically();
        return leaderboardService.getLatestLeaderboard();
    }
}
