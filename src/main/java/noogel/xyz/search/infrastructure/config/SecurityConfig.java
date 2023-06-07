package noogel.xyz.search.infrastructure.config;

import noogel.xyz.search.infrastructure.utils.EnvHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Objects;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        String authEnv = EnvHelper.FuncEnv.AUTH.getEnv();
        if (Objects.equals("true", authEnv)) {
            http.authorizeRequests().anyRequest().authenticated().and().httpBasic();
        } else {
            http.authorizeRequests().anyRequest().permitAll();
        }
//        http.authorizeRequests()
//                .anyRequest().authenticated()
//                .and().formLogin().loginProcessingUrl("/login")
//                .and().logout().permitAll()
//                .and().csrf().disable();
//        http.headers().frameOptions().sameOrigin();
        return http.build();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService(PasswordEncoder passwordEncoder, SearchPropertyConfig.SearchConfig searchConfig) {
        UserDetails user = User.withUsername(searchConfig.getBase().getUsername())
                .password(passwordEncoder.encode(searchConfig.getBase().getPassword()))
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
