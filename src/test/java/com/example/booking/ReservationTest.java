package com.example.booking;

import com.example.booking.entity.Reservation;
import com.example.booking.entity.ReservationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReservationTest extends IntegrationTestBase {

    private LocalDateTime start() { return LocalDateTime.now().plusDays(3).withNano(0); }

    // ---------- ownership ----------

    @Test
    void userIdentityComesFromJwtNotFromRequestBody() throws Exception {
        // Alice tries to book "as bob" and to force CONFIRMED status.
        mvc.perform(post("/reservations").header("Authorization", aliceToken()).contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(room.getId(), bob.getId(), start(), start().plusHours(2), "120.50", "CONFIRMED")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(alice.getId()))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.price").value(120.50));
    }

    @Test
    void adminMayBookOnBehalfOfAnotherUserWithStatus() throws Exception {
        mvc.perform(post("/reservations").header("Authorization", adminToken()).contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(room.getId(), bob.getId(), start(), start().plusHours(2), "10.00", "CONFIRMED")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(bob.getId()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void userSeesOnlyOwnReservationsAdminSeesAll() throws Exception {
        saveReservation(alice, room, "10.00", ReservationStatus.PENDING, 1);
        saveReservation(alice, projector, "20.00", ReservationStatus.CONFIRMED, 2);
        saveReservation(bob, room, "30.00", ReservationStatus.PENDING, 5);

        mvc.perform(get("/reservations").header("Authorization", aliceToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/reservations").header("Authorization", bobToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].username").value("bob"));
        mvc.perform(get("/reservations").header("Authorization", adminToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void userCannotReadOrCancelSomeoneElsesReservation() throws Exception {
        Reservation bobs = saveReservation(bob, room, "30.00", ReservationStatus.PENDING, 5);
        mvc.perform(get("/reservations/" + bobs.getId()).header("Authorization", aliceToken()))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/reservations/" + bobs.getId() + "/cancel").header("Authorization", aliceToken()))
                .andExpect(status().isForbidden());
        assertEquals(ReservationStatus.PENDING, reservations.findById(bobs.getId()).orElseThrow().getStatus());
    }

    @Test
    void userCanReadAndCancelOwnReservation() throws Exception {
        Reservation mine = saveReservation(alice, room, "30.00", ReservationStatus.PENDING, 5);
        mvc.perform(get("/reservations/" + mine.getId()).header("Authorization", aliceToken()))
                .andExpect(status().isOk());
        mvc.perform(patch("/reservations/" + mine.getId() + "/cancel").header("Authorization", aliceToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void userCannotUpdateOrDeleteReservations() throws Exception {
        Reservation mine = saveReservation(alice, room, "30.00", ReservationStatus.PENDING, 5);
        mvc.perform(put("/reservations/" + mine.getId()).header("Authorization", aliceToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(room.getId(), null, start(), start().plusHours(1), "1.00", "CONFIRMED")))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/reservations/" + mine.getId()).header("Authorization", aliceToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminHasFullCrudOnReservations() throws Exception {
        Reservation r = saveReservation(alice, room, "30.00", ReservationStatus.PENDING, 5);
        String t = adminToken();
        mvc.perform(get("/reservations/" + r.getId()).header("Authorization", t)).andExpect(status().isOk());
        mvc.perform(put("/reservations/" + r.getId()).header("Authorization", t).contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(projector.getId(), null, start(), start().plusHours(4), "99.99", "CONFIRMED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.resourceId").value(projector.getId()))
                .andExpect(jsonPath("$.userId").value(alice.getId()));
        mvc.perform(delete("/reservations/" + r.getId()).header("Authorization", t)).andExpect(status().isNoContent());
        mvc.perform(get("/reservations/" + r.getId()).header("Authorization", t)).andExpect(status().isNotFound());
    }

    @Test
    void unauthenticatedReservationAccessReturns401() throws Exception {
        mvc.perform(post("/reservations").contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(room.getId(), null, start(), start().plusHours(1), "5.00", null)))
                .andExpect(status().isUnauthorized());
    }

    // ---------- validation ----------

    @Test
    void endBeforeStartReturns400() throws Exception {
        mvc.perform(post("/reservations").header("Authorization", aliceToken()).contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(room.getId(), null, start(), start().minusHours(1), "10.00", null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void startInThePastReturns400() throws Exception {
        LocalDateTime past = LocalDateTime.now().minusDays(1);
        mvc.perform(post("/reservations").header("Authorization", aliceToken()).contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(room.getId(), null, past, past.plusHours(1), "10.00", null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void negativePriceAndMissingFieldsReturn400() throws Exception {
        mvc.perform(post("/reservations").header("Authorization", aliceToken()).contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(room.getId(), null, start(), start().plusHours(1), "-5.00", null)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.price").exists());
        mvc.perform(post("/reservations").header("Authorization", aliceToken()).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.resourceId").exists())
                .andExpect(jsonPath("$.details.startTime").exists());
    }

    @Test
    void invalidStatusValueReturns400() throws Exception {
        mvc.perform(post("/reservations").header("Authorization", adminToken()).contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(room.getId(), null, start(), start().plusHours(1), "5.00", "DONE")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownResourceReturns404() throws Exception {
        mvc.perform(post("/reservations").header("Authorization", aliceToken()).contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(9999L, null, start(), start().plusHours(1), "5.00", null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void overlappingBookingReturns409ButCancelledOnesDoNotBlock() throws Exception {
        String body = reservationJson(room.getId(), null, start(), start().plusHours(2), "10.00", null);
        mvc.perform(post("/reservations").header("Authorization", aliceToken()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/reservations").header("Authorization", bobToken()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());

        Reservation first = reservations.findAll().get(0);
        first.setStatus(ReservationStatus.CANCELLED);
        reservations.save(first);
        mvc.perform(post("/reservations").header("Authorization", bobToken()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void unavailableResourceCannotBeBooked() throws Exception {
        room.setAvailable(false);
        resources.save(room);
        mvc.perform(post("/reservations").header("Authorization", aliceToken()).contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(room.getId(), null, start(), start().plusHours(1), "5.00", null)))
                .andExpect(status().isConflict());
    }

    // ---------- filtering, pagination, sorting ----------

    @Test
    void filtersByStatusAndPriceRange() throws Exception {
        saveReservation(alice, room, "10.00", ReservationStatus.PENDING, 1);
        saveReservation(alice, room, "50.00", ReservationStatus.CONFIRMED, 2);
        saveReservation(alice, room, "90.00", ReservationStatus.CONFIRMED, 3);
        saveReservation(alice, room, "120.00", ReservationStatus.CANCELLED, 4);
        String t = aliceToken();

        mvc.perform(get("/reservations?status=CONFIRMED").header("Authorization", t))
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/reservations?minPrice=50").header("Authorization", t))
                .andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/reservations?maxPrice=50").header("Authorization", t))
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/reservations?status=CONFIRMED&minPrice=60&maxPrice=100").header("Authorization", t))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].price").value(90.0));
    }

    @Test
    void invalidFilterValuesReturn400() throws Exception {
        String t = aliceToken();
        mvc.perform(get("/reservations?status=BOGUS").header("Authorization", t)).andExpect(status().isBadRequest());
        mvc.perform(get("/reservations?minPrice=abc").header("Authorization", t)).andExpect(status().isBadRequest());
        mvc.perform(get("/reservations?minPrice=100&maxPrice=10").header("Authorization", t)).andExpect(status().isBadRequest());
    }

    @Test
    void paginationAndSorting() throws Exception {
        for (int i = 1; i <= 5; i++) saveReservation(alice, room, (i * 10) + ".00", ReservationStatus.PENDING, i);
        String t = aliceToken();

        mvc.perform(get("/reservations?page=1&size=2&sortBy=price&direction=desc").header("Authorization", t))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content[0].price").value(30.0))
                .andExpect(jsonPath("$.content[1].price").value(20.0));
    }

    @Test
    void invalidPagingOrSortingReturns400() throws Exception {
        String t = aliceToken();
        mvc.perform(get("/reservations?sortBy=password").header("Authorization", t)).andExpect(status().isBadRequest());
        mvc.perform(get("/reservations?direction=sideways").header("Authorization", t)).andExpect(status().isBadRequest());
        mvc.perform(get("/reservations?size=0").header("Authorization", t)).andExpect(status().isBadRequest());
        mvc.perform(get("/reservations?size=1000").header("Authorization", t)).andExpect(status().isBadRequest());
        mvc.perform(get("/reservations?page=-1").header("Authorization", t)).andExpect(status().isBadRequest());
    }

    @Test
    void filtersNeverLeakOtherUsersReservationsToUser() throws Exception {
        saveReservation(bob, room, "50.00", ReservationStatus.CONFIRMED, 1);
        mvc.perform(get("/reservations?status=CONFIRMED&minPrice=1").header("Authorization", aliceToken()))
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
