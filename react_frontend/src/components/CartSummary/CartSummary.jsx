import { useContext } from "react";
import { AppContext } from "../../Context/AppContext";
import "./CartSummary.css";

const CartSummary = () => {
  const { cartItems } = useContext(AppContext);
  const taxRate = 2;
  const subtotalCents = cartItems.reduce(
    (total, item) =>
      total + Math.round(Number(item.price) * 100) * item.quantity,
    0,
  );
  const taxCents = Math.round((subtotalCents * taxRate) / 100);
  const grandTotalCents = subtotalCents + taxCents;
  const formatPrice = (cents) => (cents / 100).toFixed(2);

  return (
    <section className="cart-summary">
      <h2>Order summary</h2>
      <div className="cart-summary-row">
        <span>Subtotal</span>
        <span>${formatPrice(subtotalCents)}</span>
      </div>
      <div className="cart-summary-row">
        <span>Tax ({taxRate}%)</span>
        <span>${formatPrice(taxCents)}</span>
      </div>
      <div className="cart-summary-row cart-summary-total">
        <span>Grand total</span>
        <span>${formatPrice(grandTotalCents)}</span>
      </div>
    </section>
  );
};

export default CartSummary;