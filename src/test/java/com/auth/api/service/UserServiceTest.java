package com.auth.api.service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.auth.api.exception.InvalidResetTokenException;
import com.auth.api.exception.ResourceNotFoundException;
import com.auth.api.model.User;
import com.auth.api.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    public void getAllUsers_ShouldReturnAllUsers() {
        // Arrange
        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("user1");

        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");

        List<User> expectedUsers = Arrays.asList(user1, user2);
        when(userRepository.findAll()).thenReturn(expectedUsers);

        // Act
        List<User> actualUsers = userService.getAllUsers();

        // Assert
        assertEquals(expectedUsers.size(), actualUsers.size());
        assertEquals(expectedUsers, actualUsers);
        verify(userRepository, times(1)).findAll();
    }

    @Test
    public void getUserById_WhenUserExists_ShouldReturnUser() {
        // Arrange
        Long userId = 1L;
        User expectedUser = new User();
        expectedUser.setId(userId);
        expectedUser.setUsername("testuser");

        when(userRepository.findById(userId)).thenReturn(Optional.of(expectedUser));

        // Act
        User actualUser = userService.getUserById(userId);

        // Assert
        assertNotNull(actualUser);
        assertEquals(userId, actualUser.getId());
        assertEquals("testuser", actualUser.getUsername());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    public void getUserById_WhenUserDoesNotExist_ShouldThrowException() {
        // Arrange
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.getUserById(userId);
        });
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    public void getUserByUsername_WhenUserExists_ShouldReturnUser() {
        // Arrange
        String username = "testuser";
        User expectedUser = new User();
        expectedUser.setId(1L);
        expectedUser.setUsername(username);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(expectedUser));

        // Act
        User actualUser = userService.getUserByUsername(username);

        // Assert
        assertNotNull(actualUser);
        assertEquals(username, actualUser.getUsername());
        verify(userRepository, times(1)).findByUsername(username);
    }

    @Test
    public void getUserByUsername_WhenUserDoesNotExist_ShouldThrowException() {
        // Arrange
        String username = "nonexistentuser";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> {
            userService.getUserByUsername(username);
        });
        verify(userRepository, times(1)).findByUsername(username);
    }

    @Test
    public void deleteUser_WhenUserExists_ShouldDeleteUser() {
        // Arrange
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        doNothing().when(userRepository).delete(user);

        // Act
        userService.deleteUser(userId);

        // Assert
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).delete(user);
    }

    @Test
    public void save_ShouldReturnSavedUser() {
        // Arrange
        User userToSave = new User();
        userToSave.setUsername("newuser");
        userToSave.setEmail("newuser@example.com");
        
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("newuser");
        savedUser.setEmail("newuser@example.com");
        
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        User result = userService.save(userToSave);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("newuser", result.getUsername());
        verify(userRepository, times(1)).save(userToSave);
    }
    
    @Test
    public void createPasswordResetTokenForUser_WhenUserExists_ShouldReturnToken() {
        // Arrange
        String email = "test@example.com";
        User user = new User();
        user.setEmail(email);
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        // Act
        String token = userService.createPasswordResetTokenForUser(email);
        
        // Assert
        assertNotNull(token);
        assertNotNull(user.getResetPasswordToken());
        assertNotNull(user.getResetPasswordTokenExpiry());
        verify(userRepository, times(1)).findByEmail(email);
        verify(userRepository, times(1)).save(user);
    }
    
    @Test
    public void createPasswordResetTokenForUser_WhenUserDoesNotExist_ShouldThrowException() {
        // Arrange
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.createPasswordResetTokenForUser(email);
        });
        verify(userRepository, times(1)).findByEmail(email);
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    public void resetPasswordWithToken_WhenTokenIsValid_ShouldResetPassword() {
        // Arrange
        String token = "valid-token";
        String newPassword = "newPassword";
        String encodedPassword = "encodedPassword";
        
        User user = new User();
        user.setResetPasswordToken(token);
        // Set expiry date to future
        user.setResetPasswordTokenExpiry(new Date(System.currentTimeMillis() + 86400000)); // +24h
        
        when(userRepository.findByResetPasswordToken(token)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        // Act
        userService.resetPasswordWithToken(token, newPassword);
        
        // Assert
        assertEquals(encodedPassword, user.getPassword());
        assertNull(user.getResetPasswordToken());
        assertNull(user.getResetPasswordTokenExpiry());
        verify(userRepository, times(1)).findByResetPasswordToken(token);
        verify(passwordEncoder, times(1)).encode(newPassword);
        verify(userRepository, times(1)).save(user);
    }
    
    @Test
    public void resetPasswordWithToken_WhenTokenIsExpired_ShouldThrowException() {
        // Arrange
        String token = "expired-token";
        String newPassword = "newPassword";
        
        User user = new User();
        user.setResetPasswordToken(token);
        // Set expiry date to past
        user.setResetPasswordTokenExpiry(new Date(System.currentTimeMillis() - 86400000)); // -24h
        
        when(userRepository.findByResetPasswordToken(token)).thenReturn(Optional.of(user));
        
        // Act & Assert
        assertThrows(InvalidResetTokenException.class, () -> {
            userService.resetPasswordWithToken(token, newPassword);
        });
        verify(userRepository, times(1)).findByResetPasswordToken(token);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    public void resetPasswordWithToken_WhenTokenDoesNotExist_ShouldThrowException() {
        // Arrange
        String token = "nonexistent-token";
        String newPassword = "newPassword";
        
        when(userRepository.findByResetPasswordToken(token)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(InvalidResetTokenException.class, () -> {
            userService.resetPasswordWithToken(token, newPassword);
        });
        verify(userRepository, times(1)).findByResetPasswordToken(token);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    public void existsByUsername_WhenUsernameExists_ShouldReturnTrue() {
        // Arrange
        String username = "existinguser";
        when(userRepository.existsByUsername(username)).thenReturn(true);
        
        // Act
        boolean result = userService.existsByUsername(username);
        
        // Assert
        assertTrue(result);
        verify(userRepository, times(1)).existsByUsername(username);
    }
    
    @Test
    public void existsByEmail_WhenEmailExists_ShouldReturnTrue() {
        // Arrange
        String email = "existing@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(true);
        
        // Act
        boolean result = userService.existsByEmail(email);
        
        // Assert
        assertTrue(result);
        verify(userRepository, times(1)).existsByEmail(email);
    }
}