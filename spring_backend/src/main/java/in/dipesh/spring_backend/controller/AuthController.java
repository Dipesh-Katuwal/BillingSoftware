package in.dipesh.spring_backend.controller;

import in.dipesh.spring_backend.Utils.JwtUtils;
import in.dipesh.spring_backend.dto.AuthRequest;
import in.dipesh.spring_backend.dto.AuthResponse;
import in.dipesh.spring_backend.service.Impl.AppUserDetailsService;
import in.dipesh.spring_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final AppUserDetailsService appUserDetailsService;
    private final JwtUtils jwtUtils;
    private final UserService userService;

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) throws Exception{
        authenticate(request.getEmail(),request.getPassword());
        final UserDetails userDetails = appUserDetailsService.loadUserByUsername(request.getEmail());
        final String jwtToken = jwtUtils.generateToken(userDetails);
        String role = userService.getUserRole(request.getEmail());
        return new AuthResponse(request.getEmail(),jwtToken,role);
    }

    private void authenticate(String email, String password) throws Exception {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email,password));
        } catch (DisabledException e) {
            throw new RuntimeException(e);
        } catch (BadCredentialsException e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/encode") // This is a temporary testing endpoint purely for developmental purpose. Instead of writing a whole Javascript just to generate a hash, we created this endpoint so we can do it instantly in Postman.
    public String encodePassword(@RequestBody Map<String,String> request ){
        return passwordEncoder.encode(request.get("password"));
    }

}
