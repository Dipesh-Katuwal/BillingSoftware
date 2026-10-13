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
      .post(`${BASE}/addCategory`, formData, {
        headers: { "Content-Type": "multipart/form-data" },
      })
      .then((res) => res.data);
  },
  getCategories: () =>
    axiosClient.get(`${Base}/getCategories`).then((r) => r.data),
  deleteCategory: (categoryId) =>
    axiosClient.delete(`${Base}/deleteCategory/${categoryId}`),
};
