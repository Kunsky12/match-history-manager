package com.mekheainteracive.match_history_manager.Service;

import com.mekheainteracive.match_history_manager.Entity.MatchHistory_Entry;
import com.mekheainteracive.match_history_manager.Repository.MatchRecordRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchRecordService {

    private final MatchRecordRepo repository;

    @Transactional
    public MatchHistory_Entry save(String playfabId, MatchHistory_Entry record) {

        record.setPlayfabId(playfabId);

        MatchHistory_Entry savedRecord = repository.save(record);

        repository.trimExcess(playfabId); // keep only last 20 matches

        return savedRecord;
    }

    public List<MatchHistory_Entry> getMatchHistory(String playfabId) {
        return repository.findAllByPlayfabId(playfabId);
    }


}
