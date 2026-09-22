package com.github.bogdanovmn.comeplay.history;

import com.github.bogdanovmn.comeplay.common.TrainingSlot;
import com.github.bogdanovmn.comeplay.infrastructure.security.CurrentUserId;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/clubs/{clubId}/history")
class HistoryController {

    private final HistoryService historyService;

    HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    List<TrainingSlot> pastTrainings(
            @PathVariable UUID clubId,
            @RequestParam(defaultValue = "30") int days,
            @CurrentUserId UUID userId
    ) {
        return historyService.pastTrainings(clubId, days, userId);
    }

    @GetMapping("/by-day")
    List<VisitByDay> visitByDay(
            @PathVariable UUID clubId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @CurrentUserId UUID userId
    ) {
        return historyService.visitByDay(clubId, from, to, userId);
    }

    @GetMapping("/by-player")
    List<VisitByPlayer> visitByPlayer(
            @PathVariable UUID clubId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @CurrentUserId UUID userId
    ) {
        return historyService.visitByPlayer(clubId, from, to, userId);
    }
}
