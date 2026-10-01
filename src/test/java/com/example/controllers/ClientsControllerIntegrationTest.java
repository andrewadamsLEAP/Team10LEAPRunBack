package com.example.controllers;

import com.example.DTOs.clients.ChangePasswordRequest;
import com.example.DTOs.clients.ClientProfileUpdateRequest;
import com.example.DTOs.clients.LoginRequest;
import com.example.entities.Client;
import com.example.tradingApp.TradingAppApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TradingAppApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "classpath:clients-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ClientsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void signupCreatesClientAndReturnsLoginResponse() throws Exception {
        Client request = signupClient("new.user@example.com", "newuser", "password1", "New", "User");

        mockMvc.perform(post("/api/v1/clients/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").isNumber())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.message").value("Signup successful"));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM clients WHERE username = ?",
                Integer.class,
                "newuser");

        assertEquals(1, count);
    }

    @Test
    void signupRejectsDuplicateEmail() throws Exception {
        insertClient(
                "taken@example.com",
                "existinguser",
                "password1",
                "Existing",
                "User",
                new BigDecimal("100.00"));

        Client request = signupClient("taken@example.com", "newuser", "password1", "New", "User");

        mockMvc.perform(post("/api/v1/clients/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid Email: Email is already in use"));
    }

    @Test
    void loginReturnsLoginResponseForValidCredentials() throws Exception {
        Long clientId = insertClient(
                "login@example.com",
                "loginuser",
                "password1",
                "Login",
                "User",
                new BigDecimal("100.00"));

        LoginRequest request = loginRequest("loginuser", "password1");

        mockMvc.perform(post("/api/v1/clients/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(clientId))
                .andExpect(jsonPath("$.username").value("loginuser"))
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    void loginRejectsInvalidCredentials() throws Exception {
        Long clientId = insertClient(
                "login-fail@example.com",
                "loginfailuser",
                "password1",
                "Login",
                "Failure",
                new BigDecimal("100.00"));

        LoginRequest request = loginRequest("loginfailuser", "wrongpass");

        mockMvc.perform(post("/api/v1/clients/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid Credentials: Invalid username or password"));

        assertEquals(
                clientId,
                jdbcTemplate.queryForObject(
                        "SELECT client_id FROM clients WHERE username = ?",
                        Long.class,
                        "loginfailuser"));
    }

    @Test
    void changePasswordUpdatesStoredPassword() throws Exception {
        Long clientId = insertClient(
                "password@example.com",
                "passworduser",
                "password1",
                "Password",
                "User",
                new BigDecimal("100.00"));

        ChangePasswordRequest request = changePasswordRequest("password1", "newpass12");

        mockMvc.perform(patch("/api/v1/clients/password/{clientId}", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordJson(request)))
                .andExpect(status().isNoContent());

        String updatedPassword = jdbcTemplate.queryForObject(
                "SELECT password FROM clients WHERE client_id = ?",
                String.class,
                clientId);

        assertEquals("newpass12", updatedPassword);
    }

    @Test
    void changePasswordRejectsIncorrectCurrentPassword() throws Exception {
        Long clientId = insertClient(
                "bad-password@example.com",
                "badpassworduser",
                "password1",
                "Bad",
                "Password",
                new BigDecimal("100.00"));

        ChangePasswordRequest request = changePasswordRequest("wrongpass", "newpass12");

        mockMvc.perform(patch("/api/v1/clients/password/{clientId}", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid Password Change: Current password is incorrect"));

        assertEquals(
                "password1",
                jdbcTemplate.queryForObject(
                        "SELECT password FROM clients WHERE client_id = ?",
                        String.class,
                        clientId));
    }

    @Test
    void updateProfileUpdatesStoredFields() throws Exception {
        Long clientId = insertClient(
                "profile@example.com",
                "profileuser",
                "password1",
                "Profile",
                "User",
                new BigDecimal("125.00"));

        ClientProfileUpdateRequest request = profileUpdateRequest("updateduser", "Updated", "Name");

        mockMvc.perform(patch("/api/v1/clients/profile/{clientId}", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileUpdateJson(request)))
                .andExpect(status().isNoContent());

        assertEquals(
                "updateduser",
                jdbcTemplate.queryForObject(
                        "SELECT username FROM clients WHERE client_id = ?",
                        String.class,
                        clientId));
        assertEquals(
                "Updated",
                jdbcTemplate.queryForObject(
                        "SELECT first_name FROM clients WHERE client_id = ?",
                        String.class,
                        clientId));
        assertEquals(
                "Name",
                jdbcTemplate.queryForObject(
                        "SELECT last_name FROM clients WHERE client_id = ?",
                        String.class,
                        clientId));
    }

    @Test
    void updateProfileRejectsEmptyRequest() throws Exception {
        Long clientId = insertClient(
                "empty-profile@example.com",
                "emptyprofileuser",
                "password1",
                "Empty",
                "Profile",
                new BigDecimal("125.00"));

        mockMvc.perform(patch("/api/v1/clients/profile/{clientId}", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void viewProfileReturnsClientProfileView() throws Exception {
        Long clientId = insertClient(
                "view@example.com",
                "viewuser",
                "password1",
                "View",
                "User",
                new BigDecimal("250.00"));

        mockMvc.perform(get("/api/v1/clients/profile/{clientId}", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(clientId))
                .andExpect(jsonPath("$.email").value("view@example.com"))
                .andExpect(jsonPath("$.username").value("viewuser"))
                .andExpect(jsonPath("$.firstName").value("View"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.cashAmount").value(250.00))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

        @Test
        void viewProfileReturnsNotFoundForMissingClient() throws Exception {
                mockMvc.perform(get("/api/v1/clients/profile/{clientId}", 99999L))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.detail").value("Client not found"));
        }

    private Long insertClient(
            String email,
            String username,
            String password,
            String firstName,
            String lastName,
            BigDecimal cashAmount) {

        jdbcTemplate.update(
                "INSERT INTO clients (email, username, password, first_name, last_name, cash_amount) VALUES (?, ?, ?, ?, ?, ?)",
                email,
                username,
                password,
                firstName,
                lastName,
                cashAmount);

        return jdbcTemplate.queryForObject(
                "SELECT client_id FROM clients WHERE username = ?",
                Long.class,
                username);
    }

    private Client signupClient(
            String email,
            String username,
            String password,
            String firstName,
            String lastName) {

        Client client = new Client();
        client.setEmail(email);
        client.setUsername(username);
        client.setPassword(password);
        client.setFirstName(firstName);
        client.setLastName(lastName);
        return client;
    }

    private LoginRequest loginRequest(String username, String password) {
        return new LoginRequest(username, password);
    }

    private ChangePasswordRequest changePasswordRequest(String currentPassword, String newPassword) {
        return new ChangePasswordRequest(currentPassword, newPassword);
    }

    private ClientProfileUpdateRequest profileUpdateRequest(String username, String firstName, String lastName) {
        return new ClientProfileUpdateRequest(username, firstName, lastName);
    }

    private String signupJson(Client client) {
        return "{" +
                quotedField("email", client.getEmail()) + "," +
                quotedField("username", client.getUsername()) + "," +
                quotedField("password", client.getPassword()) + "," +
                quotedField("firstName", client.getFirstName()) + "," +
                quotedField("lastName", client.getLastName()) +
                "}";
    }

    private String loginJson(LoginRequest request) {
        return "{" +
                quotedField("username", request.username()) + "," +
                quotedField("password", request.password()) +
                "}";
    }

    private String changePasswordJson(ChangePasswordRequest request) {
        return "{" +
                quotedField("currentPassword", request.currentPassword()) + "," +
                quotedField("newPassword", request.newPassword()) +
                "}";
    }

    private String profileUpdateJson(ClientProfileUpdateRequest request) {
        return "{" +
                quotedField("username", request.username()) + "," +
                quotedField("firstName", request.firstName()) + "," +
                quotedField("lastName", request.lastName()) +
                "}";
    }

    private String quotedField(String name, String value) {
        if (value == null) {
            return "\"" + name + "\":null";
        }

        return "\"" + name + "\":\"" + escapeJson(value) + "\"";
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}