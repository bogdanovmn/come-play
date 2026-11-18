package com.github.bogdanovmn.comeplay.infrastructure.security;

import com.github.bogdanovmn.comeplay.user.AppUserRegistrar;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
class JwtTokenFilter extends OncePerRequestFilter {

	private final JwtInstance jwtInstance;
	private final JwtBasedUserDetailsFactory jwtBasedUserDetailsFactory;
	private final AppUserRegistrar appUserRegistrar;

	@Override
	protected void doFilterInternal(HttpServletRequest request,
									HttpServletResponse response,
									FilterChain chain) throws ServletException, IOException
	{
		// Get authorization header and validate
		Optional<String> token = new HttpRequest(request).authToken();
		if (token.isEmpty()) {
			chain.doFilter(request, response);
			return;
		}

		// Get jwt token and validate
		Jws<Claims> parsedToken;
		try {
			parsedToken = jwtInstance.checkSignatureAndReturnClaims(token.get());
		} catch (Exception ex) {
			log.warn("JWT token parsing error: {}, token: {}", ex.getMessage(), token);
			chain.doFilter(request, response);
			return;
		}

		UserDetails userDetails = jwtBasedUserDetailsFactory.fromJwtClaims(parsedToken.getBody());
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
			userDetails,
			null,
			userDetails.getAuthorities()
		);

		authentication.setDetails(
			new WebAuthenticationDetailsSource().buildDetails(request)
		);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		// The user may be authenticated via a shared SSO token without ever calling
		// the profile API, so create the local app_user record before any business
		// operation can reference it through a foreign key.
		if (userDetails instanceof JwtBasedUserDetails jwtUserDetails) {
			appUserRegistrar.ensureRegistered(
				jwtUserDetails.getUserId(),
				jwtUserDetails.getUsername()
			);
		}

		chain.doFilter(request, response);
	}

}
