import api from "./api";
export const getProducts = async () => {
  const response = await api.get("/products");
  return response.data;
};
export const getProductsPage = async ({
  page = 0,
  size = 10,
  sortBy = "id",
  direction = "asc",
} = {}) => {
  const response = await api.get("/products/page", {
    params: { page, size, sortBy, direction },
  });
  return response.data;
};
export const createProduct = async (product) => {
  const response = await api.post("/products", product);
  return response.data;
};
export const updateProduct = async (id, product) => {
  const response = await api.put(`/products/${id}`, product);
  return response.data;
};
export const deleteProduct = async (id) => {
  const response = await api.delete(`/products/${id}`);
  return response.data;
};
