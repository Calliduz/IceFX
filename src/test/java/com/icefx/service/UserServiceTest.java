package com.icefx.service;

import com.icefx.dao.UserDAO;
import com.icefx.model.User;
import com.icefx.model.User.UserRole;
import org.junit.jupiter.api.*;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 */
class UserServiceTest {
    
    @Mock
    private UserDAO userDAO;
    
    private UserService userService;
    private AutoCloseable closeable;
    
    @BeforeEach
    void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        userService = new UserService(userDAO);
    }
    
    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }
    
    @Test
    @DisplayName("Should authenticate valid user successfully")
    void testAuthenticateSuccess() throws SQLException {
        // Arrange
        String password = "password123";
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(10));
        
        User mockUser = new User(
            1, "USER001", "Test User", "IT", "Developer",
            UserRole.STAFF, hashedPassword, LocalDateTime.now(), true
        );
        
        when(userDAO.findByUserCode("USER001")).thenReturn(Optional.of(mockUser));
        
        // Act
        UserService.AuthResult result = userService.authenticate("USER001", password);
        
        // Assert
        assertTrue(result.isSuccess());
        assertEquals(mockUser, result.getUser());
        assertEquals("Authentication successful", result.getMessage());
    }
    
    @Test
    @DisplayName("Should fail authentication with wrong password")
    void testAuthenticateWrongPassword() throws SQLException {
        // Arrange
        String hashedPassword = BCrypt.hashpw("correct", BCrypt.gensalt(10));
        
        User mockUser = new User(
            1, "USER001", "Test User", "IT", "Developer",
            UserRole.STAFF, hashedPassword, LocalDateTime.now(), true
        );
        
        when(userDAO.findByUserCode("USER001")).thenReturn(Optional.of(mockUser));
        
        // Act
        UserService.AuthResult result = userService.authenticate("USER001", "wrong");
        
        // Assert
        assertFalse(result.isSuccess());
        assertNull(result.getUser());
        assertEquals("Invalid user code or password", result.getMessage());
    }
    
    @Test
    @DisplayName("Should fail authentication for non-existent user")
    void testAuthenticateUserNotFound() throws SQLException {
        // Arrange
        when(userDAO.findByUserCode("NOTFOUND")).thenReturn(Optional.empty());
        
        // Act
        UserService.AuthResult result = userService.authenticate("NOTFOUND", "password");
        
        // Assert
        assertFalse(result.isSuccess());
        assertNull(result.getUser());
        assertEquals("Invalid user code or password", result.getMessage());
    }
    
    @Test
    @DisplayName("Should fail authentication for inactive user")
    void testAuthenticateInactiveUser() throws SQLException {
        // Arrange
        String password = "password123";
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(10));
        
        User inactiveUser = new User(
            1, "USER001", "Test User", "IT", "Developer",
            UserRole.STAFF, hashedPassword, LocalDateTime.now(), false
        );
        
        when(userDAO.findByUserCode("USER001")).thenReturn(Optional.of(inactiveUser));
        
        // Act
        UserService.AuthResult result = userService.authenticate("USER001", password);
        
        // Assert
        assertFalse(result.isSuccess());
        assertNull(result.getUser());
        assertEquals("User account is inactive", result.getMessage());
    }
    
    @Test
    @DisplayName("Should create user with valid data")
    void testCreateUserSuccess() throws SQLException {
        // Arrange
        when(userDAO.findByUserCode("NEW001")).thenReturn(Optional.empty());
        when(userDAO.createUser(any(User.class))).thenReturn(100);
        
        // Act
        User user = userService.createUser(
            "NEW001", "New User", "HR", "Manager",
            UserRole.STAFF, "password123"
        );
        
        // Assert
        assertNotNull(user);
        assertEquals("NEW001", user.getUserCode());
        assertEquals("New User", user.getFullName());
        assertTrue(user.isActive());
        verify(userDAO).createUser(any(User.class));
    }
    
    @Test
    @DisplayName("Should reject short password")
    void testCreateUserShortPassword() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.createUser(
                "NEW001", "New User", "HR", "Manager",
                UserRole.STAFF, "123"
            )
        );
        
        assertTrue(exception.getMessage().contains("at least 6 characters"));
    }
    
    @Test
    @DisplayName("Should reject empty user code")
    void testCreateUserEmptyCode() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.createUser(
                "", "New User", "HR", "Manager",
                UserRole.STAFF, "password123"
            )
        );
        
        assertTrue(exception.getMessage().contains("cannot be empty"));
    }
    
    @Test
    @DisplayName("Should reject duplicate user code")
    void testCreateUserDuplicateCode() throws SQLException {
        // Arrange
        User existingUser = new User(
            1, "EXISTS", "Existing User", "IT", "Dev",
            UserRole.STAFF, "hash", LocalDateTime.now(), true
        );
        
        when(userDAO.findByUserCode("EXISTS")).thenReturn(Optional.of(existingUser));
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.createUser(
                "EXISTS", "New User", "HR", "Manager",
                UserRole.STAFF, "password123"
            )
        );
        
        assertTrue(exception.getMessage().contains("already exists"));
    }
    
    @Test
    @DisplayName("Should change password with correct old password")
    void testChangePasswordSuccess() throws SQLException {
        // Arrange
        String oldPassword = "oldpass";
        String hashedOld = BCrypt.hashpw(oldPassword, BCrypt.gensalt(10));
        
        User user = new User(
            1, "USER001", "Test User", "IT", "Dev",
            UserRole.STAFF, hashedOld, LocalDateTime.now(), true
        );
        
        when(userDAO.findById(1)).thenReturn(Optional.of(user));
        when(userDAO.update(any(User.class))).thenReturn(true);
        
        // Act
        boolean result = userService.changePassword(1, oldPassword, "newpass123");
        
        // Assert
        assertTrue(result);
        verify(userDAO).update(any(User.class));
    }
    
    @Test
    @DisplayName("Should reject password change with wrong old password")
    void testChangePasswordWrongOld() throws SQLException {
        // Arrange
        String oldPassword = "oldpass";
        String hashedOld = BCrypt.hashpw(oldPassword, BCrypt.gensalt(10));
        
        User user = new User(
            1, "USER001", "Test User", "IT", "Dev",
            UserRole.STAFF, hashedOld, LocalDateTime.now(), true
        );
        
        when(userDAO.findById(1)).thenReturn(Optional.of(user));
        
        // Act
        boolean result = userService.changePassword(1, "wrongold", "newpass123");
        
        // Assert
        assertFalse(result);
        verify(userDAO, never()).update(any(User.class));
    }
}
