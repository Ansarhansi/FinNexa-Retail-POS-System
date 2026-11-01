import api from "./api";

// Get all purchases
export const getPurchases = async () => {
  const response = await api.get("/purchases");
  return response.data;
};

// Get purchase by ID
export const getPurchaseById = async (id) => {
  const response = await api.get(`/purchases/${id}`);
  return response.data;
};

// Get purchase by purchase number
export const getPurchaseByNumber = async (purchaseNumber) => {
  const response = await api.get("/purchases/number", {
    params: { purchaseNumber },
  });
  return response.data;
};

// Get purchases by supplier
export const getPurchasesBySupplier = async (supplierId) => {
  const response = await api.get(`/purchases/supplier/${supplierId}`);
  return response.data;
};

// Create a purchase
export const createPurchase = async (purchaseData) => {
  const response = await api.post("/purchases", purchaseData);
  return response.data;
};

// Receive a purchase
export const receivePurchase = async (id) => {
  const response = await api.post(`/purchases/${id}/receive`);
  return response.data;
};

// Cancel a pending purchase
export const cancelPurchase = async (id) => {
  const response = await api.post(`/purchases/${id}/cancel`);
  return response.data;
};
