import { useContext } from "react";
import { AppContext } from "../../Context/AppContext";
import "./CustomerForm.css";

const CustomerForm = () => {
  const { customer, setCustomer } = useContext(AppContext);

  return (
    <section className="customer-form">
      <h2>Customer details</h2>
      <div className="customer-form-fields">
        <label>
          <span>Customer name</span>
          <input
            type="text"
            autoComplete="name"
            placeholder="Enter customer name"
            value={customer.name}
            onChange={(event) =>
              setCustomer((current) => ({ ...current, name: event.target.value }))
            }
          />
        </label>
        <label>
          <span>Phone number</span>
          <input
            type="tel"
            autoComplete="tel"
            inputMode="tel"
            placeholder="Enter phone number"
            value={customer.phone}
            onChange={(event) =>
              setCustomer((current) => ({ ...current, phone: event.target.value }))
            }
          />
        </label>
      </div>
    </section>
  );
};

export default CustomerForm;