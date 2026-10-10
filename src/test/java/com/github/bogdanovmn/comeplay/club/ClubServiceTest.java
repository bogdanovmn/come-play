package com.github.bogdanovmn.comeplay.club;

import com.github.bogdanovmn.comeplay.common.PlayerSkillRepository;
import com.github.bogdanovmn.comeplay.common.SkillLevel;
import com.github.bogdanovmn.comeplay.security.AccessManagement;
import com.github.bogdanovmn.comeplay.sport.SportType;
import com.github.bogdanovmn.comeplay.sport.SportTypeService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
    private final PlayerSkillRepository playerSkillRepository = mock(PlayerSkillRepository.class);
    private final ClubService clubService = new ClubService(
        clubRepository, accessManagement, sportTypeService, playerSkillRepository, 100
    );

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

    @Test
    void setMemberSkillRequiresOwner() {
        UUID clubId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        clubService.setMemberSkill(clubId, memberId, SkillLevel.ADVANCED, ownerId);

        verify(accessManagement).requireOwner(clubId, ownerId);
        verify(accessManagement).requireMember(clubId, memberId);
        verify(playerSkillRepository).setClubOverride(clubId, memberId, SkillLevel.ADVANCED);
    }

    @Test
    void setMemberSkillDeniedForNonOwner() {
        UUID clubId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        doThrow(new AccessDeniedException("no access"))
            .when(accessManagement).requireOwner(clubId, memberId);

        assertThatThrownBy(() -> clubService.setMemberSkill(clubId, memberId, SkillLevel.ADVANCED, memberId))
            .isInstanceOf(AccessDeniedException.class);

        verify(playerSkillRepository, never()).setClubOverride(clubId, memberId, SkillLevel.ADVANCED);
    }

    @Test
    void clearMemberSkillRequiresOwner() {
        UUID clubId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        clubService.clearMemberSkill(clubId, memberId, ownerId);

        verify(accessManagement).requireOwner(clubId, ownerId);
        verify(accessManagement).requireMember(clubId, memberId);
        verify(playerSkillRepository).deleteClubOverride(clubId, memberId);
    }

    @Test
    void updatePersistsScheduleDaysAndRequiresOwner() {
        UUID clubId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UpdateClubRequest request = new UpdateClubRequest("Клуб", 1, null, 7);

        clubService.update(clubId, request, ownerId);

        verify(accessManagement).requireOwner(clubId, ownerId);
        verify(clubRepository).update(clubId, "Клуб", 1, null, 7);
    }

    @Test
    void updateDeniedForNonOwner() {
        UUID clubId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        doThrow(new AccessDeniedException("no access"))
            .when(accessManagement).requireOwner(clubId, userId);

        assertThatThrownBy(() -> clubService.update(clubId, new UpdateClubRequest("Клуб", 1, null, 7), userId))
            .isInstanceOf(AccessDeniedException.class);

        verify(clubRepository, never()).update(clubId, "Клуб", 1, null, 7);
    }

    @Test
    void createFallsBackToDefaultScheduleDaysWhenNotProvided() {
        UUID ownerId = UUID.randomUUID();
        when(sportTypeService.requireById(1)).thenReturn(SportType.builder().id(1).name("Волейбол").build());
        when(clubRepository.create(any(), anyInt(), any(), anyInt(), any())).thenReturn(UUID.randomUUID());

        clubService.create(new CreateClubRequest("Клуб", 1, null, null), ownerId);

        verify(clubRepository).create("Клуб", 1, null, Club.DEFAULT_SCHEDULE_DAYS, ownerId);
    }

    @Test
    void createKeepsProvidedScheduleDays() {
        UUID ownerId = UUID.randomUUID();
        when(sportTypeService.requireById(1)).thenReturn(SportType.builder().id(1).name("Волейбол").build());
        when(clubRepository.create(any(), anyInt(), any(), anyInt(), any())).thenReturn(UUID.randomUUID());

        clubService.create(new CreateClubRequest("Клуб", 1, "Описание", 10), ownerId);

        verify(clubRepository).create("Клуб", 1, "Описание", 10, ownerId);
    }

    @Test
    void listCommentsRequiresMember() {
        UUID clubId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        doThrow(new AccessDeniedException("no access"))
            .when(accessManagement).requireMember(clubId, userId);

        assertThatThrownBy(() -> clubService.listComments(clubId, userId))
            .isInstanceOf(AccessDeniedException.class);

        verify(clubRepository, never()).listComments(clubId, 100);
    }

    @Test
    void listCommentsReturnsConfiguredAmountOfLatestComments() {
        UUID clubId = UUID.randomUUID();
        ClubService service = new ClubService(
            clubRepository, accessManagement, sportTypeService, playerSkillRepository, 5
        );

        service.listComments(clubId, UUID.randomUUID());

        verify(clubRepository).listComments(clubId, 5);
    }

    @Test
    void createCommentReturnsStoredComment() {
        UUID clubId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        when(clubRepository.createComment(clubId, userId, "Привет")).thenReturn(commentId);
        when(clubRepository.findCommentById(commentId)).thenReturn(Optional.of(ClubComment.builder().id(commentId).build()));

        assertThat(clubService.createComment(clubId, "Привет", userId)).isNotNull();

        verify(accessManagement).requireMember(clubId, userId);
        verify(clubRepository).createComment(clubId, userId, "Привет");
    }

    @Test
    void createCommentDeniedForNonMember() {
        UUID clubId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        doThrow(new AccessDeniedException("no access"))
            .when(accessManagement).requireMember(clubId, userId);

        assertThatThrownBy(() -> clubService.createComment(clubId, "Привет", userId))
            .isInstanceOf(AccessDeniedException.class);

        verify(clubRepository, never()).createComment(clubId, userId, "Привет");
    }
}