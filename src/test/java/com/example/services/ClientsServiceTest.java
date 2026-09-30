package com.example.services;

import com.example.DTOs.clients.ChangePasswordRequest;
import com.example.DTOs.clients.ClientAdminView;
import com.example.DTOs.clients.ClientLoginView;
import com.example.DTOs.clients.ClientProfileUpdateRequest;
import com.example.DTOs.clients.ClientProfileView;
import com.example.DTOs.clients.ClientReporterView;
import com.example.DTOs.clients.LoginRequest;
import com.example.DTOs.clients.LoginResponse;
import com.example.entities.Client;
import com.example.exceptions.ClientNotFoundException;
import com.example.exceptions.InvalidCredentialsException;
import com.example.exceptions.InvalidEmailException;
import com.example.exceptions.InvalidPasswordChangeException;
import com.example.exceptions.InvalidProfileUpdateException;
import com.example.exceptions.InvalidTransactionException;
import com.example.exceptions.InvalidUsernameChangeException;
import com.example.exceptions.UpdateFailedException;
import com.example.repositories.ClientsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ClientsServiceTest {

    private ClientsRepository clientsRepository;
    private ClientsService clientsService;

    @BeforeEach
    void setUp() {
        clientsRepository = mock(ClientsRepository.class);
        clientsService = new ClientsService(clientsRepository);
    }

    @Test
    void signupReturnsLoginResponseWhenClientIsCreated() {
        Client request = client(1L, "signup@example.com", "signupuser", "password1", "Sign", "Up", BigDecimal.ZERO);
        ClientLoginView createdClient = loginClientView(5L, "signupuser", "password1");
        when(clientsRepository.findClientIdByEmail("signup@example.com")).thenReturn(null);
        when(clientsRepository.findClientIdByUsername("signupuser")).thenReturn(null);
        when(clientsRepository.createClient(request)).thenReturn(1);
        when(clientsRepository.findLoginClientByUsername("signupuser")).thenReturn(createdClient);

        LoginResponse response = clientsService.signup(request);

        assertEquals(5L, response.clientId());
        assertEquals("signupuser", response.username());
        assertEquals("Signup successful", response.message());
        verify(clientsRepository).createClient(request);
    }

    @Test
    void signupThrowsInvalidEmailWhenEmailAlreadyExists() {
        Client request = client(null, "signup@example.com", "signupuser", "password1", "Sign", "Up", BigDecimal.ZERO);
        when(clientsRepository.findClientIdByEmail("signup@example.com")).thenReturn(1L);

        InvalidEmailException exception = assertThrows(InvalidEmailException.class, () -> clientsService.signup(request));

        assertEquals("Email is already in use", exception.getMessage());
        verify(clientsRepository).findClientIdByEmail("signup@example.com");
        verifyNoMoreInteractions(clientsRepository);
    }

    @Test
    void signupThrowsInvalidUsernameWhenUsernameAlreadyExists() {
        Client request = client(null, "signup@example.com", "signupuser", "password1", "Sign", "Up", BigDecimal.ZERO);
        when(clientsRepository.findClientIdByEmail("signup@example.com")).thenReturn(null);
        when(clientsRepository.findClientIdByUsername("signupuser")).thenReturn(2L);

        InvalidUsernameChangeException exception = assertThrows(InvalidUsernameChangeException.class, () -> clientsService.signup(request));

        assertEquals("Username is not unique", exception.getMessage());
    }

    @Test
    void signupThrowsUpdateFailedWhenInsertDoesNotAffectOneRow() {
        Client request = client(null, "signup@example.com", "signupuser", "password1", "Sign", "Up", BigDecimal.ZERO);
        when(clientsRepository.findClientIdByEmail("signup@example.com")).thenReturn(null);
        when(clientsRepository.findClientIdByUsername("signupuser")).thenReturn(null);
        when(clientsRepository.createClient(request)).thenReturn(0);

        UpdateFailedException exception = assertThrows(UpdateFailedException.class, () -> clientsService.signup(request));

        assertEquals("Client signup failed", exception.getMessage());
    }

    @Test
    void signupThrowsUpdateFailedWhenCreatedClientCannotBeLoaded() {
        Client request = client(null, "signup@example.com", "signupuser", "password1", "Sign", "Up", BigDecimal.ZERO);
        when(clientsRepository.findClientIdByEmail("signup@example.com")).thenReturn(null);
        when(clientsRepository.findClientIdByUsername("signupuser")).thenReturn(null);
        when(clientsRepository.createClient(request)).thenReturn(1);
        when(clientsRepository.findLoginClientByUsername("signupuser")).thenReturn(null);

        UpdateFailedException exception = assertThrows(UpdateFailedException.class, () -> clientsService.signup(request));

        assertEquals("Client signup failed", exception.getMessage());
    }

    @Test
    void loginReturnsLoginResponseWhenCredentialsAreValid() {
        LoginRequest request = loginRequest("loginuser", "password1");
        when(clientsRepository.findLoginClientByUsername("loginuser"))
                .thenReturn(loginClientView(3L, "loginuser", "password1"));

        LoginResponse response = clientsService.login(request);

        assertEquals(3L, response.clientId());
        assertEquals("loginuser", response.username());
        assertEquals("Login successful", response.message());
    }

    @Test
    void loginThrowsInvalidCredentialsWhenClientDoesNotExist() {
        LoginRequest request = loginRequest("missing", "password1");
        when(clientsRepository.findLoginClientByUsername("missing")).thenReturn(null);

        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class, () -> clientsService.login(request));

        assertEquals("Invalid username or password", exception.getMessage());
    }

    @Test
    void loginThrowsInvalidCredentialsWhenPasswordDoesNotMatch() {
        LoginRequest request = loginRequest("loginuser", "wrongpass");
        when(clientsRepository.findLoginClientByUsername("loginuser"))
                .thenReturn(loginClientView(3L, "loginuser", "password1"));

        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class, () -> clientsService.login(request));

        assertEquals("Invalid username or password", exception.getMessage());
    }

    @Test
    void changePasswordUpdatesPasswordWhenRequestIsValid() {
        ChangePasswordRequest request = changePasswordRequest("password1", "newpass12");
        when(clientsRepository.findLoginClientById(4L))
                .thenReturn(loginClientView(4L, "passworduser", "password1"));
        when(clientsRepository.updatePassword(4L, "newpass12")).thenReturn(1);

        clientsService.changePassword(4L, request);

        verify(clientsRepository).updatePassword(4L, "newpass12");
    }

    @Test
    void changePasswordThrowsNotFoundWhenClientDoesNotExist() {
        ChangePasswordRequest request = changePasswordRequest("password1", "newpass12");
        when(clientsRepository.findLoginClientById(4L)).thenReturn(null);

        ClientNotFoundException exception = assertThrows(ClientNotFoundException.class, () -> clientsService.changePassword(4L, request));

        assertEquals("Client not found", exception.getMessage());
    }

    @Test
    void changePasswordThrowsWhenCurrentPasswordIsIncorrect() {
        ChangePasswordRequest request = changePasswordRequest("wrongpass", "newpass12");
        when(clientsRepository.findLoginClientById(4L))
                .thenReturn(loginClientView(4L, "passworduser", "password1"));

        InvalidPasswordChangeException exception = assertThrows(InvalidPasswordChangeException.class, () -> clientsService.changePassword(4L, request));

        assertEquals("Current password is incorrect", exception.getMessage());
    }

    @Test
    void changePasswordThrowsWhenNewPasswordMatchesCurrentPassword() {
        ChangePasswordRequest request = changePasswordRequest("password1", "password1");
        when(clientsRepository.findLoginClientById(4L))
                .thenReturn(loginClientView(4L, "passworduser", "password1"));

        InvalidPasswordChangeException exception = assertThrows(InvalidPasswordChangeException.class, () -> clientsService.changePassword(4L, request));

        assertEquals("New password must be different from current password", exception.getMessage());
    }

    @Test
    void changePasswordThrowsUpdateFailedWhenUpdateCountIsNotOne() {
        ChangePasswordRequest request = changePasswordRequest("password1", "newpass12");
        when(clientsRepository.findLoginClientById(4L))
                .thenReturn(loginClientView(4L, "passworduser", "password1"));
        when(clientsRepository.updatePassword(4L, "newpass12")).thenReturn(0);

        UpdateFailedException exception = assertThrows(UpdateFailedException.class, () -> clientsService.changePassword(4L, request));

        assertEquals("Password update failed", exception.getMessage());
    }

    @Test
    void getClientProfileReturnsMappedView() {
        when(clientsRepository.findClientById(7L))
                .thenReturn(client(7L, "profile@example.com", "profileuser", "password1", "Profile", "User", new BigDecimal("200.00")));

        ClientProfileView profile = clientsService.getClientProfile(7L);

        assertEquals(7L, profile.clientId());
        assertEquals("profile@example.com", profile.email());
        assertEquals("profileuser", profile.username());
        assertEquals(new BigDecimal("200.00"), profile.cashAmount());
    }

    @Test
    void getClientProfileThrowsWhenClientDoesNotExist() {
        when(clientsRepository.findClientById(7L)).thenReturn(null);

        ClientNotFoundException exception = assertThrows(ClientNotFoundException.class, () -> clientsService.getClientProfile(7L));

        assertEquals("Client not found", exception.getMessage());
    }

    @Test
    void getClientDataAdminReturnsView() {
        ClientAdminView view = adminView(9L, "adminuser", "admin@example.com", "Admin", "User", new BigDecimal("300.00"));
        when(clientsRepository.findClientByIdAdmin(9L)).thenReturn(view);

        ClientAdminView result = clientsService.getClientDataAdmin(9L);

        assertEquals(view, result);
    }

    @Test
    void getClientDataAdminThrowsWhenClientDoesNotExist() {
        when(clientsRepository.findClientByIdAdmin(9L)).thenReturn(null);

        ClientNotFoundException exception = assertThrows(ClientNotFoundException.class, () -> clientsService.getClientDataAdmin(9L));

        assertEquals("Client not found", exception.getMessage());
    }

    @Test
    void getClientDataReporterReturnsView() {
        ClientReporterView view = reporterView(10L, "reporteruser", "reporter@example.com");
        when(clientsRepository.findClientByIdReporter(10L)).thenReturn(view);

        ClientReporterView result = clientsService.getClientDataReporter(10L);

        assertEquals(view, result);
    }

    @Test
    void getClientDataReporterThrowsWhenClientDoesNotExist() {
        when(clientsRepository.findClientByIdReporter(10L)).thenReturn(null);

        ClientNotFoundException exception = assertThrows(ClientNotFoundException.class, () -> clientsService.getClientDataReporter(10L));

        assertEquals("Client not found", exception.getMessage());
    }

    @Test
    void getAllClientDataAdminReturnsListWhenPresent() {
        List<ClientAdminView> expected = List.of(adminView(1L, "admin", "admin@example.com", "Admin", "One", BigDecimal.ZERO));
        when(clientsRepository.findClientsAsAdmin()).thenReturn(expected);

        List<ClientAdminView> actual = clientsService.getAllClientDataAdmin();

        assertEquals(expected, actual);
    }

    @Test
    void getAllClientDataAdminThrowsWhenListIsEmpty() {
        when(clientsRepository.findClientsAsAdmin()).thenReturn(List.of());

        ClientNotFoundException exception = assertThrows(ClientNotFoundException.class, () -> clientsService.getAllClientDataAdmin());

        assertEquals("No clients found", exception.getMessage());
    }

    @Test
    void getAllClientDataReporterReturnsListWhenPresent() {
        List<ClientReporterView> expected = List.of(reporterView(1L, "reporter", "reporter@example.com"));
        when(clientsRepository.findClientsAsReporter()).thenReturn(expected);

        List<ClientReporterView> actual = clientsService.getAllClientDataReporter();

        assertEquals(expected, actual);
    }

    @Test
    void getAllClientDataReporterThrowsWhenListIsEmpty() {
        when(clientsRepository.findClientsAsReporter()).thenReturn(List.of());

        ClientNotFoundException exception = assertThrows(ClientNotFoundException.class, () -> clientsService.getAllClientDataReporter());

        assertEquals("No clients found", exception.getMessage());
    }

    @Test
    void updateCashAmountReturnsNetBalanceWhenValid() {
        when(clientsRepository.findClientById(11L))
                .thenReturn(client(11L, "cash@example.com", "cashuser", "password1", "Cash", "User", new BigDecimal("100.00")));
        when(clientsRepository.updateCashAmount(11L, new BigDecimal("130.00"))).thenReturn(1);

        BigDecimal net = clientsService.updateCashAmount(11L, new BigDecimal("30.00"));

        assertEquals(new BigDecimal("130.00"), net);
    }

    @Test
    void updateCashAmountThrowsWhenClientDoesNotExist() {
        when(clientsRepository.findClientById(11L)).thenReturn(null);

        ClientNotFoundException exception = assertThrows(ClientNotFoundException.class, () -> clientsService.updateCashAmount(11L, BigDecimal.ONE));

        assertEquals("Client not found", exception.getMessage());
    }

    @Test
    void updateCashAmountThrowsWhenTransactionWouldGoNegative() {
        when(clientsRepository.findClientById(11L))
                .thenReturn(client(11L, "cash@example.com", "cashuser", "password1", "Cash", "User", new BigDecimal("100.00")));

        InvalidTransactionException exception = assertThrows(InvalidTransactionException.class,
                () -> clientsService.updateCashAmount(11L, new BigDecimal("-150.00")));

        assertEquals("Invalid transaction", exception.getMessage());
    }

    @Test
    void updateCashAmountThrowsWhenUpdateCountIsNotOne() {
        when(clientsRepository.findClientById(11L))
                .thenReturn(client(11L, "cash@example.com", "cashuser", "password1", "Cash", "User", new BigDecimal("100.00")));
        when(clientsRepository.updateCashAmount(11L, new BigDecimal("130.00"))).thenReturn(0);

        UpdateFailedException exception = assertThrows(UpdateFailedException.class,
                () -> clientsService.updateCashAmount(11L, new BigDecimal("30.00")));

        assertEquals("Cash update failed", exception.getMessage());
    }

    @Test
    void updateProfileUpdatesWhenRequestIsValid() {
        ClientProfileUpdateRequest request = profileUpdateRequest("updateduser", "Updated", "Name");
        when(clientsRepository.findClientById(12L))
                .thenReturn(client(12L, "profile@example.com", "currentuser", "password1", "Current", "User", BigDecimal.ZERO));
        when(clientsRepository.findClientIdByUsername("updateduser")).thenReturn(null);
        when(clientsRepository.updateProfile(12L, request)).thenReturn(1);

        clientsService.updateProfile(12L, request);

        verify(clientsRepository).updateProfile(12L, request);
    }

    @Test
    void updateProfileAllowsSameUsernameForSameClient() {
        ClientProfileUpdateRequest request = profileUpdateRequest("currentuser", null, null);
        when(clientsRepository.findClientById(12L))
                .thenReturn(client(12L, "profile@example.com", "currentuser", "password1", "Current", "User", BigDecimal.ZERO));
        when(clientsRepository.findClientIdByUsername("currentuser")).thenReturn(12L);
        when(clientsRepository.updateProfile(12L, request)).thenReturn(1);

        clientsService.updateProfile(12L, request);

        verify(clientsRepository).updateProfile(12L, request);
    }

    @Test
    void updateProfileThrowsWhenClientDoesNotExist() {
        ClientProfileUpdateRequest request = profileUpdateRequest("updateduser", "Updated", "Name");
        when(clientsRepository.findClientById(12L)).thenReturn(null);

        ClientNotFoundException exception = assertThrows(ClientNotFoundException.class, () -> clientsService.updateProfile(12L, request));

        assertEquals("Client not found", exception.getMessage());
    }

    @Test
    void updateProfileThrowsWhenNoChangesProvided() {
        ClientProfileUpdateRequest request = profileUpdateRequest(null, null, null);
        when(clientsRepository.findClientById(12L))
                .thenReturn(client(12L, "profile@example.com", "currentuser", "password1", "Current", "User", BigDecimal.ZERO));

        InvalidProfileUpdateException exception = assertThrows(InvalidProfileUpdateException.class,
                () -> clientsService.updateProfile(12L, request));

        assertEquals("No profile changes were provided", exception.getMessage());
    }

    @Test
    void updateProfileThrowsWhenUsernameBelongsToAnotherClient() {
        ClientProfileUpdateRequest request = profileUpdateRequest("takenuser", "Updated", "Name");
        when(clientsRepository.findClientById(12L))
                .thenReturn(client(12L, "profile@example.com", "currentuser", "password1", "Current", "User", BigDecimal.ZERO));
        when(clientsRepository.findClientIdByUsername("takenuser")).thenReturn(20L);

        InvalidUsernameChangeException exception = assertThrows(InvalidUsernameChangeException.class,
                () -> clientsService.updateProfile(12L, request));

        assertEquals("Username is not unique", exception.getMessage());
    }

    @Test
    void updateProfileThrowsWhenUpdateCountIsNotOne() {
        ClientProfileUpdateRequest request = profileUpdateRequest("updateduser", "Updated", "Name");
        when(clientsRepository.findClientById(12L))
                .thenReturn(client(12L, "profile@example.com", "currentuser", "password1", "Current", "User", BigDecimal.ZERO));
        when(clientsRepository.findClientIdByUsername("updateduser")).thenReturn(null);
        when(clientsRepository.updateProfile(12L, request)).thenReturn(0);

        UpdateFailedException exception = assertThrows(UpdateFailedException.class,
                () -> clientsService.updateProfile(12L, request));

        assertEquals("Profile update failed", exception.getMessage());
    }

    private Client client(
            Long clientId,
            String email,
            String username,
            String password,
            String firstName,
            String lastName,
            BigDecimal cashAmount) {
        Client client = new Client();
        client.setClientId(clientId);
        client.setEmail(email);
        client.setUsername(username);
        client.setPassword(password);
        client.setFirstName(firstName);
        client.setLastName(lastName);
        client.setCashAmount(cashAmount);
        return client;
    }

    private ClientLoginView loginClientView(Long clientId, String username, String password) {
        ClientLoginView view = new ClientLoginView();
        view.setClientId(clientId);
        view.setUsername(username);
        view.setPassword(password);
        return view;
    }

    private LoginRequest loginRequest(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    private ChangePasswordRequest changePasswordRequest(String currentPassword, String newPassword) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword(currentPassword);
        request.setNewPassword(newPassword);
        return request;
    }

    private ClientAdminView adminView(
            Long clientId,
            String username,
            String email,
            String firstName,
            String lastName,
            BigDecimal cashAmount) {
        ClientAdminView view = new ClientAdminView();
        view.setClientId(clientId);
        view.setUsername(username);
        view.setEmail(email);
        view.setFirstName(firstName);
        view.setLastName(lastName);
        view.setCashAmount(cashAmount);
        return view;
    }

    private ClientReporterView reporterView(Long clientId, String username, String email) {
        ClientReporterView view = new ClientReporterView();
        view.setClientId(clientId);
        view.setUsername(username);
        view.setEmail(email);
        return view;
    }

    private ClientProfileUpdateRequest profileUpdateRequest(String username, String firstName, String lastName) {
        ClientProfileUpdateRequest request = new ClientProfileUpdateRequest();
        request.setUsername(username);
        request.setFirstName(firstName);
        request.setLastName(lastName);
        return request;
    }
}