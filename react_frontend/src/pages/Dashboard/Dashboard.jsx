import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { DashboardAPI } from "../../api/DashboardAPI";
import { formatNpr } from "../../utils/currency.js";
import OrderReceipt from "../../components/OrderReceipt/OrderReceipt";
import "./Dashboard.css";

export const Dashboard = () => {
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [refreshKey, setRefreshKey] = useState(0);
  const [receiptOrderId, setReceiptOrderId] = useState(null);

  useEffect(() => {
    let ignore = false;
    DashboardAPI.getSummary()
      .then(({ data }) => {
        if (data.currency !== "NPR" || !Array.isArray(data.recentOrders)) {
          throw new Error("Invalid dashboard response");
        }
        if (!ignore) setSummary(data);
      })
      .catch((dashboardError) => {
        if (!ignore) setError(dashboardError.response?.status === 401
          ? "Please sign in to view the dashboard."
          : dashboardError.response?.data?.detail || "Could not load the dashboard. Please try again.");
      })
      .finally(() => { if (!ignore) setLoading(false); });
    return () => { ignore = true; };
  }, [refreshKey]);

  const refresh = () => {
    setLoading(true);
    setError("");
    setRefreshKey((value) => value + 1);
  };

  return (
    <main className="dashboard-page">
      <header className="dashboard-header">
        <div><h1>Dashboard</h1><p>A quick overview of your billing activity.</p></div>
        <div className="dashboard-actions">
          <Link to="/explore" className="dashboard-new-sale">New sale</Link>
          <button type="button" onClick={refresh} disabled={loading}>{loading ? "Loading..." : "Refresh"}</button>
        </div>
      </header>

      {loading && <p role="status">Loading dashboard...</p>}
      {error && <p role="alert" className="dashboard-error">{error}</p>}

      {!loading && !error && summary && <>
        <p className="dashboard-date">{summary.date} · Nepal time · NPR</p>
        <section className="dashboard-cards" aria-label="Billing summary">
          <div className="dashboard-card"><h2>Today's sales</h2><strong>{formatNpr(summary.todaySales)}</strong>
            <p>{summary.todayPaidOrders} paid orders created today</p></div>
          <div className="dashboard-card"><h2>Total sales</h2><strong>{formatNpr(summary.totalSales)}</strong>
            <p>All paid orders, including tax</p></div>
          <div className="dashboard-card"><h2>Paid orders</h2><strong>{summary.paidOrders}</strong>
            <p>Completed cash and online payments</p></div>
          <div className="dashboard-card"><h2>Pending payments</h2><strong>{summary.pendingPayments}</strong>
            <p>Orders awaiting payment confirmation</p></div>
          <div className="dashboard-card"><h2>Catalog items</h2><strong>{summary.totalItems}</strong>
            <p>Items available in your catalog</p></div>
          <div className="dashboard-card"><h2>Categories</h2><strong>{summary.totalCategories}</strong>
            <p>Categories used to organize items</p></div>
        </section>

        <section className="dashboard-recent">
          <div className="dashboard-section-header"><h2>Recent paid orders</h2><Link to="/order-history">View all orders</Link></div>
          {summary.recentOrders.length === 0 ? <p>No paid orders yet. Start a new sale to get going.</p> : (
            <div className="dashboard-table-container">
              <table><thead><tr><th scope="col">Order ID</th><th scope="col">Order date</th>
                <th scope="col">Customer</th><th scope="col">Payment</th><th scope="col">Total</th><th scope="col">Receipt</th></tr></thead>
                <tbody>{summary.recentOrders.map((order) => (
                  <tr key={order.orderId}><th scope="row">{order.orderId}</th>
                    <td>{order.createdAt?.replace("T", " ").slice(0, 19) || "—"}</td>
                    <td>{order.customerName || "—"}</td><td>{order.paymentMethod || "—"}</td>
                    <td className="dashboard-order-total">{formatNpr(order.grandTotal)}</td>
                    <td><button type="button" onClick={() => setReceiptOrderId(order.orderId)}
                      aria-label={`View receipt for ${order.orderId}`}>View receipt</button></td></tr>
                ))}</tbody>
              </table>
            </div>
          )}
        </section>
        <nav className="dashboard-shortcuts" aria-label="Dashboard shortcuts">
          <Link to="/explore">Start a new sale</Link><Link to="/items">Manage items</Link>
          <Link to="/categories">Manage categories</Link><Link to="/order-history">Order history</Link>
        </nav>
        <p className="dashboard-note">Sales include paid orders only. Today's figures use the order creation date in Nepal time.</p>
      </>}
      {receiptOrderId && <OrderReceipt key={receiptOrderId} orderId={receiptOrderId}
        onClose={() => setReceiptOrderId(null)} />}
    </main>
  );
};
