package com.bank.core.controller;

import com.bank.common.dto.UniversalResponse;
import com.bank.core.config.SecurityConfig;
import com.bank.core.config.SimpleCorsConfiguration;
import com.bank.core.dto.auth.RegisterRequest;
import com.bank.core.dto.auth.RegisterResponse;
import com.bank.core.mapper.AccountMapper;
import com.bank.core.repository.BankAccountRepository;
import com.bank.core.security.CurrentUserProvider;
import com.bank.core.service.AccountService;
import com.bank.core.service.InterestAccrualService;
import com.bank.core.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AuthController.class,
        AccountController.class,
        AdminController.class
})
@Import({SecurityConfig.class, SimpleCorsConfiguration.class})
@TestPropertySource(properties = "spring.security.cors.allowed-origins=http://localhost:3000")
class AuthSecurityWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @MockitoBean
    private BankAccountRepository bankAccountRepository;

    @MockitoBean
    private AccountMapper accountMapper;

    @MockitoBean
    private InterestAccrualService interestAccrualService;

    @Test
    void protectedEndpointWithoutSessionReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/accounts/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsUnauthorizedForAnonymousUser() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data.authenticated").value(false));
    }

    @Test
    void registrationWorks() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.register(any(RegisterRequest.class)))
                .thenReturn(new UniversalResponse<>(new RegisterResponse(
                        userId,
                        "student@example.com",
                        "USER",
                        Instant.parse("2026-05-15T00:00:00Z")
                )));

        mockMvc.perform(post("/api/auth/registration")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "student@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(userId.toString()))
                .andExpect(jsonPath("$.data.email").value("student@example.com"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    void loginCreatesSession() throws Exception {
        when(userService.loadUserByUsername("student@example.com"))
                .thenReturn(User.withUsername("student@example.com")
                        .password(passwordEncoder.encode("password123"))
                        .roles("USER")
                        .build());

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "student@example.com")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(authenticated().withUsername("student@example.com"))
                .andReturn();

        assertThat(result.getRequest().getSession(false)).isNotNull();
    }

    @Test
    void adminEndpointIsForbiddenForUserRole() throws Exception {
        mockMvc.perform(get("/api/admin/accounts")
                        .with(user("student@example.com").roles("USER")))
                .andExpect(status().isForbidden());
    }
}
