import { axiosClient } from "./axiosClient";

export const DashboardAPI = {
  getSummary: () => axiosClient.get("/dashboard", {
    headers: { Authorization: `Bearer ${localStorage.getItem("token")}` },
    timeout: 15000,
  }),
};
