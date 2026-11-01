package com.finnexa.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.finnexa.entity.Product;

public class ProductResponse {

	private Long id;
	private String name;
	private String sku;
	private String description;
	private BigDecimal price;
	private Integer quantity;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public ProductResponse() {
	}

	public ProductResponse(Product product) {
		this.id = product.getId();
		this.name = product.getName();
		this.sku = product.getSku();
		this.description = product.getDescription();
		this.price = product.getPrice();
		this.quantity = product.getQuantity();
		this.createdAt = product.getCreatedAt();
		this.updatedAt = product.getUpdatedAt();
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getSku() {
		return sku;
	}

	public String getDescription() {
		return description;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

}