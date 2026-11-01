package com.finnexa.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
class HealthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void healthCheck_ShouldReturnOkWithSuccessMessage() throws Exception {
		mockMvc.perform(get("/api/health")).andExpect(status().isOk())
				.andExpect(content().string("FinNexa is running successfully!"));
	}

}
