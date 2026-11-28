import test from "node:test";
import assert from "node:assert/strict";
import { receiptDocument } from "./receiptDocument.js";

const receipt = {
  orderId: "ORD-receipt", createdAt: "2026-11-28T22:54:42",
  customerName: "Customer", phoneNumber: "9800000000", currency: "NPR",
  paymentMethod: "STRIPE", paymentStatus: "PAID", paymentReference: "cs_test_receipt",
  subTotal: "115.00", tax: "2.30", grandTotal: "117.30",
  items: [{ itemId: "tea", name: "Tea", quantity: 5, unitPrice: "23.00", lineTotal: "115.00" }],
};

test("downloaded receipt includes saved order, customer, payment, items, and exact displayed totals", () => {
  const document = receiptDocument(receipt);
  for (const expected of ["ORD-receipt", "2026-11-28 22:54:42", "Customer", "9800000000",
    "STRIPE", "PAID", "cs_test_receipt", "Tea", "NPR", "Rs. 23.00", "Rs. 115.00", "Rs. 2.30", "Rs. 117.30"]) {
    assert.ok(document.includes(expected), expected);
  }
  assert.ok(document.includes('charset="utf-8"'));
  assert.ok(document.includes("@media print"));
});

test("receipt safely escapes customer and catalog text instead of creating executable HTML", () => {
  const document = receiptDocument({ ...receipt, customerName: '<script>alert("customer")</script>',
    items: [{ ...receipt.items[0], name: '<img src=x onerror="alert(1)">' }] });
  assert.ok(!document.includes("<script>"));
  assert.ok(!document.includes("<img"));
  assert.ok(document.includes("&lt;script&gt;"));
  assert.ok(document.includes("&lt;img"));
});

test("cash receipts do not invent a provider reference", () => {
  const document = receiptDocument({ ...receipt, paymentMethod: "CASH", paymentReference: null });
  assert.ok(document.includes("CASH"));
  assert.ok(document.includes("Not applicable"));
  assert.ok(!document.includes("cs_test_receipt"));
});
