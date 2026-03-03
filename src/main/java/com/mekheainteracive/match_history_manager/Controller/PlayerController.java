package com.mekheainteracive.match_history_manager.Controller;

import com.mekheainteracive.match_history_manager.Entity.Leaderboard_Entry;
import com.mekheainteracive.match_history_manager.Entity.MatchHistory_Entry;
import com.mekheainteracive.match_history_manager.Security.JwtService;
import com.mekheainteracive.match_history_manager.Service.FetchLeaderboardService;
import com.mekheainteracive.match_history_manager.Service.MatchRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin
public class PlayerController {

    private final FetchLeaderboardService leaderboardService;
    private final MatchRecordService matchRecordService;
    private final JwtService jwtService;

    // POST /api/matches
    @PostMapping("/matches")
    public MatchHistory_Entry saveMatch(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody MatchHistory_Entry record) {

        // 1. Extract token
        String playFabId = Authenticate(authHeader);

        // 4. Save match
        return matchRecordService.save(playFabId, record);
    }

    // GET /api/matches
    @GetMapping("/matches")
    public List<MatchHistory_Entry> getHistory(@RequestHeader("Authorization") String authHeader) {

        Authenticate(authHeader);
        // 1. Extract token
        String playFabId = Authenticate(authHeader);
        return matchRecordService.getMatchHistory(playFabId);
    }

    private String Authenticate(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }          
        String token = authHeader.substring(7);

        if (!jwtService.validateToken(token)) {
            throw new RuntimeException("Invalid JWT token");
        }

        String playFabId = jwtService.extractPlayfabId(token);

        return playFabId;
    }

    // GET /api/leaderboard
    @GetMapping("/leaderboard")
    public List<Leaderboard_Entry> getLeaderboard() {
        return leaderboardService.getLeaderboard();
    }
}
