import api from "./api";

export const getSales = async () => {
  const response = await api.get("/sales");
  return response.data;
};

export const getSaleById = async (id) => {
  const response = await api.get(`/sales/${id}`);
  return response.data;
};

export const createSale = async (saleData) => {
  const response = await api.post("/sales", saleData);
  return response.data;
};

export const cancelSale = async (id) => {
  const response = await api.put(`/sales/${id}/cancel`);
  return response.data;
};

export const getSaleByInvoiceNumber = async (invoiceNumber) => {
  const response = await api.get("/sales/invoice", {
    params: { invoiceNumber },
  });
  return response.data;
};

export const getSalesByDateRange = async (startDate, endDate) => {
  const response = await api.get("/sales/date-range", {
    params: { startDate, endDate },
  });
  return response.data;
};
