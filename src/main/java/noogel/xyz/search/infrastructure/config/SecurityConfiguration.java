package noogel.xyz.search.infrastructure.config;

import java.util.Objects;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import noogel.xyz.search.infrastructure.utils.EnvHelper;

@Configuration
public class SecurityConfiguration {
    // 配置 Basic 认证（API 接口）
    // 高优先级：处理 /opds/** 和 /opds
    @Bean
    @Order(1) // 数值越小，优先级越高
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        String authEnv = EnvHelper.FuncEnv.AUTH.getEnv();
        if (Objects.equals("true", authEnv)) {
            http.securityMatcher("/opds/**", "/opds")
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .httpBasic(httpBasic -> {
                    });
        } else {
            http.securityMatcher("/opds/**", "/opds")
                    .authorizeHttpRequests((auth) -> auth.anyRequest().permitAll());
        }
        return http.build();
    }

    @Bean
    @Order(2) // 数值越大，优先级越低
    public SecurityFilterChain pageFilterChain(HttpSecurity http) throws Exception {
        String authEnv = EnvHelper.FuncEnv.AUTH.getEnv();
        if (Objects.equals("true", authEnv)) {
            http.securityMatcher("/**")
                    .formLogin(AbstractAuthenticationFilterConfigurer::permitAll)
                    .logout(LogoutConfigurer::permitAll)
                    .authorizeHttpRequests((auth) -> auth.anyRequest().authenticated());
        } else {
            http.securityMatcher("/**")
                    .authorizeHttpRequests((auth) -> auth.anyRequest().permitAll());
        }
        return http.build();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService(PasswordEncoder passwordEncoder,
            ConfigProperties configProperties) {
        UserDetails user = User.withUsername(configProperties.getBase().getUsername())
                .password(passwordEncoder.encode(configProperties.getBase().getPassword()))
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

}
