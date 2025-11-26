package com.gitProjects.adss_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    public SecurityConfig(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sess ->
                        sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                        "/api/auth/**",
                        "/health",
                        "/actuator/health",
                        "/h2-console/**",
                        "/ws/**"
                    ).permitAll()
                    .anyRequest().authenticated()
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtService),
                        UsernamePasswordAuthenticationFilter.class
                )
                // For H2 console
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .build();
    }

    /**
     * Simple JWT filter:
     * - reads Authorization: Bearer <token>
     * - parses JWT
     * - builds Authentication with employeeId, hrManager, branchId, roles.
     */
    static class JwtAuthenticationFilter extends OncePerRequestFilter {

        private final JwtService jwtService;

        public JwtAuthenticationFilter(JwtService jwtService) {
            this.jwtService = jwtService;
        }

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain
        ) throws ServletException, IOException {

            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            System.out.println("[JWT Filter] " + request.getMethod() + " " + request.getRequestURI());
            System.out.println("[JWT Filter] Authorization header: " + authHeader);
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                System.out.println("[JWT Filter] No bearer token, continuing...");
                filterChain.doFilter(request, response);
                return;
            }

            String token = authHeader.substring(7);
            try {
                Claims claims = jwtService.parseToken(token);
                System.out.println("[JWT Filter] Token parsed successfully");
                System.out.println("[JWT Filter] subject: " + claims.getSubject());
                System.out.println("[JWT Filter] hrManager: " + claims.get("hrManager"));

                String subject = claims.getSubject(); // employeeId as string
                Integer employeeId = Integer.valueOf(subject);

                Boolean hrManager = claims.get("hrManager", Boolean.class);
                Integer branchId = claims.get("branchId", Integer.class);

                @SuppressWarnings("unchecked")
                List<String> roles =
                        (List<String>) claims.getOrDefault("roles", new ArrayList<>());

                List<GrantedAuthority> authorities = new ArrayList<>();
                if (Boolean.TRUE.equals(hrManager)) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_HR_MANAGER"));
                }
                for (String role : roles) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                }

                EmployeeAuthentication auth = new EmployeeAuthentication(
                        employeeId,
                        branchId,
                        Boolean.TRUE.equals(hrManager),
                        authorities
                );

                SecurityContextHolder.getContext().setAuthentication(auth);
                System.out.println("[JWT Filter] Authentication set for employee " + employeeId + ", HR: " + hrManager);
            } catch (Exception e) {
                log.warn("Failed to parse/validate JWT: {}", e.getMessage());
                System.out.println("[JWT Filter] Failed to parse JWT: " + e.getMessage());
                // don't set auth, just continue; request will be rejected if endpoint requires auth
            }

            filterChain.doFilter(request, response);
        }
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    /**
     * Authentication object that holds employee data.
     */
    static class EmployeeAuthentication extends AbstractAuthenticationToken {

        private final Integer employeeId;
        private final Integer branchId;
        private final boolean hrManager;

        public EmployeeAuthentication(
                Integer employeeId,
                Integer branchId,
                boolean hrManager,
                List<? extends GrantedAuthority> authorities
        ) {
            super(authorities);
            this.employeeId = employeeId;
            this.branchId = branchId;
            this.hrManager = hrManager;
            setAuthenticated(true);
        }

        @Override
        public Object getCredentials() {
            return hrManager; // Return hrManager flag for easy access in controllers
        }

        @Override
        public Object getPrincipal() {
            return employeeId;
        }

        public Integer getEmployeeId() {
            return employeeId;
        }

        public Integer getBranchId() {
            return branchId;
        }

        public boolean isHrManager() {
            return hrManager;
        }
    }
}
