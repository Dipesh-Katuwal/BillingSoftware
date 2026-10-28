package in.dipesh.spring_backend.service.Impl;

import in.dipesh.spring_backend.entity.UserEntity;
import in.dipesh.spring_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository repository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity existingUser = repository.findByEmail(email).orElseThrow(()->new UsernameNotFoundException("User not found for email : "+email));
        return new User(existingUser.getEmail(),existingUser.getPassword(), Collections.singleton(new SimpleGrantedAuthority("ROLE_" + existingUser.getRole())));
    }
    // Here is the user's email, here is their hashed password, and here is a list containing their single role. Log them in.

}


/*
import org.springframework.security.core.userdetails.UserDetailsService;
This is a core interface provided by Spring Security.

Think of it as Spring Security's way of asking your application: "Hey, I have a username.
Can you go to your database, find this user, and give me their details (password, roles, etc.) so I can verify them?"
It has exactly one method that you must implement:

UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
Why do you need it?
When you build your JWT filter, you will extract the username from the token.
 To actually log the user in (so Spring knows they are authenticated), you need to fetch their details.
  Spring Security needs a UserDetails object, not your custom UserEntity. You will write a class (e.g., CustomUserDetailsService)
  that implements this interface, converts your UserEntity into a Spring UserDetails object, and returns it.
*/