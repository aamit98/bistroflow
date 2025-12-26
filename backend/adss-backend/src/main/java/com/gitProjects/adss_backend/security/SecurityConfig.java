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
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
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
    private final boolean devSurfacesEnabled;

    public SecurityConfig(JwtService jwtService, Environment environment) {
        this.jwtService = jwtService;
        this.devSurfacesEnabled = environment.acceptsProfiles(Profiles.of("dev", "test"));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sess ->
                        sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(
                            "/api/auth/**",
                            "/health",
                            "/actuator/health",
                            "/ws/**"
                    ).permitAll();

                    if (devSurfacesEnabled) {
                        auth.requestMatchers(
                                "/api/admin/demo/**",
                                "/h2-console/**"
                        ).permitAll();
                    }

                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtService),
                        UsernamePasswordAuthenticationFilter.class
                );

        if (devSurfacesEnabled) {
            // allow H2 console frames only in dev/test
            http.headers(headers -> headers.frameOptions(frame -> frame.disable()));
        }

        return http.build();
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
            if (log.isDebugEnabled()) {
                log.debug("JWT filter {} {} (auth header present: {})",
                        request.getMethod(), request.getRequestURI(), authHeader != null);
            }
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.trace("Skipping JWT processing because no bearer token was provided");
                filterChain.doFilter(request, response);
                return;
            }

            String token = authHeader.substring(7);
            try {
                Claims claims = jwtService.parseToken(token);

                // employeeId is now in its own claim
                Integer employeeId = claims.get("employeeId", Integer.class);
                if (employeeId == null) {
                    // fallback for old tokens where subject = employeeId
                    try {
                        employeeId = Integer.valueOf(claims.getSubject());
                    } catch (NumberFormatException nfe) {
                        throw new RuntimeException("Cannot determine employeeId from token");
                    }
                }

                Boolean hrManager = claims.get("hrManager", Boolean.class);
                Boolean superAdmin = claims.get("superAdmin", Boolean.class);
                Integer branchId = claims.get("branchId", Integer.class);

                @SuppressWarnings("unchecked")
                List<String> roles =
                        (List<String>) claims.getOrDefault("roles", new ArrayList<>());

                List<GrantedAuthority> authorities = new ArrayList<>();
                if (Boolean.TRUE.equals(superAdmin)) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
                }
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
                        Boolean.TRUE.equals(superAdmin),
                        authorities
                );

                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("Authenticated request for employee {} (HR={}, superAdmin={})",
                        employeeId, hrManager, superAdmin);
            } catch (Exception e) {
                log.warn("Failed to parse/validate JWT: {}", e.getMessage());
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
        private final boolean superAdmin;

        public EmployeeAuthentication(
                Integer employeeId,
                Integer branchId,
                boolean hrManager,
                boolean superAdmin,
                List<? extends GrantedAuthority> authorities
        ) {
            super(authorities);
            this.employeeId = employeeId;
            this.branchId = branchId;
            this.hrManager = hrManager;
            this.superAdmin = superAdmin;
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

        public boolean isSuperAdmin() {
            return superAdmin;
        }
    }
}
