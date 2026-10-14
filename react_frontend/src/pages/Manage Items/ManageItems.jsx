import './ManageItems.css'
import ItemForm from "../../components/ManageItems/ItemForm.jsx";

export const ManageItems = () => {
  return (
    <div className="items-container text-light">
      <div className="left-column">
        <ItemForm/>
      </div>
      <div className="right-column">
        list of items
      </div>
    </div>
  )
}
