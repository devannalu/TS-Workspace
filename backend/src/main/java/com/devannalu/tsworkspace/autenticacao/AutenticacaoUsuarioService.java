package com.devannalu.tsworkspace.autenticacao;

import com.devannalu.tsworkspace.auth.AppUserPrincipal;
import com.devannalu.tsworkspace.auth.EmailNormalizer;
import com.devannalu.tsworkspace.usuarios.Perfil;
import com.devannalu.tsworkspace.usuarios.PerfilRepository;
import com.devannalu.tsworkspace.usuarios.Usuario;
import com.devannalu.tsworkspace.usuarios.UsuarioRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AutenticacaoUsuarioService implements UserDetailsService {
    private final UsuarioRepository usuarios;
    private final PerfilRepository perfis;

    public AutenticacaoUsuarioService(UsuarioRepository usuarios, PerfilRepository perfis) {
        this.usuarios = usuarios;
        this.perfis = perfis;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String email;
        try { email = EmailNormalizer.normalize(username); }
        catch (IllegalArgumentException ex) { throw new UsernameNotFoundException("Credenciais inválidas."); }
        Usuario usuario = usuarios.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas."));
        Perfil perfil = perfis.findById(usuario.getId()).orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas."));
        return new AppUserPrincipal(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getHashSenha(), perfil.getCargo(), perfil.getStatus());
    }
}
