import { axiosClient } from "./axiosClient";

const authHeaders = () => ({
  Authorization: `Bearer ${localStorage.getItem("token")}`,
});

export const PaymentAPI = {
  getPaidOrders: () =>
    axiosClient.get("/orders/paid", { headers: authHeaders(), timeout: 15000 }),

  getReceipt: (orderId) =>
    axiosClient.get(`/orders/${encodeURIComponent(orderId)}/receipt`, {
      headers: authHeaders(),
      timeout: 15000,
    }),

  stripeOptions: () =>
    axiosClient.get("/payments/stripe/options", {
      headers: authHeaders(),
      timeout: 15000,
    }),

  initiateStripe: (orderId) =>
    axiosClient.post(`/payments/stripe/initiate/${orderId}`, null, {
      headers: authHeaders(),
      timeout: 90000,
    }),

  refreshStripeStatus: (orderId) =>
    axiosClient.get(`/payments/stripe/status/${orderId}`, {
      headers: authHeaders(),
      timeout: 90000,
    }),

  createOrder: (data) =>
    axiosClient.post("/orders", data, { headers: authHeaders(), timeout: 30000 }),

  createCashOrder: (data) =>
    axiosClient.post("/orders/cash", data, { headers: authHeaders(), timeout: 30000 }),

  initiateEsewa: (orderId) =>
    axiosClient.post(
      `/payments/esewa/initiate/${orderId}`,
      null,
      { headers: authHeaders() },
    ),

  initiateKhalti: (orderId) =>
    axiosClient.post(
      `/payments/khalti/initiate/${orderId}`,
      null,
      { headers: authHeaders() },
    ),

  refreshEsewaStatus: (orderId) =>
    axiosClient.get(`/payments/esewa/status/${orderId}`, {
      headers: authHeaders(),
    }),

  refreshKhaltiStatus: (orderId) =>
    axiosClient.get(`/payments/khalti/status/${orderId}`, {
      headers: authHeaders(),
    }),
};
