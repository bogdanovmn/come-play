package com.github.bogdanovmn.comeplay.user;

import com.github.bogdanovmn.comeplay.common.PlayerSkillRepository;
import com.github.bogdanovmn.comeplay.common.SkillLevel;
import com.github.bogdanovmn.comeplay.sport.SportType;
import com.github.bogdanovmn.comeplay.sport.SportTypeService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PlayerSkillRepository playerSkillRepository = mock(PlayerSkillRepository.class);
    private final SportTypeService sportTypeService = mock(SportTypeService.class);
    private final UserService userService = new UserService(userRepository, playerSkillRepository, sportTypeService);

    @Test
    void updateSettingsRejectsUnknownSportType() {
        UUID userId = UUID.randomUUID();
        when(sportTypeService.requireById(999)).thenThrow(new NoSuchElementException("not found"));
        SaveProfileRequest request = new SaveProfileRequest(
            "Имя",
            List.of(new SportSkillUpdate(999, SkillLevel.ADVANCED))
        );

        assertThatThrownBy(() -> userService.updateSettings(userId, request))
            .isInstanceOf(NoSuchElementException.class);

        verify(userRepository).updateDisplayName(userId, "Имя");
        verify(playerSkillRepository, never()).setUserSkill(eq(userId), anyInt(), any());
    }

    @Test
    void updateSettingsStoresAndClearsSkills() {
        UUID userId = UUID.randomUUID();
        when(sportTypeService.requireById(1)).thenReturn(SportType.builder().id(1).name("Бадминтон").build());
        when(sportTypeService.requireById(2)).thenReturn(SportType.builder().id(2).name("Шахматы").build());
        SaveProfileRequest request = new SaveProfileRequest(
            "Новое имя",
            List.of(
                new SportSkillUpdate(1, SkillLevel.ADVANCED),
                new SportSkillUpdate(2, null)
            )
        );

        userService.updateSettings(userId, request);

        verify(userRepository).updateDisplayName(userId, "Новое имя");
        verify(playerSkillRepository).setUserSkill(userId, 1, SkillLevel.ADVANCED);
        verify(playerSkillRepository).deleteUserSkill(userId, 2);
    }

    @Test
    void updateSettingsWithNoSkillsKeepsNameOnly() {
        UUID userId = UUID.randomUUID();
        SaveProfileRequest request = new SaveProfileRequest("Только имя", List.of());

        userService.updateSettings(userId, request);

        verify(userRepository).updateDisplayName(userId, "Только имя");
        verify(playerSkillRepository, never()).setUserSkill(any(), anyInt(), any());
        verify(playerSkillRepository, never()).deleteUserSkill(any(), anyInt());
    }
}