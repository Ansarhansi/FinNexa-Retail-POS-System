import api from "./api";

// LOGIN
export const login = async (email, password) => {
  sessionStorage.removeItem("finnexa_credentials");
  sessionStorage.removeItem("finnexa_user");

  try {
    const normalizedEmail = email.trim();

    const response = await api.post("/auth/login", {
      email: normalizedEmail,
      password,
    });

    sessionStorage.setItem(
      "finnexa_credentials",
      btoa(`${normalizedEmail}:${password}`),
    );

    sessionStorage.setItem("finnexa_user", JSON.stringify(response.data));

    return response.data;
  } catch (error) {
    sessionStorage.removeItem("finnexa_credentials");
    sessionStorage.removeItem("finnexa_user");
    throw error;
  }
};

// UPDATE PROFILE
export const updateProfile = async (profileData) => {
  const response = await api.patch("/auth/profile", profileData);

  const credentials = sessionStorage.getItem("finnexa_credentials");

  if (credentials) {
    const decoded = atob(credentials);
    const separatorIndex = decoded.indexOf(":");
    const password = decoded.substring(separatorIndex + 1);

    sessionStorage.setItem(
      "finnexa_credentials",
      btoa(`${response.data.email}:${password}`),
    );
  }

  sessionStorage.setItem("finnexa_user", JSON.stringify(response.data));

  return response.data;
};

// CHANGE PASSWORD
export const changePassword = async (passwordData) => {
  const response = await api.patch("/auth/change-password", passwordData);

  const credentials = sessionStorage.getItem("finnexa_credentials");

  if (credentials) {
    const decoded = atob(credentials);
    const separatorIndex = decoded.indexOf(":");
    const email = decoded.substring(0, separatorIndex);

    sessionStorage.setItem(
      "finnexa_credentials",
      btoa(`${email}:${passwordData.newPassword}`),
    );
  }

  return response.data;
};

// LOGOUT
export const logout = () => {
  sessionStorage.removeItem("finnexa_credentials");
  sessionStorage.removeItem("finnexa_user");
};
