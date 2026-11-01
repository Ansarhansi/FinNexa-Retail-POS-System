import api from "./api";

// Fetch all users
export const getUsers = async () => {
  const response = await api.get("/users");
  return response.data;
};

// Fetch a user by ID
export const getUserById = async (userId) => {
  const response = await api.get(`/users/${userId}`);
  return response.data;
};

// Update a user's role
export const updateUserRole = async (userId, role) => {
  const response = await api.patch(`/users/${userId}/role`, {
    role,
  });

  return response.data;
};

// Enable or disable a user
export const updateUserStatus = async (userId, enabled) => {
  const response = await api.patch(`/users/${userId}/status`, {
    enabled,
  });

  return response.data;
};
