package fit.iuh.kh3tshopbe.configuration;

import com.nimbusds.jose.JWSAlgorithm;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter; // Đảm bảo import này tồn tại

import javax.crypto.spec.SecretKeySpec;
import java.util.List;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, fit.iuh.kh3tshopbe.filter.RateLimitingFilter rateLimitingFilter) throws Exception {
    http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(request -> request
                    // 1. CHO PHÉP GIAO DIỆN (FRONTEND) - THÊM MỚI (Cho phép cả các client-side routes của React Router để khi reload F5 không bị chặn)
                    .requestMatchers("/", "/index.html", "/static/**", "/assets/**", "/*.js", "/*.css", "/*.png", "/*.ico",
                            "/product", "/product/**", "/about", "/policy", "/compare", "/wishlists", "/wishlists/**",
                            "/cart", "/checkout", "/payment", "/profile", "/login", "/register", "/forget_password", "/reset_password",
                            "/admin", "/admin/**", "/staff/**").permitAll()
                    
                    // 2. CÁC API KHÔNG CẦN LOGIN
                    .requestMatchers(HttpMethod.POST, "/api/v1/payment/sepay-callback").permitAll()
                    .requestMatchers(HttpMethod.POST, "/accounts").permitAll()
                    .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/introspect", "/auth/forgot-password", "/auth/reset-password").permitAll()
                    
                    // 3. CÁC API LẤY DỮ LIỆU (Đã sửa thêm dấu / vào trước cart-details để hết cảnh báo log)
                    .requestMatchers(HttpMethod.GET, "/accounts/username/*", "/products", "/products/**", "/categories", "/categories/**", 
                                    "/cart-details/**", "/cart-details/cart/**", "/carts/", "/carts/**", "/addresses/", "/addresses/**", 
                                    "/sizes", "/orders", "/orders/**", "/invoices", "/invoices/**", "/customers", "/customers/**").permitAll()
                    
                    .requestMatchers(HttpMethod.POST, "/chat/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/chat/**").permitAll()
                    .requestMatchers("/admin-chat/**").hasRole("ADMIN")
                    
                    // TẤT CẢ CÁC REQUEST KHÁC MỚI CẦN LOGIN
                    .anyRequest().authenticated()
            )
            .oauth2ResourceServer(auth -> auth
                    .jwt(jwt -> jwt
                            .decoder(jwtDecoder())
                            .jwtAuthenticationConverter(jwtAuthenticationConverter())
                    )
                    .authenticationEntryPoint(new AuthenticationEntryPoint())
                )
                .addFilterAfter(rateLimitingFilter, BearerTokenAuthenticationFilter.class);

    return http.build();
}
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = corsConfigurationSource();
        return new CorsFilter(source);
    }

    // Giữ nguyên CorsConfigurationSource của bạn
    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173")); 
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
        converter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(converter);
        return authenticationConverter;
    }

    private static final String SIGNER_KEY =
            "c8e09fddda9e192d16c485affabc61c9f4bca77a60c19d448f3a6e8475b9f0a4e0d1f69bca8d21f1123b8f0f8a0b8d12";

    @Bean
    JwtDecoder jwtDecoder() {
        SecretKeySpec keySpec = new SecretKeySpec(SIGNER_KEY.getBytes(), "HS512");
        return NimbusJwtDecoder
                .withSecretKey(keySpec)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}