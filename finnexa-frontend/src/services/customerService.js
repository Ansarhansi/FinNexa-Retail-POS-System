import api from "./api";

// Get all customers
export const getCustomers = async () => {
  const response = await api.get("/customers");
  return response.data;
};

// Create customer
export const createCustomer = async (customer) => {
  const response = await api.post("/customers", customer);
  return response.data;
};

// Update customer
export const updateCustomer = async (id, customer) => {
  const response = await api.put(`/customers/${id}`, customer);
  return response.data;
};

// Delete customer
export const deleteCustomer = async (id) => {
  const response = await api.delete(`/customers/${id}`);
  return response.data;
};
