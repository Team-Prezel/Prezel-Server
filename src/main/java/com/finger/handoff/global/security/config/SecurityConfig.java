package com.finger.handoff.global.security.config;

import com.finger.handoff.global.security.filter.JwtAuthenticationFilter;
import com.finger.handoff.global.security.handler.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity // 스프링 시큐리티 필터 체인을 활성화합니다.
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. CORS 및 CSRF 기본 설정 (기본 출입 수칙)
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // 프론트엔드 접근 허용
                .csrf(AbstractHttpConfigurer::disable) // JWT를 쓰므로 CSRF 방어는 끕니다.
                .formLogin(AbstractHttpConfigurer::disable) // 기본 로그인 폼 안 씁니다.
                .httpBasic(AbstractHttpConfigurer::disable) // 기본 인증 방식 안 씁니다.

                // 세션을 쓰지 않고 상태가 없는(Stateless) JWT 방식을 쓴다고 선언합니다.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 2. API 주소별 접근 권한 설정 (사내 지도 보안 등급)
                .authorizeHttpRequests(auth -> auth
                        // 로그인, 토큰 재발급 등 인증이 필요 없는 API는 프리패스 (로비)
                        .requestMatchers("/auth/login", "/auth/reissue").permitAll()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        // 그 외의 모든 요청은 무조건 인증(사원증)이 필요함 (보안 구역)
                        .anyRequest().authenticated()
                )

                // 3. JWT 필터 등록 (보안 요원 배치)
                // 원래 스프링이 비밀번호를 검사하는 필터(UsernamePasswordAuthenticationFilter)가 작동하기 '전'에
                // 우리가 만든 JWT 필터가 먼저 작동하도록 정문에 배치합니다.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customAuthenticationEntryPoint) // 401 에러 전담
                );

        return http.build();
    }

    // CORS 설정 명부 (프론트엔드 출입 허가증)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 프론트엔드 주소 허용 (예: 로컬 리액트, 배포된 도메인 등)
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true); // 쿠키나 인증 정보를 포함한 요청 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // 모든 주소에 위 설정 적용
        return source;
    }
}