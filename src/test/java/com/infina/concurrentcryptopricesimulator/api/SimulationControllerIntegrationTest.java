package com.infina.concurrentcryptopricesimulator.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class SimulationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsInitialCoinsBeforeFirstSimulation() throws Exception {
        mockMvc.perform(get("/coins"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("BTC"))
                .andExpect(jsonPath("$[0].currentPrice").value(60_000));
    }

    @Test
    void returnsNotFoundWhenStatsDoNotExist() throws Exception {
        mockMvc.perform(get("/stats"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Henüz bir simülasyon çalıştırılmadı."));
    }

    @Test
    void runsSimulationAndExposesLatestResults() throws Exception {
        mockMvc.perform(post("/simulate")
                        .param("updates", "500")
                        .param("workers", "4")
                        .param("seed", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seed").value(42))
                .andExpect(jsonPath("$.submittedUpdates").value(500))
                .andExpect(jsonPath("$.workers").value(4))
                .andExpect(jsonPath("$.safeProcessedUpdates").value(500))
                .andExpect(jsonPath("$.safeInvariantPassed").value(true));

        mockMvc.perform(get("/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seed").value(42));

        mockMvc.perform(get("/coins"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].updateCount").isNumber());
    }

    @Test
    void rejectsInvalidSimulationParameters() throws Exception {
        mockMvc.perform(post("/simulate")
                        .param("updates", "0")
                        .param("workers", "17"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
