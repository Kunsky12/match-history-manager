package com.mekheainteracive.match_history_manager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.mekheainteracive.match_history_manager.entity.MatchRecord;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MatchRecordRepository extends JpaRepository<MatchRecord, Long> {

    // Get top 20 latest matches
    List<MatchRecord> findTop20ByPlayfabIdOrderByCreatedAtDesc(String playfabId);

    // Trim excess matches (keep only 20)
    @Modifying
    @Transactional
    @Query(value = """
        DELETE FROM match_records
        WHERE id IN (
            SELECT id FROM (
                SELECT id FROM match_records
                WHERE playfab_id = :playfabId
                ORDER BY created_at DESC
                OFFSET 20
            ) AS sub
        )
    """, nativeQuery = true)
    void trimExcess(@Param("playfabId") String playfabId);
}
