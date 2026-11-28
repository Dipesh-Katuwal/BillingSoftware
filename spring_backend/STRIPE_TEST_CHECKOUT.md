# Stripe Checkout (test mode)

The existing cash, eSewa CAPTCHA-unavailable toast, and Khalti checkout remain available. Stripe uses hosted Checkout; React never receives a secret key or card details. A missing Khalti key does not affect Stripe.

## Configuration

Use Java 17 or a supported newer JDK, the existing MySQL database, Node/npm, and the Stripe CLI. Set these environment variables in the backend process environment through your terminal/IDE's private environment settings:

| Variable | Value |
| --- | --- |
| `STRIPE_TEST_SECRET_KEY` | Your Stripe test/sandbox secret key beginning `sk_test_`, from the same account/sandbox as the CLI. Live keys are rejected. |
| `STRIPE_WEBHOOK_SECRET` | The `whsec_` signing secret printed by the running `stripe listen` command below. Use the CLI listener's secret locally, not a Dashboard endpoint's secret. |
| `FRONTEND_URL` | Optional; defaults to `http://localhost:5173`. This is the trusted base for both result redirects. |
| `STRIPE_MINIMUM_TOTAL_NPR` | Optional; defaults to `100.00`. The app's minimum cart total for starting a new Stripe payment. Keep this high enough to meet your account's provider minimum. |
| `KHALTI_TEST_SECRET_KEY` | Optional, only needed for Khalti. Missing keys produce an unavailable error when Khalti is used. |
| `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` | Existing app requirements for its S3 configuration; unrelated to Stripe. |

Keep credentials out of source control, frontend/Vite environment variables, logs, screenshots, and chat. No Stripe publishable key is needed for a hosted Checkout redirect. Both Stripe secrets can be absent at startup; attempting checkout gives a useful unavailable error. MySQL connection settings remain in `application.properties`.

Start the listener first and keep it running:

```powershell
stripe login
stripe listen --latest --events checkout.session.completed,checkout.session.async_payment_succeeded,checkout.session.async_payment_failed,checkout.session.expired --forward-to http://localhost:8080/payments/stripe/webhook
```

Set `STRIPE_WEBHOOK_SECRET` privately from the listener output, then launch the backend in a terminal/IDE that inherits the environment:

```powershell
# From spring_backend
.\mvnw.cmd spring-boot:run
```

In another terminal:

```powershell
cd ..\react_frontend
npm install
npm run dev
```

The SDK is pinned to `stripe-java:34.2.0`, whose API version is `2026-11-18.endive`. `--latest` requests the latest event format rather than an older account default. The SDK's safe webhook deserializer accepts compatible API versions and rejects incompatible ones. If a later Stripe API release changes the format, align the SDK and the account/webhook destination version. A Dashboard test webhook destination should use the SDK-compatible API version and the four events above; its own signing secret replaces the CLI secret when testing that destination.

Hibernate's existing `ddl-auto=update` adds a nullable unique `stripe_session_id` column. For an existing MySQL database, run `src/main/resources/db/manual/expand-payment-methods.sql` once in MySQL Workbench against `billing_software`. Hibernate does not reliably expand an existing native enum: older databases can still allow only `CASH/ESEWA`, making Khalti and Stripe updates fail. The script adds both allowed values and preserves existing order data; it is safe to re-run. Fresh schemas already include all four payment methods.

## Local test flow

