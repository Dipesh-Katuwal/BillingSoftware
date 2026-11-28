const escape = (value) => String(value ?? "?").replace(/[&<>"']/g, (character) => ({
  "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
})[character]);
import { formatNpr as money } from "../../utils/currency.js";

export const receiptDocument = (receipt) => {
  const rows = receipt.items.map((item) => `<tr>
    <td>${escape(item.name)}<small>${escape(item.itemId)}</small></td>
    <td class="number">${escape(item.quantity)}</td>
    <td class="number">${escape(money(item.unitPrice))}</td>
    <td class="number">${escape(money(item.lineTotal))}</td>
  </tr>`).join("");
  return `<!doctype html><html lang="en"><head><meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Receipt ${escape(receipt.orderId)}</title><style>
  *{box-sizing:border-box}body{font:14px/1.5 Arial,sans-serif;color:#20252b;background:#fff;margin:0;padding:28px}
  main{max-width:850px;margin:auto}h1{margin:0;font-size:28px}header{border-bottom:2px solid #222;padding-bottom:14px;margin-bottom:18px}
  .paid{font-weight:bold;color:#166534}dl{display:grid;grid-template-columns:150px 1fr;gap:6px 12px}dt{font-weight:bold}dd{margin:0;overflow-wrap:anywhere}
  table{width:100%;border-collapse:collapse;margin-top:24px}th,td{padding:10px 6px;border-bottom:1px solid #ddd;text-align:left;vertical-align:top}
  thead{display:table-header-group}tr{break-inside:avoid}small{display:block;color:#555}.number{text-align:right;white-space:nowrap}
  .totals{margin:20px 0 0 auto;max-width:340px}.totals div{display:flex;justify-content:space-between;gap:20px;padding:6px 0}
  .grand-total{font-weight:bold;font-size:18px;border-top:2px solid #222}footer{margin-top:30px;color:#555}
  @media print{body{padding:0}@page{margin:16mm}}@media(max-width:500px){body{padding:12px}dl{grid-template-columns:100px 1fr}th,td{padding:8px 3px;font-size:12px}}
  </style></head><body><main><header><h1>BillingSoftware receipt</h1><span class="paid">${escape(receipt.paymentStatus)}</span></header>
  <dl><dt>Order ID</dt><dd>${escape(receipt.orderId)}</dd>
  <dt>Order date</dt><dd>${escape(String(receipt.createdAt ?? "?").replace("T", " "))}</dd>
  <dt>Customer</dt><dd>${escape(receipt.customerName)}</dd>
  <dt>Phone</dt><dd>${escape(receipt.phoneNumber)}</dd>
  <dt>Currency</dt><dd>${escape(receipt.currency)}</dd>
  <dt>Payment method</dt><dd>${escape(receipt.paymentMethod)}</dd>
  <dt>Payment reference</dt><dd>${escape(receipt.paymentReference ?? "Not applicable")}</dd></dl>
  <table><thead><tr><th>Item</th><th class="number">Qty</th><th class="number">Unit price</th><th class="number">Amount</th></tr></thead><tbody>${rows}</tbody></table>
  <section class="totals"><div><span>Subtotal</span><span>${escape(money(receipt.subTotal))}</span></div>
  <div><span>Tax</span><span>${escape(money(receipt.tax))}</span></div>
  <div class="grand-total"><span>Grand total</span><span>${escape(money(receipt.grandTotal))}</span></div></section>
  <footer>Receipt generated from the saved, paid order.</footer></main></body></html>`;
};
