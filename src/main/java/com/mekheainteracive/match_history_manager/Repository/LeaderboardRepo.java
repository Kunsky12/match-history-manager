package com.mekheainteracive.match_history_manager.Repository;

import com.mekheainteracive.match_history_manager.Entity.Leaderboard_Entry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaderboardRepo extends JpaRepository<Leaderboard_Entry, Long> {
    List<Leaderboard_Entry> findAllByOrderByPositionAsc();
}
