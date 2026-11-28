import { useEffect, useState } from "react";
import { PaymentAPI } from "../../api/PaymentAPI";
import { formatNpr } from "../../utils/currency.js";
import OrderReceipt from "../../components/OrderReceipt/OrderReceipt";
import "./OrderHistory.css";

const OrderHistory = () => {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [refreshKey, setRefreshKey] = useState(0);
  const [search, setSearch] = useState("");
  const [receiptOrderId, setReceiptOrderId] = useState(null);

  useEffect(() => {
    let ignore = false;
    PaymentAPI.getPaidOrders()
      .then(({ data }) => {
        if (!Array.isArray(data)) throw new Error("Invalid order history response");
        if (!ignore) {
          setOrders(data);
          setError("");
        }
      })
      .catch((historyError) => {
        if (!ignore) setError(historyError.response?.status === 401
          ? "Please sign in to view order history."
          : historyError.response?.data?.detail || "Could not load order history. Please try again.");
      })
      .finally(() => { if (!ignore) setLoading(false); });
    return () => { ignore = true; };
  }, [refreshKey]);

  const refresh = () => {
    setLoading(true);
    setError("");
    setRefreshKey((value) => value + 1);
  };
  const query = search.trim().toLowerCase();
  const visibleOrders = orders.filter((order) =>
    [order.orderId, order.customerName, order.phoneNumber]
      .some((value) => String(value ?? "").toLowerCase().includes(query)));

  return (
    <main className="order-history">
      <header className="order-history-header">
        <div><h1>Order history</h1><p>Paid orders · Newest first · NPR</p></div>
        <button type="button" onClick={refresh} disabled={loading}>
          {loading ? "Loading..." : "Refresh orders"}
        </button>
      </header>
      <label className="order-history-search">
        Search orders
        <input type="search" placeholder="Order ID, customer name or phone"
          value={search} onChange={(event) => setSearch(event.target.value)} />
      </label>
      {error && <p className="order-history-error" role="alert">{error}</p>}
      {loading && <p role="status">Loading paid orders...</p>}
      {!loading && !error && <>
        <p className="order-history-count">{visibleOrders.length} of {orders.length} paid orders</p>
        {visibleOrders.length === 0 ? (
          <p>{orders.length === 0 ? "No paid orders yet." : "No orders match your search."}</p>
        ) : (
          <div className="order-history-table-container">
            <table>
              <thead><tr><th scope="col">Order ID</th><th scope="col">Order date</th>
                <th scope="col">Customer</th><th scope="col">Phone</th><th scope="col">Payment</th>
                <th scope="col">Status</th><th scope="col">Total</th><th scope="col">Receipt</th></tr></thead>
              <tbody>{visibleOrders.map((order) => (
                <tr key={order.orderId}>
                  <th scope="row">{order.orderId}</th>
                  <td>{order.createdAt?.replace("T", " ") || "—"}</td>
                  <td>{order.customerName || "—"}</td><td>{order.phoneNumber || "—"}</td>
                  <td>{order.paymentMethod || "—"}</td>
                  <td><span className="order-history-paid">{order.paymentStatus}</span></td>
                  <td className="order-history-total">{formatNpr(order.grandTotal)}</td>
                  <td><button type="button" onClick={() => setReceiptOrderId(order.orderId)}
                    aria-label={`View receipt for ${order.orderId}`}>View receipt</button></td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        )}
      </>}
      {receiptOrderId && <OrderReceipt key={receiptOrderId} orderId={receiptOrderId}
        onClose={() => setReceiptOrderId(null)} />}
    </main>
  );
};

export default OrderHistory;
