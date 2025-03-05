package com.auth.api.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auth.api.dto.JwtResponse;
import com.auth.api.dto.LoginRequest;
import com.auth.api.dto.MessageResponse;
import com.auth.api.dto.PasswordResetRequest;
import com.auth.api.dto.PasswordResetTokenRequest;
import com.auth.api.dto.SignupRequest;
import com.auth.api.exception.UserAlreadyExistsException;
import com.auth.api.model.ERole;
import com.auth.api.model.Role;
import com.auth.api.model.User;
import com.auth.api.repository.RoleRepository;
import com.auth.api.repository.UserRepository;
import com.auth.api.security.jwt.JwtUtils;
import com.auth.api.security.services.UserDetailsImpl;
import com.auth.api.service.EmailService;
import com.auth.api.service.UserService;

import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserService userService;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;
    
    @Autowired
    EmailService emailService;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(
                jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userService.existsByUsername(signUpRequest.getUsername())) {
            throw new UserAlreadyExistsException("Erreur: Nom d'utilisateur déjà utilisé!");
        }

        if (userService.existsByEmail(signUpRequest.getEmail())) {
            throw new UserAlreadyExistsException("Erreur: Email déjà utilisé!");
        }

        // Création du nouveau compte utilisateur
        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));

        Set<String> strRoles = signUpRequest.getRoles();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null) {
            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Erreur: Role utilisateur non trouvé."));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin":
                        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("Erreur: Role admin non trouvé."));
                        roles.add(adminRole);
                        break;
                    default:
                        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                                .orElseThrow(() -> new RuntimeException("Erreur: Role utilisateur non trouvé."));
                        roles.add(userRole);
                }
            });
        }

        user.setRoles(roles);
        userService.save(user);

        return ResponseEntity.ok(new MessageResponse("Utilisateur enregistré avec succès!"));
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String headerAuth) {
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            String jwt = headerAuth.substring(7);
            if (jwtUtils.validateJwtToken(jwt)) {
                String username = jwtUtils.getUserNameFromJwtToken(jwt);
                User user = userService.getUserByUsername(username);
                
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String newJwt = jwtUtils.generateJwtToken(authentication);
                
                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                List<String> roles = userDetails.getAuthorities().stream()
                        .map(item -> item.getAuthority())
                        .collect(Collectors.toList());
                
                return ResponseEntity.ok(new JwtResponse(
                        newJwt,
                        userDetails.getId(),
                        userDetails.getUsername(),
                        userDetails.getEmail(),
                        roles));
            }
        }
        
        return ResponseEntity.badRequest().body(new MessageResponse("Erreur: Token invalide ou expiré!"));
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<?> requestPasswordReset(@Valid @RequestBody PasswordResetRequest resetRequest) {
        try {
            String token = userService.createPasswordResetTokenForUser(resetRequest.getEmail());
            
            // Envoi de l'email avec le token de réinitialisation
            emailService.sendPasswordResetEmail(resetRequest.getEmail(), token);
            
            return ResponseEntity.ok(new MessageResponse("Un e-mail de réinitialisation de mot de passe a été envoyé à l'adresse fournie."));
        } catch (Exception e) {
            // Ne pas révéler si l'e-mail existe ou non pour des raisons de sécurité
            return ResponseEntity.ok(new MessageResponse("Si l'e-mail existe dans notre système, un lien de réinitialisation sera envoyé."));
        }
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody PasswordResetTokenRequest resetRequest) {
        try {
            // Récupérer l'utilisateur par token avant de le valider et de réinitialiser le mot de passe
            User user = userRepository.findByResetPasswordToken(resetRequest.getToken())
                    .orElseThrow(() -> new RuntimeException("Token invalide"));
            
            userService.resetPasswordWithToken(resetRequest.getToken(), resetRequest.getNewPassword());
            
            // Envoyer un email de confirmation après la réinitialisation réussie
            emailService.sendPasswordResetConfirmationEmail(user.getEmail());
            
            return ResponseEntity.ok(new MessageResponse("Mot de passe réinitialisé avec succès!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
}