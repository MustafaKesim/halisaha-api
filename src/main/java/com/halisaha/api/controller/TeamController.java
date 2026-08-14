package com.halisaha.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.halisaha.api.dto.CreateTeamRequest;
import com.halisaha.api.dto.TeamResponse;
import com.halisaha.api.entity.Team;
import com.halisaha.api.entity.User;
import com.halisaha.api.service.TeamService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Takımlar", description = "Takım oluşturma")
@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @Operation(summary = "Yeni bir takım oluşturur, oluşturan kullanıcı otomatik kaptan olur")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Takım oluşturuldu"),
            @ApiResponse(responseCode = "400", description = "Geçersiz veri (isim boş)")
    })
    @PostMapping
    public ResponseEntity<TeamResponse> create(@Valid @RequestBody CreateTeamRequest request,
                                                @AuthenticationPrincipal User user) {
        Team team = teamService.createTeam(request.name(), user);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(team));
    }

    @Operation(summary = "Giriş yapmış kullanıcının kaptanı olduğu takımları listeler")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Takım listesi döndü"))
    @GetMapping("/me")
    public ResponseEntity<List<TeamResponse>> myTeams(@AuthenticationPrincipal User user) {
        List<TeamResponse> teams = teamService.getMyTeams(user).stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(teams);
    }

    private TeamResponse toResponse(Team team) {
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getCaptain().getId(),
                team.getCaptain().getFullName()
        );
    }
}
