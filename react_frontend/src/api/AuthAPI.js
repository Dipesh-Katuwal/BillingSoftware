import { axiosClient } from "./axiosClient";

export const AuthAPI = {
  login : (data) => axiosClient.post("/login",data)
}