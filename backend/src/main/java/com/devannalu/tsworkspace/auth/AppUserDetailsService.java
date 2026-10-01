package com.devannalu.tsworkspace.auth;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {
    private final UserRepository users;
    private final ProfileRepository profiles;

    public AppUserDetailsService(UserRepository users, ProfileRepository profiles) {
        this.users = users;
        this.profiles = profiles;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String email;
        try { email = EmailNormalizer.normalize(username); }
        catch (IllegalArgumentException ex) { throw new UsernameNotFoundException("Credenciais inválidas."); }
        User user = users.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas."));
        Profile profile = profiles.findById(user.getId()).orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas."));
        return new AppUserPrincipal(user.getId(), user.getName(), user.getEmail(), user.getPasswordHash(), profile.getJobTitle(), profile.getStatus());
    }
}
