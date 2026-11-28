import { useEffect, useRef, useState } from "react";
import { PaymentAPI } from "../../api/PaymentAPI";
import { receiptDocument } from "./receiptDocument";
import "./OrderReceipt.css";

const OrderReceipt = ({ orderId, onClose }) => {
  const dialog = useRef(null);
  const preview = useRef(null);
  const [receipt, setReceipt] = useState(null);
  const [error, setError] = useState("");
  const [retry, setRetry] = useState(0);
  const [previewReady, setPreviewReady] = useState(false);

  useEffect(() => {
    const element = dialog.current;
    element.showModal();
    return () => element.close();
  }, []);

  useEffect(() => {
    let ignore = false;
    PaymentAPI.getReceipt(orderId)
      .then(({ data }) => {
        if (ignore) return;
        if (data.orderId !== orderId || data.paymentStatus !== "PAID") {
          throw new Error("Receipt is not a matching paid order");
        }
        setReceipt(data);
        setError("");
      })
      .catch((receiptError) => {
        if (!ignore) setError(receiptError.response?.data?.detail || "Could not load the saved receipt. Please retry.");
      });
    return () => { ignore = true; };
  }, [orderId, retry]);

  const download = () => {
    const url = URL.createObjectURL(new Blob([receiptDocument(receipt)], {
      type: "text/html;charset=utf-8",
    }));
    const link = document.createElement("a");
    link.href = url;
    link.download = `receipt-${receipt.orderId.replace(/[^a-zA-Z0-9_-]/g, "_")}.html`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
  };

  const print = () => {
    try {
      preview.current.contentWindow.focus();
      preview.current.contentWindow.print();
    } catch {
      setError("Printing is unavailable in this browser. Download the receipt, open it, and print or save it as PDF.");
    }
  };

  return (
    <dialog className="order-receipt-dialog" ref={dialog} aria-labelledby="receipt-heading"
      onCancel={(event) => { event.preventDefault(); onClose(); }}>
      <header className="order-receipt-header">
        <h2 id="receipt-heading">Order receipt</h2>
        <button type="button" onClick={onClose} aria-label="Close receipt">Close</button>
      </header>
      {!receipt && !error && <p role="status">Loading your saved receipt...</p>}
      {error && <p role="alert" className="order-receipt-error">{error}</p>}
      {!receipt && error && <button type="button" onClick={() => setRetry((value) => value + 1)}>Retry receipt</button>}
      {receipt && <>
        <iframe ref={preview} title={`Receipt for ${receipt.orderId}`}
          sandbox="allow-same-origin allow-modals" srcDoc={receiptDocument(receipt)}
          onLoad={() => setPreviewReady(true)} />
        <footer className="order-receipt-actions">
          <button type="button" onClick={download}>Download receipt (.html)</button>
          <button type="button" onClick={print} disabled={!previewReady}>Print / Save PDF</button>
        </footer>
      </>}
    </dialog>
  );
};

export default OrderReceipt;
