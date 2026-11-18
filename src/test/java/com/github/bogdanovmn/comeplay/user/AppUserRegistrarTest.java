package com.github.bogdanovmn.comeplay.user;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AppUserRegistrarTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final AppUserRegistrar registrar = new AppUserRegistrar(userRepository);

    @Test
    void createsMissingLocalUser() {
        UUID userId = UUID.randomUUID();

        registrar.ensureRegistered(userId, "Игрок");

        verify(userRepository).getOrCreate(userId, "Игрок");
    }
}
