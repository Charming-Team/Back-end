package charming.server.global.security;

import charming.server.domain.user.entity.User;
import charming.server.domain.user.repository.UserRepository;
import charming.server.global.common.BaseResponse;
import charming.server.global.error.CustomException;
import charming.server.global.error.ErrorCode;
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

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

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
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));
            if (!user.isActive()) {
                throw new CustomException(ErrorCode.INACTIVE_ACCOUNT);
            }

            AuthUser authUser = new AuthUser(user.getId(), user.getEmail(), user.getRole());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    authUser,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug(
                    "[JwtAuthenticationFilter] 인증 성공 userId={}, email={}, role={}, path={}",
                    user.getId(),
                    user.getEmail(),
                    user.getRole(),
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
}
