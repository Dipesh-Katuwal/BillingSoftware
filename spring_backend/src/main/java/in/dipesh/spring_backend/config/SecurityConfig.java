package in.dipesh.spring_backend.config;

import in.dipesh.spring_backend.filter.JwtRequestFilter;
import in.dipesh.spring_backend.service.Impl.AppUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.CorsFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration  //Hey, this isn't a regular class; it's a file where I will define settings and create specific objects (Beans).
@EnableWebSecurity
@RequiredArgsConstructor//Tells Spring, Turn on the security system. I'm about to give you the rules.
public class SecurityConfig {

    private final AppUserDetailsService appUserDetailsService;
    private final JwtRequestFilter jwtRequestFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception { // It tells Spring Security exactly how to handle every single incoming HTTP request
        http.cors(cors -> cors.configurationSource(corsConfigurationSource())) // Connects the CORS rules above. Hey, remember those CORS rules we wrote in the previous class (SecurityConfig)? Apply them here.
                .csrf(csrf -> csrf.disable()) // Disables CSRF for APIs , no need while using JWT tokens
                    .authorizeHttpRequests(auth -> auth.requestMatchers("/login","/encode").permitAll() // Allow login without JWT
                            .requestMatchers("/category", "/items").hasAnyRole("ADMIN","USER")
                            .requestMatchers("/admin/**").hasRole("ADMIN")
                            .anyRequest().authenticated()) // Everything else needs JWT
                            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                            // Tells Spring: "Do not create an HTTP Session. Do not remember this user between requests."
                            // This is the whole point of JWT. Traditional Spring apps create a session ID and store it in a cookie. With JWT, every single request must carry its own ID card (the token). The server shouldn't have to remember anything.
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean   //Take the object this method returns and manage it globally
    public CorsFilter corsFilter(){ //This is the actual gatekeeper that intercepts requests.
        return new CorsFilter(corsConfigurationSource());
    }

    private UrlBasedCorsConfigurationSource corsConfigurationSource() {  //writing specific cors rules
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("http://localhost:5173"); // Only allow requests coming from your React app.
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")); // Allows these specific HTTP actions.
        config.setAllowedHeaders(List.of("Authorization", "Content-Type")); // Tells the browser which headers React is allowed to send to the server.
        config.setAllowCredentials(true); // Allows cookies, authorization headers, or TLS client certificates to be sent.

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**",config);
        return source;
        //It takes all those rules (config) and applies them to every single URL in your app ("/**").
        // Then it returns the final rulebook to the @Bean method above.
    }
    @Bean
    public AuthenticationManager authenticationManager(){ //This configuration is the actual login engine of Spring Security.
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(appUserDetailsService); //This is the specific type of provider that knows how to authenticate a user by checking a database (DAO = Data Access Object).Use appUserDetailsService to fetch their info from the database.
        authProvider.setPasswordEncoder(passwordEncoder()); //It tells the provider: "When the user types their password, use this encoder to check if it matches the hashed password we got from the database."
        return new ProviderManager(authProvider);
        //What it does: It wraps the provider in a ProviderManager and returns it as the AuthenticationManager bean.
        //Why: The AuthenticationManager is the main interface Spring uses to trigger a login. By returning it as a @Bean, you can inject it into your AuthController or AuthService later.
    }
}
