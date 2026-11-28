import { useContext } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import OrderReceipt from '../../components/OrderReceipt/OrderReceipt'
import './Explore.css'
import { AppContext } from '../../Context/AppContext'
import Categories from '../../components/Categories/Categories';
import Items from '../../components/Items/Items';
import CartItems from '../../components/CartItems/CartItems';
import CartSummary from '../../components/CartSummary/CartSummary';
import CustomerForm from '../../components/CustomerForm/CustomerForm';

export const Explore = () => {
  const {categories} =useContext(AppContext);
  const location = useLocation();
  const navigate = useNavigate();
  const receiptOrderId = location.state?.receiptOrderId;
  console.log(categories);
  return (
    <div className='explore-container text-light'>
      {receiptOrderId && (
        <OrderReceipt
          key={receiptOrderId}
          orderId={receiptOrderId}
          onClose={() => navigate('/explore', { replace: true, state: null })}
        />
      )}
      <div className="left-column">
        <div className="first-row" style={{overflow:"auto"}}>
          <Categories/>
        </div>
        <hr className='horizontal-line'/>
        <div className="second-row">
          <Items/>
        </div>
      </div>
      <div className="right-column d-flex flex-column">
        <div className="customer-form-container" style={{height:'15%'}}>
          <CustomerForm/>
        </div>
        <hr className='my-3 text-light'/>
        <div className="cart-items-container" style={{height:"55%" , overflowY:'auto'}}>
          <CartItems/>
        </div>
        <div className="cart-summary-container" style={{height:"30%"}}>
          <CartSummary/>
        </div>
      </div>
    </div>
  )
}
