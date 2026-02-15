package com.mekheainteracive.match_history_manager.controller;

import com.mekheainteracive.match_history_manager.entity.MatchRecord;
import com.mekheainteracive.match_history_manager.service.MatchRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recordmatch")
@RequiredArgsConstructor
@CrossOrigin
public class MatchRecordController {

    private final MatchRecordService service;

    @PostMapping("/{playfabId}")
    public MatchRecord saveMatch(
            @PathVariable String playfabId,
            @RequestBody MatchRecord record) {

        return service.save(playfabId, record);
    }

    @GetMapping("/{playfabId}")
    public List<MatchRecord> getHistory(@PathVariable String playfabId) {
        return service.getPlayerHistory(playfabId);
    }
}
