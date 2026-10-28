package in.dipesh.spring_backend.Utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component // Tells Spring to manage this class as a Bean
public class JwtUtils {

    // 1. THE SECRET KEY
    // This key is used to sign the token. It must be at least 256 bits (32+ characters) long for HS256.
    @Value("${jwt.secret.key}")
    private String secretkey;

    // 2. GENERATE TOKEN
    // Takes the user details and creates a JWT string.
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();

        // Safe extraction of the role
        String role = "USER"; // Default fallback
        if (userDetails.getAuthorities() != null && !userDetails.getAuthorities().isEmpty()) {
            role = userDetails.getAuthorities().iterator().next().getAuthority();
        }
        claims.put("role", role);  // Add the role into the token

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername()) // Usually the email
                .setIssuedAt(new Date(System.currentTimeMillis())) // Current time
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // Expires in 10 hours
                .signWith(getSignInKey(), SignatureAlgorithm.HS256) // Sign it with the secret key
                .compact(); // Build the string
    }

    // 3. VALIDATE TOKEN
    // Checks if the token belongs to the user and hasn't expired
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    // 4. EXTRACT USERNAME
    // Reads the "subject" (email) from inside the token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // --- HELPER METHODS BELOW ---

    // Checks if the expiration date is before the current date
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // A generic method to extract any specific piece of data from the token
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // The core method that actually decodes the token using the Secret Key
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Converts the String SECRET_KEY into a cryptographic Key object
    private Key getSignInKey() {
        byte[] keyBytes = secretkey.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
