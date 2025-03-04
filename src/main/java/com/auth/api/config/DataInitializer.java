package com.auth.api.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.auth.api.model.ERole;
import com.auth.api.model.Role;
import com.auth.api.repository.RoleRepository;

@Component
public class DataInitializer implements CommandLineRunner {
    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        // Initialisation des rôles s'ils n'existent pas
        if (roleRepository.count() == 0) {
            roleRepository.save(new Role(ERole.ROLE_USER));
            roleRepository.save(new Role(ERole.ROLE_ADMIN));
            System.out.println("Rôles initialisés avec succès.");
        }
    }
}