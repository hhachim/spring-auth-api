package com.auth.api.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auth.api.exception.ResourceNotFoundException;
import com.auth.api.model.User;
import com.auth.api.repository.UserRepository;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

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
     * @throws UsernameNotFoundException si l'utilisateur n'est pas trouvé
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
}