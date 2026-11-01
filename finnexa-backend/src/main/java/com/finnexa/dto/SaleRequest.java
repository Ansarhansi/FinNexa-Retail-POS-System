
package com.finnexa.dto;

import java.util.List;

import com.finnexa.entity.Sale;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class SaleRequest {

	@NotNull(message = "Payment method is required")
	private Sale.PaymentMethod paymentMethod;

	@NotEmpty(message = "A sale must contain at least one item")
	@Valid
	private List<SaleItemRequest> items;

	public SaleRequest() {
	}

	public Sale.PaymentMethod getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(Sale.PaymentMethod paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public List<SaleItemRequest> getItems() {
		return items;
	}

	public void setItems(List<SaleItemRequest> items) {
		this.items = items;
	}

	public static class SaleItemRequest {

        @NotNull(message = "Product ID is required")
        @Positive(message = "Product ID must be positive")
        private Long productId;

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than zero")
        private Integer quantity;

        public SaleItemRequest() {
        }

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}
