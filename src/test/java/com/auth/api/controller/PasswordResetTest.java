package com.auth.api.controller;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Captor;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import org.springframework.transaction.annotation.Transactional;

import com.auth.api.dto.PasswordResetRequest;
import com.auth.api.dto.PasswordResetTokenRequest;
import com.auth.api.model.ERole;
import com.auth.api.model.Role;
import com.auth.api.model.User;
import com.auth.api.repository.RoleRepository;
import com.auth.api.repository.UserRepository;
import com.auth.api.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class PasswordResetTest {

    // Configuration interne pour fournir un mock de EmailService
    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public EmailService emailService() {
            return mock(EmailService.class);
        }
    }

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
    private EmailService emailService;
    
    @Captor
    private ArgumentCaptor<String> tokenCaptor;

    private final String testEmail = "testreset@example.com";
    private final String testUsername = "resetuser";
    private final String testPassword = "password123";
    private final String newPassword = "newpassword456";

    @BeforeEach
    public void setup() {
        // Réinitialiser le mock avant chaque test
        reset(emailService);
        
        // Clean up any existing test user
        userRepository.findByEmail(testEmail).ifPresent(user -> userRepository.delete(user));

        // Ensure roles exist
        if (!roleRepository.findByName(ERole.ROLE_USER).isPresent()) {
            roleRepository.save(new Role(ERole.ROLE_USER));
        }

        // Create a test user
        User user = new User();
        user.setUsername(testUsername);
        user.setEmail(testEmail);
        user.setPassword(passwordEncoder.encode(testPassword));
        
        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName(ERole.ROLE_USER).get();
        roles.add(userRole);
        user.setRoles(roles);
        
        userRepository.save(user);
    }

    @Test
    public void testForgotPassword_ValidEmail_ShouldCreateToken() throws Exception {
        // Create request
        PasswordResetRequest request = new PasswordResetRequest();
        request.setEmail(testEmail);

        // Execute request
        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        // Verify email service was called
        verify(emailService, times(1)).sendPasswordResetEmail(eq(testEmail), anyString());
        
        // Verify token was created
        User user = userRepository.findByEmail(testEmail).get();
        assertNotNull(user.getResetPasswordToken());
        assertNotNull(user.getResetPasswordTokenExpiry());
        assertTrue(user.getResetPasswordTokenExpiry().after(new Date()));
    }

    @Test
    public void testForgotPassword_InvalidEmail_ShouldStillReturnOk() throws Exception {
        // Create request with non-existent email
        PasswordResetRequest request = new PasswordResetRequest();
        request.setEmail("nonexistent@example.com");

        // Execute request
        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        // Verify email service was not called with actual email
        verify(emailService, never()).sendPasswordResetEmail(eq("nonexistent@example.com"), anyString());
    }

    @Test
    public void testResetPassword_ValidToken_ShouldResetPassword() throws Exception {
        // Set up a reset token
        String token = UUID.randomUUID().toString();
        User user = userRepository.findByEmail(testEmail).get();
        user.setResetPasswordToken(token);
        user.setResetPasswordTokenExpiry(Date.from(Instant.now().plus(24, ChronoUnit.HOURS)));
        userRepository.save(user);

        // Create reset request
        PasswordResetTokenRequest request = new PasswordResetTokenRequest();
        request.setToken(token);
        request.setNewPassword(newPassword);

        // Execute request
        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Mot de passe réinitialisé avec succès!"));

        // Verify email confirmation was sent
        verify(emailService, times(1)).sendPasswordResetConfirmationEmail(testEmail);

        // Verify password was updated and token cleared
        user = userRepository.findByEmail(testEmail).get();
        assertNull(user.getResetPasswordToken());
        assertNull(user.getResetPasswordTokenExpiry());
        assertTrue(passwordEncoder.matches(newPassword, user.getPassword()));
    }

    @Test
    public void testResetPassword_InvalidToken_ShouldReturnBadRequest() throws Exception {
        // Create reset request with invalid token
        PasswordResetTokenRequest request = new PasswordResetTokenRequest();
        request.setToken("invalid-token");
        request.setNewPassword(newPassword);

        // Execute request
        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify email confirmation was not sent
        verify(emailService, never()).sendPasswordResetConfirmationEmail(anyString());
    }

    @Test
    public void testResetPassword_ExpiredToken_ShouldReturnBadRequest() throws Exception {
        // Set up an expired reset token
        String token = UUID.randomUUID().toString();
        User user = userRepository.findByEmail(testEmail).get();
        user.setResetPasswordToken(token);
        user.setResetPasswordTokenExpiry(Date.from(Instant.now().minus(1, ChronoUnit.DAYS))); // 1 day ago
        userRepository.save(user);

        // Create reset request
        PasswordResetTokenRequest request = new PasswordResetTokenRequest();
        request.setToken(token);
        request.setNewPassword(newPassword);

        // Execute request
        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify email confirmation was not sent
        verify(emailService, never()).sendPasswordResetConfirmationEmail(anyString());
    }

    @Test
    public void testResetPasswordForm_ValidToken_ShouldShowForm() throws Exception {
        // Set up a reset token
        String token = UUID.randomUUID().toString();
        User user = userRepository.findByEmail(testEmail).get();
        user.setResetPasswordToken(token);
        user.setResetPasswordTokenExpiry(Date.from(Instant.now().plus(24, ChronoUnit.HOURS)));
        userRepository.save(user);

        // Execute request
        mockMvc.perform(get("/reset-password")
                .param("token", token))
                .andExpect(status().isOk())
                .andExpect(view().name("reset-password-form"))
                .andExpect(model().attribute("token", token));
    }

    @Test
    public void testResetPasswordForm_InvalidToken_ShouldShowError() throws Exception {
        // Execute request with invalid token
        mockMvc.perform(get("/reset-password")
                .param("token", "invalid-token"))
                .andExpect(status().isOk())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));
    }
}