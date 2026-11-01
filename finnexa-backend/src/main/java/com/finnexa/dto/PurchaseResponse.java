package com.finnexa.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.finnexa.entity.Purchase;
import com.finnexa.entity.PurchaseItem;

public class PurchaseResponse {

	private Long id;
	private String purchaseNumber;
	private Long supplierId;
	private String supplierName;
	private BigDecimal totalAmount;
	private Purchase.PurchaseStatus status;
	private LocalDateTime purchaseDate;
	private List<PurchaseItemResponse> items;

	public PurchaseResponse() {
	}

	public PurchaseResponse(Purchase purchase) {
		this.id = purchase.getId();
		this.purchaseNumber = purchase.getPurchaseNumber();
		this.supplierId = purchase.getSupplier().getId();
		this.supplierName = purchase.getSupplier().getName();
		this.totalAmount = purchase.getTotalAmount();
		this.status = purchase.getStatus();
		this.purchaseDate = purchase.getPurchaseDate();

		this.items = purchase.getItems().stream().map(PurchaseItemResponse::new).toList();
	}

	public Long getId() {
		return id;
	}

	public String getPurchaseNumber() {
		return purchaseNumber;
	}

	public Long getSupplierId() {
		return supplierId;
	}

	public String getSupplierName() {
		return supplierName;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public Purchase.PurchaseStatus getStatus() {
		return status;
	}

	public LocalDateTime getPurchaseDate() {
		return purchaseDate;
	}

	public List<PurchaseItemResponse> getItems() {
		return items;
	}

	public static class PurchaseItemResponse {

		private Long productId;
		private String productName;
		private String sku;
		private Integer quantity;
		private BigDecimal unitCost;
		private BigDecimal subtotal;

		public PurchaseItemResponse() {
		}

		public PurchaseItemResponse(PurchaseItem item) {
			this.productId = item.getProduct().getId();
			this.productName = item.getProduct().getName();
			this.sku = item.getProduct().getSku();
			this.quantity = item.getQuantity();
			this.unitCost = item.getUnitCost();
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

		public BigDecimal getUnitCost() {
			return unitCost;
		}

		public BigDecimal getSubtotal() {
			return subtotal;
		}
	}

}