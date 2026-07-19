package com.infina.concurrentcryptopricesimulator.api;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /simulate, /coins ve /stats endpoint'lerinin integration testleri.
 *
 * <p>lastStats controller uzerinde tutulan bir durum oldugu icin "henuz simulasyon yok" senaryosu
 * @Order(1) ile once calistirilir; sonraki testler simulasyon calistirdiginda o durum kaybolur.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SimulationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@Order(1)
	void statsReturns404WhenNoSimulationHasRunYet() throws Exception {
		mockMvc.perform(get("/stats"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Henüz bir simülasyon çalıştırılmadı."));
	}

	@Test
	@Order(2)
	void coinsReturnInitialPricesBeforeAnySimulation() throws Exception {
		mockMvc.perform(get("/coins"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)))
				.andExpect(jsonPath("$[?(@.id == 'BTC')].initialPrice").value(60000))
				.andExpect(jsonPath("$[?(@.id == 'ETH')].initialPrice").value(3000))
				.andExpect(jsonPath("$[?(@.id == 'SOL')].initialPrice").value(150));
	}

	@ParameterizedTest(name = "updates={0}, workers={1} -> 400")
	@CsvSource({
			"0, 4",
			"-10, 4",
			"100001, 4",
			"500, 0",
			"500, 1000",
			"500, 17"
	})
	@Order(3)
	void invalidParametersReturnBadRequest(String updates, String workers) throws Exception {
		mockMvc.perform(post("/simulate")
						.param("updates", updates)
						.param("workers", workers))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(notNullValue()));
	}

	@Test
	@Order(4)
	void simulateRunsToCompletionAndSatisfiesSafeInvariant() throws Exception {
		mockMvc.perform(post("/simulate")
						.param("updates", "2000")
						.param("workers", "4")
						.param("seed", "42"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.seed").value(42))
				.andExpect(jsonPath("$.submittedUpdates").value(2000))
				.andExpect(jsonPath("$.workers").value(4))
				// Yonerge §10: guvenli kosu her gorevi kaybetmeden islemelidir.
				.andExpect(jsonPath("$.safeInvariantPassed").value(true))
				.andExpect(jsonPath("$.safeProcessedUpdates").value(2000))
				.andExpect(jsonPath("$.coins", hasSize(3)));
	}

	@Test
	@Order(5)
	void statsReturnsLastCompletedSimulationAfterRun() throws Exception {
		mockMvc.perform(post("/simulate")
						.param("updates", "1000")
						.param("workers", "2")
						.param("seed", "7"))
				.andExpect(status().isOk());

		mockMvc.perform(get("/stats"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.seed").value(7))
				.andExpect(jsonPath("$.submittedUpdates").value(1000))
				.andExpect(jsonPath("$.workers").value(2))
				.andExpect(jsonPath("$.safeInvariantPassed").value(true));
	}

	@Test
	@Order(6)
	void coinsReflectSafeStateOfLastSimulation() throws Exception {
		mockMvc.perform(post("/simulate")
						.param("updates", "1500")
						.param("workers", "4")
						.param("seed", "99"))
				.andExpect(status().isOk());

		mockMvc.perform(get("/coins"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)))
				// Her coin en az bir gorev almis ve son guncelleyen worker kaydedilmis olmali.
				.andExpect(jsonPath("$[0].updateCount").value(greaterThan(0)))
				.andExpect(jsonPath("$[0].lastUpdatedBy").value(notNullValue()));
	}

	@Test
	@Order(7)
	void sameSeedProducesSameExpectedPrices() throws Exception {
		String first = runAndReturnBody(42L);
		String second = runAndReturnBody(42L);

		org.junit.jupiter.api.Assertions.assertEquals(
				expectedPricesOf(first),
				expectedPricesOf(second),
				"Ayni seed ayni gorev listesini, dolayisiyla ayni beklenen fiyatlari uretmelidir"
		);
	}

	private String runAndReturnBody(long seed) throws Exception {
		return mockMvc.perform(post("/simulate")
						.param("updates", "1000")
						.param("workers", "4")
						.param("seed", String.valueOf(seed)))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
	}

	/** Cevap govdesinden yalnizca coin id -> expected fiyat esleslerini cikarir. */
	private static String expectedPricesOf(String body) {
		return com.jayway.jsonpath.JsonPath.read(body, "$.coins[*].expected").toString();
	}
}
