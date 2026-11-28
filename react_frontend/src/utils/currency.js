export const CURRENCY_CODE = "NPR";
export const CURRENCY_SYMBOL = "Rs.";

export const formatNpr = (amount) =>
  `${CURRENCY_SYMBOL} ${Number(amount).toFixed(2)}`;
