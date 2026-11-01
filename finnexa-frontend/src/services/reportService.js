import api from "./api";

// Fetch all sales
export const getSalesReport = async () => {
  const response = await api.get("/sales");
  return response.data;
};

// Fetch all purchases
export const getPurchasesReport = async () => {
  const response = await api.get("/purchases");
  return response.data;
};

// Fetch all expenses
export const getExpensesReport = async () => {
  const response = await api.get("/expenses");
  return response.data;
};

// Fetch inventory records
export const getInventoryReport = async () => {
  const response = await api.get("/inventory");
  return response.data;
};

// Fetch inventory summary
export const getInventorySummaryReport = async () => {
  const response = await api.get("/inventory/summary");
  return response.data;
};

// Fetch sales for a date range
export const getSalesByDateRange = async (startDate, endDate) => {
  const response = await api.get("/sales/date-range", {
    params: { startDate, endDate },
  });

  return response.data;
};

// Fetch expenses for a date range
export const getExpensesByDateRange = async (startDate, endDate) => {
  const response = await api.get("/expenses/date-range", {
    params: { startDate, endDate },
  });

  return response.data;
};
