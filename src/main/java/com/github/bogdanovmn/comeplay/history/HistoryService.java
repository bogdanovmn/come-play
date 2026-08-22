package com.github.bogdanovmn.comeplay.history;

import com.github.bogdanovmn.comeplay.security.AccessManagement;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class HistoryService {

    private final HistoryRepository historyRepository;
    private final AccessManagement accessManagement;

    @Transactional
    public void recordVisit(UUID clubId, UUID userId, LocalDate slotDate, String sportType) {
        historyRepository.recordVisit(clubId, userId, slotDate, sportType);
    }

    @Transactional(readOnly = true)
    public List<VisitByDay> visitByDay(UUID clubId, LocalDate from, LocalDate to, UUID userId) {
        accessManagement.requireMember(clubId, userId);
        return historyRepository.visitSummaryByDay(clubId, from, to);
    }

    @Transactional(readOnly = true)
    public List<VisitByPlayer> visitByPlayer(UUID clubId, LocalDate from, LocalDate to, UUID userId) {
        accessManagement.requireMember(clubId, userId);
        return historyRepository.visitSummaryByPlayer(clubId, from, to);
    }
}
