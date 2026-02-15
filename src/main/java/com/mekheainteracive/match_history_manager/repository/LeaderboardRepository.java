package com.mekheainteracive.match_history_manager.repository;

import com.mekheainteracive.match_history_manager.entity.LeaderboardEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaderboardRepository
        extends JpaRepository<LeaderboardEntry, Long> {

    List<LeaderboardEntry> findAllByOrderByPositionAsc();

    void deleteAll();
}
