import api from "./api";

export const getAuditLogs = async () => {
  const response = await api.get("/audit-logs");
  return response.data;
};

export const getAuditLogsByUsername = async (username) => {
  const response = await api.get(
    `/audit-logs/user/${encodeURIComponent(username)}`,
  );
  return response.data;
};

export const getAuditLogsByAction = async (action) => {
  const response = await api.get("/audit-logs/action", {
    params: { action },
  });
  return response.data;
};

export const createAuditLog = async (auditLogData) => {
  const response = await api.post("/audit-logs", auditLogData);
  return response.data;
};
