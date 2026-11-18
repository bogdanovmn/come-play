package com.github.bogdanovmn.comeplay.infrastructure.security;

import com.github.bogdanovmn.comeplay.user.AppUserRegistrar;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JwtTokenFilterTest {

    private final JwtInstance jwtInstance = mock(JwtInstance.class);
    private final JwtBasedUserDetailsFactory userDetailsFactory = mock(JwtBasedUserDetailsFactory.class);
    private final AppUserRegistrar appUserRegistrar = mock(AppUserRegistrar.class);
    private final JwtTokenFilter filter = new JwtTokenFilter(
        jwtInstance, userDetailsFactory, appUserRegistrar
    );

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registersUserForValidToken() throws Exception {
        UUID userId = UUID.randomUUID();
        Claims claims = mock(Claims.class);
        JwtBasedUserDetails userDetails = new JwtBasedUserDetails(userId, "Игрок", List.of());
        whenValidToken("token", claims, userDetails);

        filter.doFilter(requestWithToken("token"), new MockHttpServletResponse(), new MockFilterChain());

        verify(appUserRegistrar).ensureRegistered(userId, "Игрок");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void doesNotRegisterUserWithoutToken() throws Exception {
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());

        verifyNoInteractions(appUserRegistrar);
    }

    @Test
    void doesNotRegisterUserForInvalidToken() throws Exception {
        when(jwtInstance.checkSignatureAndReturnClaims("invalid")).thenThrow(new RuntimeException("invalid token"));

        filter.doFilter(requestWithToken("invalid"), new MockHttpServletResponse(), new MockFilterChain());

        verifyNoInteractions(appUserRegistrar);
    }

    @SuppressWarnings("unchecked")
    private void whenValidToken(String token, Claims claims, JwtBasedUserDetails userDetails) {
        Jws<Claims> jws = mock(Jws.class);
        when(jws.getBody()).thenReturn(claims);
        when(jwtInstance.checkSignatureAndReturnClaims(token)).thenReturn(jws);
        when(userDetailsFactory.fromJwtClaims(claims)).thenReturn(userDetails);
    }

    private static MockHttpServletRequest requestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return request;
    }
}
