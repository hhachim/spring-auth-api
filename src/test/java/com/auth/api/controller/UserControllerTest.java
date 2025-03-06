package com.auth.api.controller;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.auth.api.model.ERole;
import com.auth.api.model.Role;
import com.auth.api.model.User;
import com.auth.api.repository.RoleRepository;
import com.auth.api.repository.UserRepository;
import com.auth.api.security.jwt.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    private final String adminUsername = "admin";
    private final String adminEmail = "admin@example.com";
    private final String adminPassword = "admin123";

    private final String userUsername = "user";
    private final String userEmail = "user@example.com";
    private final String userPassword = "user123";

    private String adminToken;
    private String userToken;
    private Long adminId;
    private Long userId;

    @BeforeEach
    public void setup() {
        // Clean existing test users
        userRepository.findByUsername(adminUsername).ifPresent(user -> userRepository.delete(user));
        userRepository.findByUsername(userUsername).ifPresent(user -> userRepository.delete(user));

        // Ensure roles exist
        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(ERole.ROLE_ADMIN)));
        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseGet(() -> roleRepository.save(new Role(ERole.ROLE_USER)));

        // Create admin user
        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        Set<Role> adminRoles = new HashSet<>();
        adminRoles.add(adminRole);
        adminRoles.add(userRole);
        admin.setRoles(adminRoles);
        admin = userRepository.save(admin);
        adminId = admin.getId();

        // Create regular user
        User user = new User();
        user.setUsername(userUsername);
        user.setEmail(userEmail);
        user.setPassword(passwordEncoder.encode(userPassword));
        Set<Role> userRoles = new HashSet<>();
        userRoles.add(userRole);
        user.setRoles(userRoles);
        user = userRepository.save(user);
        userId = user.getId();

        // Generate JWT tokens
        adminToken = getToken(adminUsername, adminPassword);
        userToken = getToken(userUsername, userPassword);
    }

    private String getToken(String username, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return jwtUtils.generateJwtToken(authentication);
    }

    @Test
    public void testGetAllUsers_AsAdmin_ShouldReturnAllUsers() throws Exception {
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[*].username", hasItems(adminUsername, userUsername)));
    }

    @Test
    public void testGetAllUsers_AsUser_ShouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetUserById_AsAdmin_ShouldReturnUser() throws Exception {
        mockMvc.perform(get("/api/users/" + userId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.username").value(userUsername))
                .andExpect(jsonPath("$.email").value(userEmail));
    }

    @Test
    public void testGetUserById_AsUser_OwnProfile_ShouldReturnUser() throws Exception {
        mockMvc.perform(get("/api/users/" + userId)
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.username").value(userUsername))
                .andExpect(jsonPath("$.email").value(userEmail));
    }

    @Test
    public void testGetCurrentUser_ShouldReturnCurrentUser() throws Exception {
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(userUsername))
                .andExpect(jsonPath("$.email").value(userEmail));
    }

    @Test
    public void testDeleteUser_AsAdmin_ShouldDeleteUser() throws Exception {
        // Create a user to delete
        User userToDelete = new User();
        userToDelete.setUsername("deleteme");
        userToDelete.setEmail("delete@example.com");
        userToDelete.setPassword(passwordEncoder.encode("delete123"));
        Set<Role> roles = new HashSet<>();
        roles.add(roleRepository.findByName(ERole.ROLE_USER).get());
        userToDelete.setRoles(roles);
        userToDelete = userRepository.save(userToDelete);
        Long deleteId = userToDelete.getId();

        // Delete the user as admin
        mockMvc.perform(delete("/api/users/" + deleteId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Verify user is deleted
        mockMvc.perform(get("/api/users/" + deleteId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteUser_AsUser_ShouldBeForbidden() throws Exception {
        mockMvc.perform(delete("/api/users/" + adminId)
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetUserById_NonExistentUser_ShouldReturnNotFound() throws Exception {
        Long nonExistentId = 999999L;
        mockMvc.perform(get("/api/users/" + nonExistentId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}