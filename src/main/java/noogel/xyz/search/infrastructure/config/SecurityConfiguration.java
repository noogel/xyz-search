package noogel.xyz.search.infrastructure.config;

import noogel.xyz.search.infrastructure.utils.EnvHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Objects;

@Configuration
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        String authEnv = EnvHelper.FuncEnv.AUTH.getEnv();
        if (Objects.equals("true", authEnv)) {
            auth(http);
        } else {
            http.authorizeHttpRequests((auth) -> auth.anyRequest().permitAll());
        }
        return http.build();
    }

    private void auth(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests((auth) -> auth.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService(PasswordEncoder passwordEncoder, ConfigProperties configProperties) {
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
