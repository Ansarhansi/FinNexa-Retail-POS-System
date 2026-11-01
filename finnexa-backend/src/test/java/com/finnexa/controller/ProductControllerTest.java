
package com.finnexa.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finnexa.dto.ProductRequest;
import com.finnexa.dto.ProductResponse;
import com.finnexa.entity.Product;
import com.finnexa.service.ProductService;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

	@Mock
	private ProductService productService;

	@InjectMocks
	private ProductController productController;

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(productController).build();

		objectMapper = new ObjectMapper();
	}

	@Test
	void createProduct_shouldReturnCreatedStatus() throws Exception {
		ProductRequest request = createRequest();

		ProductResponse response = createResponse();

		when(productService.createProduct(any(ProductRequest.class))).thenReturn(response);

		mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Laptop")).andExpect(jsonPath("$.sku").value("LAP-001"));

		verify(productService).createProduct(any(ProductRequest.class));
	}

	@Test
	void getAllProducts_shouldReturnProductList() throws Exception {
		when(productService.getAllProducts()).thenReturn(List.of(createResponse()));

		mockMvc.perform(get("/api/products")).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Laptop")).andExpect(jsonPath("$[0].sku").value("LAP-001"));

		verify(productService).getAllProducts();
	}

	@Test
	void getProductById_shouldReturnProduct() throws Exception {
		when(productService.getProductById(1L)).thenReturn(createResponse());

		mockMvc.perform(get("/api/products/{id}", 1L)).andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Laptop")).andExpect(jsonPath("$.sku").value("LAP-001"));

		verify(productService).getProductById(1L);
	}

	@Test
	void getProductBySku_shouldReturnProduct() throws Exception {
		when(productService.getProductBySku("LAP-001")).thenReturn(createResponse());

		mockMvc.perform(get("/api/products/sku/{sku}", "LAP-001")).andExpect(status().isOk())
				.andExpect(jsonPath("$.sku").value("LAP-001"));

		verify(productService).getProductBySku("LAP-001");
	}

	@Test
	void searchProducts_shouldReturnMatchingProducts() throws Exception {
		when(productService.searchProducts("Laptop")).thenReturn(List.of(createResponse()));

		mockMvc.perform(get("/api/products/search").param("name", "Laptop")).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Laptop"));

		verify(productService).searchProducts("Laptop");
	}

	@Test
	void getLowStockProducts_shouldUseDefaultThreshold() throws Exception {
		when(productService.getLowStockProducts(10)).thenReturn(List.of(createResponse()));

		mockMvc.perform(get("/api/products/low-stock")).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Laptop"));

		verify(productService).getLowStockProducts(10);
	}

	@Test
	void getLowStockProducts_shouldAcceptCustomThreshold() throws Exception {
		when(productService.getLowStockProducts(5)).thenReturn(List.of(createResponse()));

		mockMvc.perform(get("/api/products/low-stock").param("threshold", "5")).andExpect(status().isOk());

		verify(productService).getLowStockProducts(5);
	}

	@Test
	void updateProduct_shouldReturnUpdatedProduct() throws Exception {
		ProductRequest request = createRequest();

		when(productService.updateProduct(eq(1L), any(ProductRequest.class))).thenReturn(createResponse());

		mockMvc.perform(put("/api/products/{id}", 1L).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Laptop")).andExpect(jsonPath("$.sku").value("LAP-001"));

		verify(productService).updateProduct(eq(1L), any(ProductRequest.class));
	}

	@Test
	void deleteProduct_shouldReturnNoContent() throws Exception {
		doNothing().when(productService).deleteProduct(1L);

		mockMvc.perform(delete("/api/products/{id}", 1L)).andExpect(status().isNoContent())
				.andExpect(content().string(""));

		verify(productService).deleteProduct(1L);
	}

	// --------------------------------------------------
	// Helper methods
	// --------------------------------------------------

	private ProductRequest createRequest() {
		ProductRequest request = new ProductRequest();
		request.setName("Laptop");
		request.setSku("LAP-001");
		request.setPrice(new BigDecimal("50000.00"));
		request.setQuantity(20);
		return request;
	}

	private ProductResponse createResponse() {
		Product product = new Product();
		product.setId(1L);
		product.setName("Laptop");
		product.setSku("LAP-001");
		product.setPrice(new BigDecimal("50000.00"));
		product.setQuantity(20);

		return new ProductResponse(product);
	}

}
