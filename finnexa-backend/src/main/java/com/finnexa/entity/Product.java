package com.finnexa.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "products")
public class Product {

@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

@NotBlank(message = "Product name is required")
@Size(max = 100, message = "Product name cannot exceed 100 characters")
@Column(nullable = false, length = 100)
private String name;

@NotBlank(message = "SKU is required")
@Size(max = 50, message = "SKU cannot exceed 50 characters")
@Column(nullable = false, unique = true, length = 50)
private String sku;

@Size(max = 500, message = "Description cannot exceed 500 characters")
@Column(length = 500)
private String description;

@NotNull(message = "Price is required")
@DecimalMin(value = "0.01", message = "Price must be greater than zero")
@Column(nullable = false, precision = 12, scale = 2)
private BigDecimal price;

@NotNull(message = "Quantity is required")
@PositiveOrZero(message = "Quantity cannot be negative")
@Column(nullable = false)
private Integer quantity;

@Column(nullable = false, updatable = false)
private LocalDateTime createdAt;

@Column(nullable = false)
private LocalDateTime updatedAt;

public Product() {
}

@PrePersist
protected void onCreate() {
    LocalDateTime now = LocalDateTime.now();
    createdAt = now;
    updatedAt = now;
}

@PreUpdate
protected void onUpdate() {
    updatedAt = LocalDateTime.now();
}

public Long getId() {
    return id;
}

public void setId(Long id) {
    this.id = id;
}

public String getName() {
    return name;
}

public void setName(String name) {
    this.name = name;
}

public String getSku() {
    return sku;
}

public void setSku(String sku) {
    this.sku = sku;
}

public String getDescription() {
    return description;
}

public void setDescription(String description) {
    this.description = description;
}

public BigDecimal getPrice() {
    return price;
}

public void setPrice(BigDecimal price) {
    this.price = price;
}

public Integer getQuantity() {
    return quantity;
}

public void setQuantity(Integer quantity) {
    this.quantity = quantity;
}

public LocalDateTime getCreatedAt() {
    return createdAt;
}

public LocalDateTime getUpdatedAt() {
    return updatedAt;
}

}