package com.github.bogdanovmn.comeplay.club;

import com.github.bogdanovmn.comeplay.security.AccessManagement;
import com.github.bogdanovmn.comeplay.sport.SportTypeService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClubServiceTest {

    private final ClubRepository clubRepository = mock(ClubRepository.class);
    private final AccessManagement accessManagement = mock(AccessManagement.class);
    private final SportTypeService sportTypeService = mock(SportTypeService.class);
    private final ClubService clubService = new ClubService(clubRepository, accessManagement, sportTypeService);

    @Test
    void getDeniedForNonMemberAndNeverReadsClub() {
        UUID clubId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        doThrow(new AccessDeniedException("no access"))
            .when(accessManagement).requireMember(clubId, userId);

        assertThatThrownBy(() -> clubService.get(clubId, userId))
            .isInstanceOf(AccessDeniedException.class);

        verify(clubRepository, never()).findById(clubId);
    }

    @Test
    void getChecksAccessOnEveryCall() {
        UUID clubId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Club club = Club.builder().id(clubId).build();
        when(clubRepository.findById(clubId)).thenReturn(Optional.of(club));

        assertThat(clubService.get(clubId, userId)).isSameAs(club);
        assertThat(clubService.get(clubId, userId)).isSameAs(club);

        verify(accessManagement, times(2)).requireMember(clubId, userId);
        verify(clubRepository, times(2)).findById(clubId);
    }
}