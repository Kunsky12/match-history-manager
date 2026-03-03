package com.mekheainteracive.match_history_manager.Repository;

import com.mekheainteracive.match_history_manager.Entity.MatchHistory_Entry;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MatchRecordRepo extends JpaRepository<MatchHistory_Entry, Long> {
    List<MatchHistory_Entry> findAllByPlayfabId(String playfabId);

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
