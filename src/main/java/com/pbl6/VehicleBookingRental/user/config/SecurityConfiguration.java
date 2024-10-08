package com.pbl6.VehicleBookingRental.user.config;

import java.io.IOException;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.pbl6.VehicleBookingRental.user.service.CustomOAuth2User;
import com.pbl6.VehicleBookingRental.user.service.CustomOAuth2UserService;
import com.pbl6.VehicleBookingRental.user.util.SecurityUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.util.Base64;

@Configuration
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfiguration {
    @Value("${pbl6.jwt.base64-secret}")
    private String jwtKey;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

   
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CustomAuthenticationEntryPoint customAuthenticationEntryPoint, 
    CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler) throws Exception {
        http
                .csrf(c->c.disable())
                .cors(Customizer.withDefaults())
                // .antMatcher("/secured/**")
                .authorizeHttpRequests(
                        authz -> authz
                                .requestMatchers("/", "/api/v1/auth/login", "/api/v1/auth/refresh", 
                                                "/api/v1/auth/register", "/api/v1/auth/verify", 
                                                "/api/v1/auth/resend_otp", "/api/v1/auth/register-info", "/api/v1/auth/google-login").permitAll()
                                .anyRequest().authenticated()
                                )
                // .oauth2Login(oauth2 -> oauth2
                //     .loginPage("/api/v1/auth/google-login")  // Chỉ định trang login cho OAuth2
                //     .authorizationEndpoint(authorization -> authorization
                //         .baseUri("/api/v1/auth/google-login")  // Đường dẫn cho OAuth2 login
                //     )
                //     .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService)) // Xử lý thông tin người dùng
                //     .successHandler(customAuthenticationSuccessHandler)  // Xử lý khi đăng nhập thành công
                // )
                                
                                
                .oauth2ResourceServer((oauth2) -> oauth2.jwt(Customizer.withDefaults())
                .authenticationEntryPoint(customAuthenticationEntryPoint))
                
                
                    
                .formLogin(f -> f.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

//     @Bean
// public SecurityFilterChain filterChain(HttpSecurity http,
//                                        CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
//                                        CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler) throws Exception {
//     http
//         .csrf(csrf -> csrf.disable()) // Tắt CSRF
//         .cors(Customizer.withDefaults()) // Cấu hình CORS mặc định
//         .authorizeHttpRequests(authz -> authz
//             // Cho phép các API dưới đây không cần xác thực
//             .requestMatchers("/api/v1/auth/google-login", 
//                              "/api/v1/auth/login", "/api/v1/auth/refresh", 
//                              "/api/v1/auth/register", "/api/v1/auth/verify", 
//                              "/api/v1/auth/resend_otp", "/api/v1/auth/register-info").permitAll()
//             .anyRequest().authenticated() // Các request khác yêu cầu xác thực
//         )
//         // Cấu hình OAuth2 login cho đường dẫn /api/v1/auth/google-login
//         .oauth2Login(oauth2 -> oauth2
//             .loginPage("/api/v1/auth/google-login") // Chỉ áp dụng OAuth2 login cho URL này
//             .authorizationEndpoint(authorization -> authorization
//                 .baseUri("/api/v1/auth/google-login")) // Chỉ định đường dẫn OAuth2 login
//             .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService)) // Lấy thông tin người dùng
//             .successHandler(customAuthenticationSuccessHandler) // Xử lý khi đăng nhập thành công
//         )
//         // Xử lý lỗi xác thực
//         .exceptionHandling(exceptions -> exceptions
//             .authenticationEntryPoint(customAuthenticationEntryPoint))
//         // Cấu hình JWT cho xác thực resource server
//         .oauth2ResourceServer(oauth2 -> oauth2
//             .jwt(Customizer.withDefaults())
//             .authenticationEntryPoint(customAuthenticationEntryPoint))
//         // Quản lý session dưới dạng Stateless
//         .sessionManagement(session -> session
//             .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

//     return http.build();
// }

    

    //Create signature 
    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(getSecretKey()));
    }

    private SecretKey getSecretKey() {
        byte[] keyBytes = Base64.from(jwtKey).decode();
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, SecurityUtil.JWT_ALGORITHM.getName());
    }


    //Decode jwt from request's client which has jwt in its header
    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(
                getSecretKey()).macAlgorithm(SecurityUtil.JWT_ALGORITHM).build();
        return token -> {
            try {
                return jwtDecoder.decode(token);
            } catch (Exception e) {
                System.out.println(">>> JWT error: " + e.getMessage());
                throw e;
            }
        };
    }

}

