import { useContext, useEffect, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { PaymentAPI } from "../../api/PaymentAPI";
import { AppContext } from "../../Context/AppContext";
import "./PaymentResult.css";

const PaymentResult = () => {
  const navigate = useNavigate();
  const { setCartItems } = useContext(AppContext);
  const [searchParams] = useSearchParams();
  const orderId = searchParams.get("orderId");
  const paymentMethod = searchParams.get("method");
  const [paymentStatus, setPaymentStatus] = useState(
    paymentMethod === "STRIPE" ? "PENDING" : searchParams.get("status") || "PENDING",
  );
  const [message, setMessage] = useState(
    paymentMethod === "STRIPE"
      ? "We are checking your Stripe payment status."
      : searchParams.get("message") || "We are checking your payment status.",
  );
  const [error, setError] = useState("");
  const [isChecking, setIsChecking] = useState(paymentMethod === "STRIPE");
  const [receiptReady, setReceiptReady] = useState(false);
  const [receiptRetry, setReceiptRetry] = useState(0);

  const refreshStatus = async () => {
    if (!orderId) return;

    setError("");
    setIsChecking(true);
    try {
      const response =
        paymentMethod === "STRIPE"
          ? await PaymentAPI.refreshStripeStatus(orderId)
          : paymentMethod === "KHALTI"
          ? await PaymentAPI.refreshKhaltiStatus(orderId)
          : await PaymentAPI.refreshEsewaStatus(orderId);
      setPaymentStatus(response.data.paymentStatus);
      setMessage(response.data.message);
    } catch (statusError) {
      console.error("Unable to refresh payment status:", statusError);
      setError(statusError.response?.data?.detail || "Could not check the payment status. Please try again.");
    } finally {
      setIsChecking(false);
    }
  };

  useEffect(() => {
    if (paymentMethod !== "STRIPE" || !orderId) return;
    let ignore = false;
    PaymentAPI.refreshStripeStatus(orderId)
      .then(({ data }) => {
        if (!ignore) {
          setPaymentStatus(data.paymentStatus);
          setMessage(data.message);
        }
      })
      .catch((statusError) => {
        if (!ignore) setError(statusError.response?.data?.detail || "Could not check the payment status. Please try again.");
      })
      .finally(() => {
        if (!ignore) setIsChecking(false);
      });
    return () => { ignore = true; };
  }, [orderId, paymentMethod]);

  useEffect(() => {
    if (paymentStatus !== "PAID" || !orderId) return;
    let ignore = false;
    let timer;
    // Verify the persisted paid order before redirecting, including cash/Khalti
    // result URLs whose query strings are not proof of payment.
    PaymentAPI.getReceipt(orderId)
      .then(({ data }) => {
        if (ignore) return;
        if (data.orderId !== orderId || data.paymentStatus !== "PAID") {
          throw new Error("Receipt does not match a paid order");
        }
        setError("");
        setReceiptReady(true);
        setCartItems([]);
        timer = window.setTimeout(() => {
          navigate("/explore", { replace: true, state: { receiptOrderId: orderId } });
        }, 2500);
      })
      .catch((receiptError) => {
        if (!ignore) setError(receiptError.response?.data?.detail || "Could not load the saved receipt. Please retry.");
      });
    return () => {
      ignore = true;
      window.clearTimeout(timer);
    };
  }, [paymentStatus, orderId, navigate, setCartItems, receiptRetry]);

  const title =
    paymentStatus === "PAID"
      ? "Payment confirmed"
      : paymentStatus === "FAILED"
        ? "Payment not completed"
        : "Payment status pending";

  return (
    <main className="payment-result">
      <section className="payment-result-card" aria-live="polite">
        <h1>{title}</h1>
        <p>{message}</p>
        {orderId && <p>Order: {orderId}</p>}
        {paymentMethod && <p>Payment method: {paymentMethod}</p>}
        {receiptReady && <p>Returning to items with your receipt in 2–3 seconds...</p>}
        {error && (
          <p className="payment-result-error" role="alert">
            {error}
          </p>
        )}
        {paymentStatus === "PENDING" && orderId && (
          <button
            className="payment-result-check-button"
            type="button"
            disabled={isChecking}
            onClick={refreshStatus}
          >
            {isChecking ? "Checking..." : "Check payment status"}
          </button>
        )}
        {paymentStatus === "PAID" && error && !receiptReady && (
          <button type="button" onClick={() => setReceiptRetry((value) => value + 1)}>Retry receipt</button>
        )}
        <Link to="/explore" state={receiptReady ? { receiptOrderId: orderId } : null}>Return to items</Link>
      </section>
    </main>
  );
};

export default PaymentResult;
