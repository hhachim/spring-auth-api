package com.auth.api.service;

import java.util.List;
import java.util.UUID;
import java.util.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auth.api.exception.ResourceNotFoundException;
import com.auth.api.exception.InvalidResetTokenException;
import com.auth.api.model.User;
import com.auth.api.repository.UserRepository;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Récupère tous les utilisateurs
     * 
     * @return Liste de tous les utilisateurs
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Récupère un utilisateur par son ID
     * 
     * @param id Identifiant de l'utilisateur
     * @return L'utilisateur trouvé
     * @throws ResourceNotFoundException si l'utilisateur n'est pas trouvé
     */
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'id: " + id));
    }

    /**
     * Récupère un utilisateur par son nom d'utilisateur
     * 
     * @param username Nom d'utilisateur
     * @return L'utilisateur trouvé
     * @throws UsernameNotFoundException si l'utilisateur n'est pas trouvé
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé avec le nom d'utilisateur: " + username));
    }

    /**
     * Supprime un utilisateur par son ID
     * 
     * @param id Identifiant de l'utilisateur à supprimer
     */
    @Transactional
    public void deleteUser(Long id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }

    /**
     * Vérifie si un nom d'utilisateur existe déjà
     * 
     * @param username Nom d'utilisateur à vérifier
     * @return true si le nom d'utilisateur existe, false sinon
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Vérifie si un email existe déjà
     * 
     * @param email Email à vérifier
     * @return true si l'email existe, false sinon
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Sauvegarde un utilisateur (création ou mise à jour)
     * 
     * @param user L'utilisateur à sauvegarder
     * @return L'utilisateur sauvegardé
     */
    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }
    
    /**
     * Crée un token de réinitialisation de mot de passe pour un utilisateur
     * 
     * @param email Email de l'utilisateur
     * @return Le token de réinitialisation
     * @throws ResourceNotFoundException si l'email n'est pas trouvé
     */
    @Transactional
    public String createPasswordResetTokenForUser(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        
        if (!userOptional.isPresent()) {
            throw new ResourceNotFoundException("Aucun utilisateur trouvé avec l'email: " + email);
        }
        
        User user = userOptional.get();
        String token = UUID.randomUUID().toString();
        
        // Définir l'expiration du token à 24 heures à partir de maintenant
        Date expiryDate = Date.from(Instant.now().plus(24, ChronoUnit.HOURS));
        
        user.setResetPasswordToken(token);
        user.setResetPasswordTokenExpiry(expiryDate);
        userRepository.save(user);
        
        return token;
    }
    
    /**
     * Valide un token de réinitialisation de mot de passe et change le mot de passe
     * 
     * @param token Token de réinitialisation
     * @param newPassword Nouveau mot de passe
     * @throws InvalidResetTokenException si le token est invalide ou expiré
     */
    @Transactional
    public void resetPasswordWithToken(String token, String newPassword) {
        User user = userRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new InvalidResetTokenException("Token de réinitialisation invalide"));
        
        // Vérifier si le token n'a pas expiré
        if (user.getResetPasswordTokenExpiry().before(new Date())) {
            throw new InvalidResetTokenException("Token de réinitialisation expiré");
        }
        
        // Mettre à jour le mot de passe et réinitialiser le token
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        
        userRepository.save(user);
    }
}