1. Sign in to the app at `http://localhost:5173`, add catalog items, enter the customer details, select **Pay via Stripe**, and continue. The backend creates the order using catalog prices and tax, then creates a Checkout Session using only that stored total.
2. Complete hosted Checkout with Stripe's test card `4242 4242 4242 4242`, any future expiry, and any valid CVC. Use only Stripe test card data. The order stays in NPR. Choose an amount that meets your account's Stripe minimum charge requirement; an unsupported account or amount produces an error and never triggers a currency conversion in this application.

   For the configured US test account, Stripe requires a total equivalent to at least USD 0.50 even when presentment is NPR. A diagnostic with an existing unpaid NPR 23.46 order was rejected as approximately USD 0.16. The app therefore requires at least NPR 100 by default for a new Stripe checkout. React fetches this policy from the authenticated `/payments/stripe/options` endpoint, displays exactly how much more to add, and blocks an ineligible submission before creating an order. The backend independently validates the stored total before contacting Stripe. Cash and Khalti remain selectable for small orders (subject to Khalti's own minimum). No surcharge or currency change is applied. The provider's exact threshold depends on its conversion rate; adjust `STRIPE_MINIMUM_TOTAL_NPR` if necessary. With the current API, the provider error can be nested under `allowed_payment_method_types`; the backend recognizes that as a minimum-amount error before checking activation errors.
3. Watch the listener for a successful callback. On returning, the result page automatically calls the authenticated Stripe status endpoint. It shows `PAID` only after the backend validates a paid, completed Session against the stored session ID, both order references, currency, amount, mode, and test-mode flag. The success URL itself contains no payment verdict.
4. Test cancellation by using Checkout's back/cancel link. Both redirect paths lead to status verification. An open session stays `PENDING`; clicking cancel does not prove provider-side cancellation. Use **Check payment status** to refresh, or resume the original Checkout URL.
5. Test expiry using an actual session ID from the initiation response or Stripe Dashboard:

   ```powershell
   stripe checkout sessions expire cs_test_REPLACE_WITH_ACTUAL_SESSION_ID
   ```

   A verified expiry webhook or authenticated refresh changes the order to `FAILED`. Create a new order after a finished attempt; this implementation deliberately does not replace a terminal Stripe session on the same order.
6. Test a decline with Stripe's `4000 0000 0000 0002` test card. A declined card can be retried within hosted Checkout; it does not by itself make an open Session terminal. The order remains pending until a verified final outcome.

7. After confirmation, the result page verifies the stored paid order, clears the completed cart, and returns to `/explore` after 2.5 seconds with the order's receipt open. Download the standalone HTML receipt or choose **Print / Save PDF** to use the browser's print dialog. The receipt contains the saved customer, order date, item snapshots, quantities, unit/line prices, subtotal, tax, NPR grand total, payment method/status, and provider reference. It does not recalculate historical prices from today's catalog.

Orders are inserted before Checkout starts; confirmation updates the same row to `PAID` rather than inserting another row. In MySQL Workbench, refresh the result grid (and commit/rollback any old read transaction) and query the configured `billing_software` schema by the exact `order_id` shown on the result page. Item rows are in `order_item_entity`, whose `order_id` foreign key is the numeric `tbl_orders.id`, not the public `ORD...` identifier. The authenticated receipt endpoint returns 404 if the order is missing and 409 if its saved status is not `PAID`; no receipt or redirect can create a payment verdict.

The **Order History** menu opens `/order-history`. It reads all persisted `PAID` orders (including cash and previously completed online payments), newest order date first, with the numeric row ID breaking date ties. Search by order ID, customer name, or phone; refresh to fetch newly paid orders; use **View receipt** to open, download, or print an order's saved receipt. Pending, unpaid, and failed orders are excluded by the backend query.

`stripe trigger checkout.session.completed` creates an unrelated fixture Session. It cannot pay a BillingSoftware order because it lacks the matching stored session and order references. Use the app-created Session for an actual end-to-end test. Duplicate delivery is safe: the first confirmed transition is saved once, and later expiry/failure callbacks never downgrade `PAID`.

## Backend endpoints

| Method/path | Authentication | Behavior |
| --- | --- | --- |
| `GET /payments/stripe/options` | Existing JWT | Returns only `{currency, minimumTotal}` for checkout eligibility. No secrets or provider call. |
| `GET /orders/{orderId}/receipt` | Existing JWT | Returns the persisted paid order and item snapshots for viewing, download, and printing; never creates another order. |
| `GET /orders/paid` | Existing JWT | Returns paid order summaries newest first, using saved customer/payment details and NPR totals. No provider request or payment update. |
| `POST /payments/stripe/initiate/{orderId}` | Existing JWT | Existing unpaid order only; returns `{sessionId, paymentUrl}` and stores `STRIPE/PENDING`. Reuses an open pending session. |
| `GET /payments/stripe/status/{orderId}` | Existing JWT | Retrieves the stored Session directly from Stripe and validates it before updating/returning `{orderId, paymentStatus, message}`. Ignores browser verdict parameters. |
| `POST /payments/stripe/webhook` | Stripe signature | Verifies `Stripe-Signature` with the untouched request body and signing secret. Invalid signatures return 400. |

Orders follow the existing authenticated staff/POS access model; the project currently has no per-customer order ownership model. Shared pessimistic order locking serializes Stripe, Khalti, and eSewa initiation, and Stripe sends a stable per-order idempotency key for retry recovery. Stripe creation/status requests have a longer frontend timeout than the app's default three seconds.

The shared repository uses an explicit `SELECT ... FOR UPDATE` query compatible with both MySQL and MariaDB. This avoids `FOR UPDATE OF <alias>` generated when the MySQL JDBC driver causes Hibernate to misidentify a MariaDB server. Payment services keep this query inside their existing transactions, so concurrent initiation remains serialized.

NPR is a supported two-decimal presentment currency. Conversion is exact `BigDecimal.movePointRight(2).longValueExact()` (NPR 372.30 becomes 37230 paisa). Fractional paisa, overflow, and nonpositive totals are rejected. No browser-supplied amount, exchange rate, automatic tax, discount, or alternate currency is used.

## Automated verification

```powershell
# All backend tests, including focused payment tests and Spring context/HTTP tests
.\mvnw.cmd clean test

cd ..\react_frontend
node --test src/components/OrderReceipt/receiptDocument.test.js
npm run build
npm run lint
```

Tests use H2 and fixture AWS settings under the `test` profile; no local MySQL, Stripe key, live charge, or provider network access is needed. Service tests exercise real Stripe HMAC verification with signed fixtures. SDK adapter tests mock Stripe's HTTP client. HTTP integration tests check public callback access, protected initiation/status routes, and ignored browser verdicts.

## Official references checked

- [Stripe Java SDK requirements and current release](https://github.com/stripe/stripe-java/tree/v34.2.0): Java 17 is explicitly supported.
- [Supported currencies and minor units](https://docs.stripe.com/currencies): NPR is listed; currencies have two decimals unless specified otherwise.
- [Create a Checkout Session](https://docs.stripe.com/api/checkout/sessions/create): currency, integer unit amount, references, metadata, hosted redirect URLs, and allowed card payment type.
- [Checkout fulfillment](https://docs.stripe.com/checkout/fulfillment): webhook-based confirmation and checking payment status.
- [Webhook verification](https://docs.stripe.com/webhooks): raw body, endpoint secret, signatures, and retries.
- [Stripe CLI](https://docs.stripe.com/stripe-cli/use-cli): local webhook forwarding.

Automated tests do not establish that a real Stripe sandbox transaction was completed. That requires configured test credentials and the app-created Checkout flow above.
