import './ManageItems.css'
import ItemForm from "../../components/ManageItems/ItemForm.jsx";
import ItemsList from "../../components/ManageItems/ItemsList.jsx";

export const ManageItems = () => {
  return (
    <div className="items-container text-light">
      <div className="item-form-column">
        <ItemForm/>
      </div>
      <div className="item-list-column">
        <ItemsList/>
      </div>
    </div>
  )
}
