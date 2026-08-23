package com.nexus.cms.config;

import com.nexus.cms.chat.service.interfaces.PresenceService;
import com.nexus.cms.payload.TokenPayloadDto;
import com.nexus.cms.util.CommonUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ActivityFilter extends OncePerRequestFilter {
    private final PresenceService presenceService;
    private final CommonUtils commonUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null) {
            TokenPayloadDto tokenPayloadDto = commonUtils.decryptToken(token);
            presenceService.heartbeat(tokenPayloadDto.getUserId());
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (commonUtils.validateToken(token)) {
                return token;
            }
        }
        return null;
    }
}
