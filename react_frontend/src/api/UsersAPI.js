import { axiosClient } from "./axiosClient";

export const UsersAPI = {
  addUser: (userRequest) =>
    axiosClient
      .post("/admin/register", userRequest, {
        headers: {
          Authorization: `Bearer ${localStorage.getItem("token")}`,
        },
      })
      .then((response) => response.data),
  getUsers: () =>
    axiosClient
      .get("/admin/users", {
        headers: { Authorization: `Bearer ${localStorage.getItem("token")}` },
      })
      .then((response) => response.data),
  deleteUser: (userId) =>
    axiosClient.delete(`/admin/user/${userId}`, {
      headers: { Authorization: `Bearer ${localStorage.getItem("token")}` },
    }),
};