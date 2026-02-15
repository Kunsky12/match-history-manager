package com.mekheainteracive.match_history_manager.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "Top_20_Leaderboard")
public class LeaderboardEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "playfab_Id", nullable = false)
    private String playfabId;

    @Column(name = "display_name")
    private String displayName;

    private int leaguePoints;

    @Column(nullable = false)
    private int position;

    @Column
    private String facebookId;
    private String countryCode;

    // getters & setters
}
