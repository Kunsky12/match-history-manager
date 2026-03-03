package com.mekheainteracive.match_history_manager.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "Match_Records")
public class MatchHistory_Entry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "playfab_Id", nullable = false)
    private String playfabId;

    private Integer selectedFighter;
    private String matchSummary;
    private Integer leaguePoints;
    private Integer exp;
    private String result;
    private String matchType;
    private String currentRound;
    private String roundTimer;
    private String gameMode;
    private String date;
    private String time;

    private LocalDateTime timestamp;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}