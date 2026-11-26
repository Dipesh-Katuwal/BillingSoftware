import { useContext } from "react";
import { AppContext } from "../../Context/AppContext";
import "./CartItems.css";

const CartItems = () => {
  const { cartItems, setCartItems } = useContext(AppContext);
  const itemCount = cartItems.reduce(
    (total, item) => total + item.quantity,
    0,
  );

  const updateQuantity = (itemId, change) => {
    setCartItems((currentCartItems) =>
      currentCartItems.flatMap((item) => {
        if (item.itemId !== itemId) return [item];
        const quantity = item.quantity + change;
        return quantity > 0 ? [{ ...item, quantity }] : [];
      }),
    );
  };

  return (
    <section className="cart-items">
      <header className="cart-items-heading">
        <h2>Cart items</h2>
        <span>
          {itemCount} {itemCount === 1 ? "item" : "items"}
        </span>
      </header>

      {cartItems.length === 0 ? (
        <p className="cart-items-empty">Your cart is empty.</p>
      ) : (
        <div className="cart-items-list">
          {cartItems.map((item) => (
            <article className="cart-item" key={item.itemId}>
              {item.imgUrl ? (
                <img src={item.imgUrl} alt="" className="cart-item-image" />
              ) : (
                <div className="cart-item-image cart-item-image-placeholder">
                  {item.name.charAt(0).toUpperCase()}
                </div>
              )}
              <div className="cart-item-details">
                <h3>{item.name}</h3>
                <span>${Number(item.price).toFixed(2)} each</span>
              </div>
              <div className="cart-item-quantity">
                <button
                  type="button"
                  aria-label={`Remove one ${item.name}`}
                  onClick={() => updateQuantity(item.itemId, -1)}
                >
                  −
                </button>
                <span>{item.quantity}</span>
                <button
                  type="button"
                  aria-label={`Add one ${item.name}`}
                  onClick={() => updateQuantity(item.itemId, 1)}
                >
                  +
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
};

export default CartItems;