package com.example.booking;

import com.example.booking.entity.*;
import com.example.booking.repository.ReservationRepository;
import com.example.booking.repository.ResourceRepository;
import com.example.booking.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
abstract class IntegrationTestBase {
    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper om;
    @Autowired protected UserRepository users;
    @Autowired protected ResourceRepository resources;
    @Autowired protected ReservationRepository reservations;
    @Autowired protected PasswordEncoder encoder;

    protected User admin, alice, bob;
    protected Resource room, projector;

    @BeforeEach
    void seedData() {
        reservations.deleteAll();
        resources.deleteAll();
        users.deleteAll();
        admin = users.save(new User("admin", encoder.encode("admin123"), Role.ADMIN));
        alice = users.save(new User("alice", encoder.encode("alice123"), Role.USER));
        bob = users.save(new User("bob", encoder.encode("bob12345"), Role.USER));
        room = resources.save(new Resource("Room A", "Meeting room", "ROOM", true));
        projector = resources.save(new Resource("Projector", "4K", "EQUIPMENT", true));
    }

    protected String bearer(String username, String password) throws Exception {
        String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    protected String adminToken() throws Exception { return bearer("admin", "admin123"); }
    protected String aliceToken() throws Exception { return bearer("alice", "alice123"); }
    protected String bobToken() throws Exception { return bearer("bob", "bob12345"); }

    protected Reservation saveReservation(User owner, Resource res, String price, ReservationStatus st, int daysAhead) {
        Reservation r = new Reservation();
        r.setUser(owner);
        r.setResource(res);
        r.setStartTime(LocalDateTime.now().plusDays(daysAhead).withNano(0));
        r.setEndTime(LocalDateTime.now().plusDays(daysAhead).plusHours(2).withNano(0));
        r.setPrice(new BigDecimal(price));
        r.setStatus(st);
        return reservations.save(r);
    }

    protected String reservationJson(Long resourceId, Long userId, LocalDateTime start, LocalDateTime end,
                                     String price, String status) {
        return """
                {"resourceId": %d, "userId": %s, "startTime": "%s", "endTime": "%s", "price": %s, "status": %s}
                """.formatted(resourceId, userId, start, end, price, status == null ? "null" : "\"" + status + "\"");
    }
}
