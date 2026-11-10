package com.foodapp.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:ordertest",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class OrderControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    RestaurantClient restaurantClient;

    private static final String BODY =
            "{\"customerName\":\"Asha\",\"restaurantId\":1,\"items\":[{\"menuItemId\":10,\"quantity\":2}]}";

    @Test
    void createOrderCalculatesTotalFromRestaurantPrices() throws Exception {
        when(restaurantClient.findMenuItem(10L)).thenReturn(Optional.of(
                new RestaurantClient.MenuItemDto(10L, 1L, "Paneer Tikka", new BigDecimal("9.50"), true)));

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(19.00))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void unknownMenuItemIsRejected() throws Exception {
        when(restaurantClient.findMenuItem(anyLong())).thenReturn(Optional.empty());
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void unavailableItemIsRejected() throws Exception {
        when(restaurantClient.findMenuItem(10L)).thenReturn(Optional.of(
                new RestaurantClient.MenuItemDto(10L, 1L, "Paneer Tikka", new BigDecimal("9.50"), false)));
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void cancelOrderThenCannotChangeStatus() throws Exception {
        when(restaurantClient.findMenuItem(10L)).thenReturn(Optional.of(
                new RestaurantClient.MenuItemDto(10L, 1L, "Paneer Tikka", new BigDecimal("9.50"), true)));
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated());

        mvc.perform(delete("/api/orders/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(put("/api/orders/1/status").param("status", "CONFIRMED"))
                .andExpect(status().isConflict());
    }
}
