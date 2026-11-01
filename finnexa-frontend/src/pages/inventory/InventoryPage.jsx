import { useCallback, useEffect, useMemo, useState } from "react";
import {
  getInventory,
  getInventorySummary,
  getTotalInventorySellingValue,
  getTotalProductCount,
  getLowStockProductCount,
} from "../../services/inventoryService";
import "./InventoryPage.css";

const formatCurrency = (value) =>
  new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2,
  }).format(Number(value) || 0);

const getQuantity = (item) =>
  Number(
    item.quantity ??
      item.stockQuantity ??
      item.currentStock ??
      item.availableQuantity ??
      0
  );

const getProductName = (item) =>
  item.productName ?? item.name ?? item.product?.name ?? "Unknown Product";

const getSku = (item) => item.sku ?? item.productSku ?? item.product?.sku ?? "—";

const getStockStatus = (item) => {
  const quantity = getQuantity(item);
  const reorderLevel = Number(
    item.reorderLevel ?? item.minimumStockLevel ?? item.product?.reorderLevel ?? 5
  );

  if (quantity <= 0) {
    return { label: "Out of Stock", className: "out-of-stock" };
  }

  if (item.lowStock === true || quantity <= reorderLevel) {
    return { label: "Low Stock", className: "low-stock" };
  }

  return { label: "In Stock", className: "in-stock" };
};

