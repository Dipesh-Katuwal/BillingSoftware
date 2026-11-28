import { useContext, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import { AppContext } from "../../Context/AppContext";
import { PaymentAPI } from "../../api/PaymentAPI";
import { CURRENCY_CODE, formatNpr } from "../../utils/currency.js";
import "./CartSummary.css";

const CartSummary = () => {
  const { cartItems, customer, setCartItems } = useContext(AppContext);
  const navigate = useNavigate();
  const [paymentMethod, setPaymentMethod] = useState("cash");
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [checkoutOrder, setCheckoutOrder] = useState(null);
  const [stripeMinimumCents, setStripeMinimumCents] = useState(null);
  const [stripeOptionsError, setStripeOptionsError] = useState("");
  const taxRate = 2;
  const subtotalCents = cartItems.reduce(
    (total, item) =>
      total + Math.round(Number(item.price) * 100) * item.quantity,
    0,
  );
  const taxCents = Math.round((subtotalCents * taxRate) / 100);
  const grandTotalCents = subtotalCents + taxCents;
  const formatPrice = (cents) => formatNpr(cents / 100);
  const stripeShortfall = stripeMinimumCents === null
    ? 0 : Math.max(0, stripeMinimumCents - grandTotalCents);
  const stripeBlocked = paymentMethod === "stripe"
    && (stripeMinimumCents === null || stripeShortfall > 0);

  useEffect(() => {
    let ignore = false;
    PaymentAPI.stripeOptions()
      .then(({ data }) => {
        const cents = Math.round(Number(data.minimumTotal) * 100);
        if (data.currency !== CURRENCY_CODE || !Number.isSafeInteger(cents) || cents <= 0) {
          throw new Error("Invalid Stripe requirements");
        }
        if (!ignore) setStripeMinimumCents(cents);
      })
      .catch(() => {
        if (!ignore) setStripeOptionsError("Cannot check Stripe's minimum right now. Reload the page or choose cash.");
      });
    return () => { ignore = true; };
  }, []);

  const placeOrder = async () => {
    if (isSubmitting) return;
    setError("");

    if (stripeBlocked) {
      setError(stripeOptionsError || (stripeMinimumCents === null
        ? "Checking Stripe checkout requirements. Please wait."
        : `Stripe requires at least ${formatPrice(stripeMinimumCents)}. Add more items or choose cash.`));
      return;
    }

    if (!customer.name.trim() || !customer.phone.trim()) {
      setError("Enter the customer name and phone number.");
      return;
    }

    if (cartItems.length === 0) {
      setError("Add at least one item to the cart.");
      return;
    }

    setIsSubmitting(true);

    try {
      const orderRequest = {
        customerName: customer.name.trim(),
        phoneNumber: customer.phone.trim(),
        items: cartItems.map((item) => ({
          itemId: item.itemId,
          quantity: item.quantity,
        })),
      };

      if (paymentMethod === "cash") {
        const orderResponse = await PaymentAPI.createCashOrder(orderRequest);
        setCartItems([]);
        const resultParams = new URLSearchParams({
          orderId: orderResponse.data.orderId,
          status: orderResponse.data.paymentStatus,
          method: orderResponse.data.paymentMethod,
          message: "Cash payment recorded.",
        });
        navigate(`/payment-result?${resultParams.toString()}`);
        return;
      }

      const requestKey = JSON.stringify(orderRequest);
      let orderId = checkoutOrder?.requestKey === requestKey ? checkoutOrder.orderId : null;
      if (!orderId) {
        const orderResponse = await PaymentAPI.createOrder(orderRequest);
        orderId = orderResponse.data.orderId;
        setCheckoutOrder({ requestKey, orderId });
      }

      if (paymentMethod === "stripe") {
        const paymentResponse = await PaymentAPI.initiateStripe(orderId);
        window.location.assign(paymentResponse.data.paymentUrl);
        return;
      }

      if (paymentMethod === "khalti") {
        const paymentResponse =
          await PaymentAPI.initiateKhalti(orderId);
        window.location.assign(paymentResponse.data.paymentUrl);
        return;
      }
    } catch (submitError) {
      const status = submitError.response?.status;
      const fallbackMessage = status === 401
        ? "Your login is missing or expired. Please sign in again before checking out."
        : status === 403
          ? "The backend denied this checkout request (HTTP 403). Please check your account permissions and backend error logs."
        : submitError.code === "ECONNABORTED" || submitError.code === "ETIMEDOUT"
          ? "The checkout request timed out. Please check that the backend is running, then try again."
          : !submitError.response
            ? "Cannot reach the billing backend at http://localhost:8080. Start Spring Boot and check the connection."
            : `Unable to start checkout (HTTP ${status}). Please check the backend error and try again.`;
      setError(
        submitError.response?.data?.detail ||
          submitError.response?.data?.message ||
          fallbackMessage,
      );
    } finally {
      // Also reset after a successful redirect request, so returning to checkout
      // or a browser-blocked navigation cannot leave the button disabled.
      setIsSubmitting(false);
    }
  };

  return (
    <section className="cart-summary">
      <h2>Order summary</h2>
      <div className="cart-summary-row">
        <span>Subtotal</span>
        <span>{formatPrice(subtotalCents)}</span>
      </div>
      <div className="cart-summary-row">
        <span>Tax ({taxRate}%)</span>
        <span>{formatPrice(taxCents)}</span>
      </div>
      <div className="cart-summary-row cart-summary-total">
        <span>Grand total</span>
        <span>{formatPrice(grandTotalCents)}</span>
      </div>
      <div className="cart-payment-methods" aria-label="Payment method">
        <button
          className={`cart-payment-button ${
            paymentMethod === "cash" ? "selected" : ""
          }`}
          type="button"
          aria-pressed={paymentMethod === "cash"}
          onClick={() => setPaymentMethod("cash")}
        >
          Cash
        </button>
        <button
          className="cart-payment-button"
          type="button"
          aria-label="Pay via eSewa (currently unavailable)"
          onClick={() =>
            toast.info(
              "eSewa sandbox checkout is temporarily unavailable due to a CAPTCHA issue. Please use Stripe or cash.",
            )
          }
        >
          Pay via eSewa
        </button>
        <button
          className="cart-payment-button"
          type="button"
          aria-label="Pay via Khalti (currently unavailable)"
          onClick={() => toast.info("Khalti is currently unavailable. Please use Stripe or cash.")}
        >
          Pay via Khalti
        </button>
        <button
          className={`cart-payment-button ${paymentMethod === "stripe" ? "selected" : ""}`}
          type="button"
          aria-pressed={paymentMethod === "stripe"}
          onClick={() => setPaymentMethod("stripe")}
        >
          Pay via Stripe
        </button>
      </div>
      {paymentMethod === "stripe" && (
        <p id="stripe-checkout-requirements" role="status">
          {stripeOptionsError || (stripeMinimumCents === null
            ? "Checking Stripe checkout requirements..."
            : stripeShortfall > 0
              ? `Stripe requires at least ${formatPrice(stripeMinimumCents)}. Add ${formatPrice(stripeShortfall)} more, or choose cash for this order.`
              : `Stripe checkout minimum: ${formatPrice(stripeMinimumCents)}. Your order meets this minimum.`)}
        </p>
      )}
      <button
        className="cart-place-order-button"
        type="button"
        disabled={cartItems.length === 0 || isSubmitting || stripeBlocked}
        aria-busy={isSubmitting}
        aria-describedby={cartItems.length === 0 ? "checkout-empty-cart"
          : paymentMethod === "stripe" ? "stripe-checkout-requirements" : undefined}
        onClick={placeOrder}
      >
        {isSubmitting
          ? paymentMethod === "cash"
            ? "Recording cash payment..."
            : `Starting ${paymentMethod === "stripe" ? "Stripe" : "Khalti"} payment...`
          : stripeBlocked
            ? stripeOptionsError ? "Stripe unavailable"
              : stripeMinimumCents === null ? "Checking Stripe..."
                : `Add ${formatPrice(stripeShortfall)} more for Stripe`
          : paymentMethod === "cash"
            ? "Confirm cash & place order"
            : `Continue to ${paymentMethod === "stripe" ? "Stripe" : "Khalti"}`}
      </button>
      {cartItems.length === 0 && (
        <p id="checkout-empty-cart" role="status">
          Add at least one item to enable checkout.
        </p>
      )}
      {error && (
        <p className="cart-summary-error" role="alert">
          {error}
        </p>
      )}
    </section>
  );
};

export default CartSummary;
