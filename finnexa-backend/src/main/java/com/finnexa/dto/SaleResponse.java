
package com.finnexa.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.finnexa.entity.Sale;
import com.finnexa.entity.SaleItem;

public class SaleResponse {

	private Long id;
	private String invoiceNumber;
	private BigDecimal totalAmount;
	private Sale.PaymentMethod paymentMethod;
	private Sale.SaleStatus status;
	private LocalDateTime saleDate;
	private List<SaleItemResponse> items;

	public SaleResponse() {
	}

	public SaleResponse(Sale sale) {
		this.id = sale.getId();
		this.invoiceNumber = sale.getInvoiceNumber();
		this.totalAmount = sale.getTotalAmount();
		this.paymentMethod = sale.getPaymentMethod();
		this.status = sale.getStatus();
		this.saleDate = sale.getSaleDate();

		this.items = sale.getItems().stream().map(SaleItemResponse::new).toList();
	}

	public Long getId() {
		return id;
	}

	public String getInvoiceNumber() {
		return invoiceNumber;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public Sale.PaymentMethod getPaymentMethod() {
		return paymentMethod;
	}

	public Sale.SaleStatus getStatus() {
		return status;
	}

	public LocalDateTime getSaleDate() {
		return saleDate;
	}

	public List<SaleItemResponse> getItems() {
		return items;
	}

	public static class SaleItemResponse {

		private Long productId;
		private String productName;
		private String sku;
		private Integer quantity;
		private BigDecimal unitPrice;
		private BigDecimal subtotal;

		public SaleItemResponse() {
		}

		public SaleItemResponse(SaleItem item) {
			this.productId = item.getProduct().getId();
			this.productName = item.getProduct().getName();
			this.sku = item.getProduct().getSku();
			this.quantity = item.getQuantity();
			this.unitPrice = item.getUnitPrice();
			this.subtotal = item.getSubtotal();
		}

		public Long getProductId() {
			return productId;
		}

		public String getProductName() {
			return productName;
		}

		public String getSku() {
			return sku;
		}

		public Integer getQuantity() {
			return quantity;
		}

		public BigDecimal getUnitPrice() {
			return unitPrice;
		}

		public BigDecimal getSubtotal() {
			return subtotal;
		}
	}
}