export default function InventoryPage() {
  const [inventory, setInventory] = useState([]);
  const [summary, setSummary] = useState(null);
  const [totalValue, setTotalValue] = useState(0);
  const [productCount, setProductCount] = useState(0);
  const [lowStockCount, setLowStockCount] = useState(0);

  const [search, setSearch] = useState("");
  const [stockFilter, setStockFilter] = useState("ALL");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState("");

  const loadInventory = useCallback(async (showRefresh = false) => {
    if (showRefresh) {
      setRefreshing(true);
    } else {
      setLoading(true);
    }

    setError("");

    try {
      const results = await Promise.allSettled([
        getInventory(),
        getInventorySummary(),
        getTotalInventorySellingValue(),
        getTotalProductCount(),
        getLowStockProductCount(),
      ]);

      const [inventoryResult, summaryResult, valueResult, countResult, lowResult] =
        results;

      if (inventoryResult.status === "rejected") {
        throw inventoryResult.reason;
      }

      const inventoryData = inventoryResult.value;
      setInventory(Array.isArray(inventoryData) ? inventoryData : []);

      if (summaryResult.status === "fulfilled") {
        setSummary(summaryResult.value);
      }

      if (valueResult.status === "fulfilled") {
        setTotalValue(Number(valueResult.value) || 0);
      }

      if (countResult.status === "fulfilled") {
        setProductCount(Number(countResult.value) || 0);
      } else if (Array.isArray(inventoryData)) {
        setProductCount(inventoryData.length);
      }

      if (lowResult.status === "fulfilled") {
        setLowStockCount(Number(lowResult.value) || 0);
      }
    } catch (err) {
      setError(
        err.response?.data?.message ||
          err.message ||
          "Unable to load inventory. Please try again."
      );
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    loadInventory();
  }, [loadInventory]);

  const filteredInventory = useMemo(() => {
    const query = search.trim().toLowerCase();

    return inventory.filter((item) => {
      const name = getProductName(item).toLowerCase();
      const sku = String(getSku(item)).toLowerCase();
      const status = getStockStatus(item);

      const matchesSearch =
        !query || name.includes(query) || sku.includes(query);

      const matchesFilter =
        stockFilter === "ALL" ||
        (stockFilter === "LOW" &&
          status.label === "Low Stock") ||
        (stockFilter === "OUT" &&
          status.label === "Out of Stock") ||
        (stockFilter === "IN" &&
          status.label === "In Stock");

      return matchesSearch && matchesFilter;
    });
  }, [inventory, search, stockFilter]);

  const totalPages = Math.max(
    1,
    Math.ceil(filteredInventory.length / pageSize)
  );

  const paginatedInventory = filteredInventory.slice(
    (currentPage - 1) * pageSize,
    currentPage * pageSize
  );

  useEffect(() => {
    setCurrentPage(1);
  }, [search, stockFilter, pageSize]);

  const displayedProductCount =
    summary?.totalProducts ??
    summary?.productCount ??
    productCount;

  const displayedLowStockCount =
    summary?.lowStockProducts ??
    summary?.lowStockCount ??
    lowStockCount;

  const displayedValue =
    summary?.totalInventoryValue ??
    summary?.totalSellingValue ??
    totalValue;

  const totalUnits = inventory.reduce(
    (total, item) => total + getQuantity(item),
    0
  );

  if (loading) {
    return (
      <div className="inventory-page">
        <div className="inventory-loading">
          <div className="inventory-spinner" />
          <p>Loading inventory...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="inventory-page">
      <div className="inventory-page-header">
        <div>
          <div className="inventory-breadcrumb">
            <span>FinNexa</span>
            <span>/</span>
            <span>Inventory</span>
          </div>

          <h1>Inventory Management</h1>
          <p className="inventory-subtitle">
            Monitor stock levels and track your inventory at a glance.
          </p>
        </div>

        <button
          type="button"
          className="inventory-refresh-btn"
          onClick={() => loadInventory(true)}
          disabled={refreshing}
        >
          <i
            className={`bi bi-arrow-clockwise ${
              refreshing ? "inventory-rotating" : ""
            }`}
          />
          {refreshing ? "Refreshing..." : "Refresh"}
        </button>
      </div>

      {error && (
        <div className="inventory-error" role="alert">
          <i className="bi bi-exclamation-circle" />
          <span>{error}</span>
          <button type="button" onClick={() => loadInventory(true)}>
            Retry
          </button>
        </div>
      )}

      <div className="inventory-stats-grid">
        <div className="inventory-stat-card">
          <div className="inventory-stat-top">
            <span className="inventory-stat-label">Total Products</span>
            <span className="inventory-stat-icon products-icon">
              <i className="bi bi-box-seam" />
            </span>
          </div>
          <h2>{displayedProductCount}</h2>
          <p>Products tracked in inventory</p>
        </div>

        <div className="inventory-stat-card">
          <div className="inventory-stat-top">
            <span className="inventory-stat-label">Total Stock Units</span>
            <span className="inventory-stat-icon units-icon">
              <i className="bi bi-stack" />
            </span>
          </div>
          <h2>{totalUnits.toLocaleString("en-IN")}</h2>
          <p>Units currently recorded</p>
        </div>

        <div className="inventory-stat-card">
          <div className="inventory-stat-top">
            <span className="inventory-stat-label">Low Stock Items</span>
            <span className="inventory-stat-icon low-icon">
              <i className="bi bi-exclamation-triangle" />
            </span>
          </div>
          <h2>{displayedLowStockCount}</h2>
          <p>Items requiring attention</p>
        </div>

        <div className="inventory-stat-card">
          <div className="inventory-stat-top">
            <span className="inventory-stat-label">Inventory Value</span>
            <span className="inventory-stat-icon value-icon">
              <i className="bi bi-currency-rupee" />
            </span>
          </div>
          <h2 className="inventory-value-number">
            {formatCurrency(displayedValue)}
          </h2>
          <p>Estimated selling value</p>
        </div>
      </div>

      <section className="inventory-table-card">
        <div className="inventory-table-heading">
          <div>
            <h2>Stock Overview</h2>
            <p>Review product availability and stock status.</p>
          </div>

          <span className="inventory-record-count">
            {filteredInventory.length} records
          </span>
        </div>

        <div className="inventory-toolbar">
          <div className="inventory-search">
            <i className="bi bi-search" />
            <input
              type="search"
              placeholder="Search by product name or SKU..."
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
            {search && (
              <button
                type="button"
                aria-label="Clear search"
                onClick={() => setSearch("")}
              >
                <i className="bi bi-x-lg" />
              </button>
            )}
          </div>

          <div className="inventory-filter">
            <i className="bi bi-funnel" />
            <select
              value={stockFilter}
              onChange={(event) => setStockFilter(event.target.value)}
              aria-label="Filter by stock status"
            >
              <option value="ALL">All stock</option>
              <option value="IN">In stock</option>
              <option value="LOW">Low stock</option>
              <option value="OUT">Out of stock</option>
            </select>
          </div>
        </div>

        <div className="inventory-table-wrapper">
          <table className="inventory-table">
            <thead>
              <tr>
                <th>PRODUCT</th>
                <th>SKU</th>
                <th>AVAILABLE STOCK</th>
                <th>STOCK STATUS</th>
                <th>UNIT PRICE</th>
              </tr>
            </thead>

            <tbody>
              {paginatedInventory.map((item, index) => {
                const status = getStockStatus(item);
                const productId =
                  item.productId ?? item.id ?? item.product?.id;

                const unitPrice =
                  item.sellingPrice ??
                  item.unitPrice ??
                  item.price ??
                  item.product?.sellingPrice ??
                  item.product?.price;

                return (
                  <tr key={productId ?? `${getSku(item)}-${index}`}>
                    <td>
                      <div className="inventory-product-cell">
                        <span className="inventory-product-icon">
                          <i className="bi bi-box" />
                        </span>
                        <div>
                          <strong>{getProductName(item)}</strong>
                          <small>
                            {productId != null
                              ? `Product ID: ${productId}`
                              : "Inventory item"}
                          </small>
                        </div>
                      </div>
                    </td>

                    <td>
                      <span className="inventory-sku">{getSku(item)}</span>
                    </td>

                    <td>
                      <strong className="inventory-quantity">
                        {getQuantity(item).toLocaleString("en-IN")}
                      </strong>
                      <span className="inventory-units-label"> units</span>
                    </td>

                    <td>
                      <span
                        className={`inventory-status ${status.className}`}
                      >
                        <span className="inventory-status-dot" />
                        {status.label}
                      </span>
                    </td>

                    <td>
                      {unitPrice != null
                        ? formatCurrency(unitPrice)
                        : "—"}
                    </td>
                  </tr>
                );
              })}

              {paginatedInventory.length === 0 && (
                <tr>
                  <td colSpan="5">
                    <div className="inventory-empty">
                      <span>
                        <i className="bi bi-inbox" />
                      </span>
                      <h3>No inventory records found</h3>
                      <p>
                        {search || stockFilter !== "ALL"
                          ? "Try changing your search or stock filter."
                          : "Inventory records will appear here when available."}
                      </p>
                      {(search || stockFilter !== "ALL") && (
                        <button
                          type="button"
                          onClick={() => {
                            setSearch("");
                            setStockFilter("ALL");
                          }}
                        >
                          Clear filters
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <div className="inventory-pagination">
          <div className="inventory-pagination-info">
            Showing{" "}
            {filteredInventory.length === 0
              ? 0
              : (currentPage - 1) * pageSize + 1}
            {"–"}
            {Math.min(currentPage * pageSize, filteredInventory.length)} of{" "}
            {filteredInventory.length}
          </div>

          <div className="inventory-pagination-controls">
            <label htmlFor="inventory-page-size">Rows:</label>
            <select
              id="inventory-page-size"
              value={pageSize}
              onChange={(event) => setPageSize(Number(event.target.value))}
            >
              <option value={5}>5</option>
              <option value={10}>10</option>
              <option value={20}>20</option>
              <option value={50}>50</option>
            </select>

            <button
              type="button"
              aria-label="Previous page"
              disabled={currentPage <= 1}
              onClick={() =>
                setCurrentPage((page) => Math.max(1, page - 1))
              }
            >
              <i className="bi bi-chevron-left" />
            </button>

            <span className="inventory-page-number">
              {currentPage} / {totalPages}
            </span>

            <button
              type="button"
              aria-label="Next page"
              disabled={currentPage >= totalPages}
              onClick={() =>
                setCurrentPage((page) => Math.min(totalPages, page + 1))
              }
            >
              <i className="bi bi-chevron-right" />
            </button>
          </div>
        </div>
      </section>

      <div className="inventory-page-footer">
        <i className="bi bi-info-circle" />
        Stock changes are managed through sales and purchase receiving.
        Inventory adjustment actions are not available through the current
        inventory API.
      </div>
    </div>
  );
}