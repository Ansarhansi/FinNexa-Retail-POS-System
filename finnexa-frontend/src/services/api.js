import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8081/api",
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use(
  (config) => {
    // Login is public; never attach previously stored credentials.
    if (config.url?.includes("/auth/login")) {
      delete config.headers.Authorization;
      return config;
    }

    const credentials = sessionStorage.getItem("finnexa_credentials");

    if (credentials) {
      config.headers.Authorization = `Basic ${credentials}`;
    } else {
      delete config.headers.Authorization;
    }

    return config;
  },
  (error) => Promise.reject(error),
);

export default api;
