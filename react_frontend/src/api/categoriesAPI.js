import { axiosClient } from "./axiosClient";

const Base = "/categories";

export const categoriesAPI = {
  addCategory: (categoryRequest, file) => {
    const formData = new FormData();
    formData.append(
      "category",
      new Blob([JSON.stringify(categoryRequest)], { type: "application/json" }),
    );
    if (file) {
      formData.append("file", file);
    }
    return axiosClient
      .post(`/admin${Base}/addCategory`, formData, {
        headers: {
          "Content-Type": "multipart/form-data",
          Authorization: `Bearer ${localStorage.getItem("token")}`,
        },
      })
      .then((res) => res.data);
  },
  getCategories: () =>
    axiosClient
      .get(`${Base}/getCategories`, {
        headers: { Authorization: `Bearer ${localStorage.getItem("token")}` },
      })
      .then((r) => r.data),
  deleteCategory: (categoryId) =>
    axiosClient.delete(`/admin${Base}/deleteCategory/${categoryId}`, {
      headers: { Authorization: `Bearer ${localStorage.getItem("token")}` },
    }),
};
