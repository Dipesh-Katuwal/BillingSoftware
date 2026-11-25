import { axiosClient } from "./axiosClient";

export const ItemsAPI = {
  addItem: (itemRequest, file) => {
    const formData = new FormData();
    formData.append(
      "item",
      new Blob([JSON.stringify(itemRequest)], { type: "application/json" }),
    );
    if (file) {
      formData.append("file", file);
    }
    return axiosClient
      .post("/admin/addItem", formData, {
        headers: {
          "Content-Type": "multipart/form-data",
          Authorization: `Bearer ${localStorage.getItem("token")}`,
        },
      })
      .then((response) => response.data);
  },
  getItems: () =>
    axiosClient
      .get("/items", {
        headers: { Authorization: `Bearer ${localStorage.getItem("token")}` },
      })
      .then((response) => response.data),
  deleteItem: (itemId) =>
    axiosClient.delete(`/admin/item/${itemId}`, {
      headers: { Authorization: `Bearer ${localStorage.getItem("token")}` },
    }),
};