import { axiosClient } from "./axiosClient";

export const AuthAPI = {
  currentUser: () => axiosClient.get("/me", {
    headers: { Authorization: `Bearer ${localStorage.getItem("token")}` },
    timeout: 15000,
  }),
  login : (data) => axiosClient.post("/login",data)
}
