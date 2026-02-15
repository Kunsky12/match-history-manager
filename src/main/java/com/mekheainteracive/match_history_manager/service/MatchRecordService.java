package com.mekheainteracive.match_history_manager.service;

import com.mekheainteracive.match_history_manager.entity.LeaderboardEntry;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import com.mekheainteracive.match_history_manager.entity.MatchRecord;
import com.mekheainteracive.match_history_manager.repository.MatchRecordRepository;
import org.springframework.web.bind.annotation.GetMapping;

@Service
@RequiredArgsConstructor
public class MatchRecordService {

    private final MatchRecordRepository repository;

    @Transactional
    public MatchRecord save(String playfabId, MatchRecord record) {

        record.setPlayfabId(playfabId);

        repository.save(record);

        repository.trimExcess(playfabId);

        return record;
    }

    public List<MatchRecord> getHistory(String playfabId) {
        return repository.findAllByPlayfabId(playfabId);
    }

}
