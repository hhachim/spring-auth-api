package com.auth.api.controller;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.auth.api.dto.JwtResponse;
import com.auth.api.dto.LoginRequest;
import com.auth.api.dto.MessageResponse;
import com.auth.api.dto.SignupRequest;
import com.auth.api.model.ERole;
import com.auth.api.model.Role;
import com.auth.api.model.User;
import com.auth.api.repository.RoleRepository;
import com.auth.api.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AuthControllerTest {

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

    private final String testUsername = "testuser";
    private final String testEmail = "test@example.com";
    private final String testPassword = "password123";

    @BeforeEach
    public void setup() {
        // Clean up any existing test user
        userRepository.findByUsername(testUsername).ifPresent(user -> userRepository.delete(user));

        // Ensure roles exist
        if (!roleRepository.findByName(ERole.ROLE_USER).isPresent()) {
            roleRepository.save(new Role(ERole.ROLE_USER));
        }
        if (!roleRepository.findByName(ERole.ROLE_ADMIN).isPresent()) {
            roleRepository.save(new Role(ERole.ROLE_ADMIN));
        }
    }

    @Test
    public void testSignup_Success() throws Exception {
        // Create signup request
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername(testUsername);
        signupRequest.setEmail(testEmail);
        signupRequest.setPassword(testPassword);
        Set<String> roles = new HashSet<>();
        roles.add("user");
        signupRequest.setRoles(roles);

        // Perform signup request
        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Verify response
        String contentAsString = result.getResponse().getContentAsString();
        MessageResponse response = objectMapper.readValue(contentAsString, MessageResponse.class);
        assertEquals("Utilisateur enregistré avec succès!", response.getMessage());

        // Verify user was created in database
        User user = userRepository.findByUsername(testUsername).orElse(null);
        assertNotNull(user);
        assertEquals(testEmail, user.getEmail());
        assertTrue(user.getRoles().stream().anyMatch(role -> role.getName() == ERole.ROLE_USER));
    }

    @Test
    public void testSignup_UsernameTaken() throws Exception {
        // Create a user in the database with the test username
        User existingUser = new User();
        existingUser.setUsername(testUsername);
        existingUser.setEmail("another@example.com");
        existingUser.setPassword(passwordEncoder.encode(testPassword));
        
        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName(ERole.ROLE_USER).get();
        roles.add(userRole);
        existingUser.setRoles(roles);
        
        userRepository.save(existingUser);

        // Create signup request with the same username
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername(testUsername);
        signupRequest.setEmail(testEmail);
        signupRequest.setPassword(testPassword);
        Set<String> roleSet = new HashSet<>();
        roleSet.add("user");
        signupRequest.setRoles(roleSet);

        // Perform signup request, expect bad request
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSignin_Success() throws Exception {
        // Create a user in the database
        User user = new User();
        user.setUsername(testUsername);
        user.setEmail(testEmail);
        user.setPassword(passwordEncoder.encode(testPassword));
        
        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName(ERole.ROLE_USER).get();
        roles.add(userRole);
        user.setRoles(roles);
        
        userRepository.save(user);

        // Create login request
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(testUsername);
        loginRequest.setPassword(testPassword);

        // Perform login request
        MvcResult result = mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Verify response
        String contentAsString = result.getResponse().getContentAsString();
        JwtResponse response = objectMapper.readValue(contentAsString, JwtResponse.class);
        
        assertNotNull(response.getToken());
        assertEquals(testUsername, response.getUsername());
        assertEquals(testEmail, response.getEmail());
        assertTrue(response.getRoles().contains("ROLE_USER"));
    }

    @Test
    public void testSignin_InvalidCredentials() throws Exception {
        // Create a user in the database
        User user = new User();
        user.setUsername(testUsername);
        user.setEmail(testEmail);
        user.setPassword(passwordEncoder.encode(testPassword));
        
        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName(ERole.ROLE_USER).get();
        roles.add(userRole);
        user.setRoles(roles);
        
        userRepository.save(user);

        // Create login request with wrong password
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(testUsername);
        loginRequest.setPassword("wrongpassword");

        // Perform login request, expect unauthorized
        mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }
}