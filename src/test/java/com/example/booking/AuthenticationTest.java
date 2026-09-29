package com.example.booking;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthenticationTest extends IntegrationTestBase {

    @Test
    void loginWithValidCredentialsReturnsJwt() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"alice123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"nope\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithUnknownUserReturns401() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ghost\",\"password\":\"whatever\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithBlankFieldsReturns400() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.username").exists());
    }

    @Test
    void passwordsAreStoredAsBCryptHashes() {
        String stored = users.findByUsername("alice").orElseThrow().getPassword();
        org.junit.jupiter.api.Assertions.assertTrue(stored.startsWith("$2"));
        org.junit.jupiter.api.Assertions.assertNotEquals("alice123", stored);
    }

    @Test
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mvc.perform(get("/resources")).andExpect(status().isUnauthorized());
        mvc.perform(get("/reservations")).andExpect(status().isUnauthorized());
    }

    @Test
    void tamperedOrGarbageTokenReturns401() throws Exception {
        String valid = aliceToken();
        mvc.perform(get("/resources").header("Authorization", valid + "x")).andExpect(status().isUnauthorized());
        mvc.perform(get("/resources").header("Authorization", "Bearer not.a.jwt")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenOfDeletedUserIsRejected() throws Exception {
        String token = bobToken();
        users.delete(bob);
        mvc.perform(get("/resources").header("Authorization", token)).andExpect(status().isUnauthorized());
    }
}
