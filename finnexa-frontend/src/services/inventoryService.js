import api from "./api";

// Get all inventory records
export const getInventory = async () => {
  const response = await api.get("/inventory");
  return response.data;
};

// Get inventory for a specific product
export const getInventoryByProductId = async (productId) => {
  const response = await api.get(`/inventory/${productId}`);
  return response.data;
};

// Get products with low stock
export const getLowStockInventory = async () => {
  const response = await api.get("/inventory/low-stock");
  return response.data;
};

// Get inventory summary
export const getInventorySummary = async () => {
  const response = await api.get("/inventory/summary");
  return response.data;
};

// Get total inventory selling value
export const getTotalInventorySellingValue = async () => {
  const response = await api.get("/inventory/total-value");
  return response.data;
};

// Get total product count
export const getTotalProductCount = async () => {
  const response = await api.get("/inventory/count");
  return response.data;
};

// Get low-stock product count
export const getLowStockProductCount = async () => {
  const response = await api.get("/inventory/low-stock/count");
  return response.data;
};
