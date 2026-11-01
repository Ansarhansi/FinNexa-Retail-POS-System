import api from "./api";

// Get all expenses
export const getExpenses = async () => {
  const response = await api.get("/expenses");
  return response.data;
};

// Get expense by ID
export const getExpenseById = async (id) => {
  const response = await api.get(`/expenses/${id}`);
  return response.data;
};

// Create a new expense
export const createExpense = async (expenseData) => {
  const response = await api.post("/expenses", expenseData);
  return response.data;
};

// Update an existing expense
export const updateExpense = async (id, expenseData) => {
  const response = await api.put(`/expenses/${id}`, expenseData);
  return response.data;
};

// Delete an expense
export const deleteExpense = async (id) => {
  const response = await api.delete(`/expenses/${id}`);
  return response.data;
};

// Search expenses by title
export const searchExpenses = async (title) => {
  const response = await api.get("/expenses/search", {
    params: { title },
  });
  return response.data;
};

// Get expenses by category
export const getExpensesByCategory = async (category) => {
  const response = await api.get("/expenses/category", {
    params: { category },
  });
  return response.data;
};

// Get expenses between two dates
export const getExpensesBetweenDates = async (startDate, endDate) => {
  const response = await api.get("/expenses/date-range", {
    params: { startDate, endDate },
  });
  return response.data;
};
