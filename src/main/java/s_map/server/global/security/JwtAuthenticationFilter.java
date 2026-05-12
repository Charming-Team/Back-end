package s_map.server.global.security;

import s_map.server.domain.user.entity.Role;
import s_map.server.global.common.BaseResponse;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;
import s_map.server.global.security.AuthUser;
import s_map.server.global.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String[] PUBLIC_EXACT_PATHS = {
            "/api/auth/login",
            "/api/token/refresh",
            "/swagger-ui.html",
            "/api/swagger-ui.html",
            "/actuator/health"
    };
    private static final String[] PUBLIC_PATH_PREFIXES = {
            "/swagger-ui/",
            "/v3/api-docs",
            "/api/swagger-ui/",
            "/api/v3/api-docs",
            "/actuator/health/"
    };

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        String path = getPathWithoutContextPath(request);
        for (String publicPath : PUBLIC_EXACT_PATHS) {
            if (path.equals(publicPath)) {
                return true;
            }
        }

        for (String publicPathPrefix : PUBLIC_PATH_PREFIXES) {
            if (path.startsWith(publicPathPrefix)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authorizationHeader.substring(BEARER_PREFIX.length());
            Map<String, Object> claims = jwtTokenProvider.getAccessTokenClaims(token);
            Long userId = jwtTokenProvider.getUserId(claims);
            String email = jwtTokenProvider.getSubject(claims);
            Role role = jwtTokenProvider.getRole(claims);

            s_map.server.global.security.AuthUser authUser = new AuthUser(userId, email, role);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    authUser,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug(
                    "[JwtAuthenticationFilter] 인증 성공 userId={}, email={}, role={}, path={}",
                    userId,
                    email,
                    role,
                    request.getRequestURI()
            );
            filterChain.doFilter(request, response);
        } catch (CustomException exception) {
            SecurityContextHolder.clearContext();
            log.warn(
                    "[JwtAuthenticationFilter] 인증 실패 reason={} path={}",
                    exception.getErrorCode().getCode(),
                    request.getRequestURI()
            );
            writeErrorResponse(response, exception.getErrorCode());
        }
    }

    private void writeErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), BaseResponse.fail(errorCode));
    }

    private String getPathWithoutContextPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            return path.substring(contextPath.length());
        }
        return path;
    }
}
