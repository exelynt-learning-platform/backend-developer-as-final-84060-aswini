package com.example.booking;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResourceAuthorizationTest extends IntegrationTestBase {
    private static final String VALID = "{\"name\":\"Van\",\"description\":\"Cargo van\",\"type\":\"VEHICLE\",\"available\":true}";

    @Test
    void userCanReadResources() throws Exception {
        mvc.perform(get("/resources").header("Authorization", aliceToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/resources/" + room.getId()).header("Authorization", aliceToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Room A"));
    }

    @Test
    void userCannotCreateUpdateOrDeleteResources() throws Exception {
        String t = aliceToken();
        mvc.perform(post("/resources").header("Authorization", t).contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isForbidden());
        mvc.perform(put("/resources/" + room.getId()).header("Authorization", t).contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/resources/" + room.getId()).header("Authorization", t))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminHasFullCrudOnResources() throws Exception {
        String t = adminToken();
        String created = mvc.perform(post("/resources").header("Authorization", t).contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();
        Number id = com.jayway.jsonpath.JsonPath.read(created, "$.id");

        mvc.perform(put("/resources/" + id).header("Authorization", t).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Big Van\",\"type\":\"VEHICLE\",\"available\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Big Van"))
                .andExpect(jsonPath("$.available").value(false));
        mvc.perform(delete("/resources/" + id).header("Authorization", t)).andExpect(status().isNoContent());
        mvc.perform(get("/resources/" + id).header("Authorization", t)).andExpect(status().isNotFound());
    }

    @Test
    void invalidResourceReturns400WithFieldDetails() throws Exception {
        mvc.perform(post("/resources").header("Authorization", adminToken()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"type\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.name").exists())
                .andExpect(jsonPath("$.details.type").exists());
    }

    @Test
    void deletingResourceWithReservationsReturns409() throws Exception {
        saveReservation(alice, room, "50.00", com.example.booking.entity.ReservationStatus.PENDING, 2);
        mvc.perform(delete("/resources/" + room.getId()).header("Authorization", adminToken()))
                .andExpect(status().isConflict());
    }

    @Test
    void availableFilterWorks() throws Exception {
        projector.setAvailable(false);
        resources.save(projector);
        mvc.perform(get("/resources?available=true").header("Authorization", aliceToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }
}
