import api from "./api";

// Fetch all suppliers
export const getSuppliers = async () => {
  const response = await api.get("/suppliers");
  return response.data;
};

// Create a new supplier
export const createSupplier = async (supplier) => {
  const response = await api.post("/suppliers", supplier);
  return response.data;
};

// Update an existing supplier
export const updateSupplier = async (id, supplier) => {
  const response = await api.put(`/suppliers/${id}`, supplier);
  return response.data;
};

// Delete a supplier
export const deleteSupplier = async (id) => {
  const response = await api.delete(`/suppliers/${id}`);
  return response.data;
};
