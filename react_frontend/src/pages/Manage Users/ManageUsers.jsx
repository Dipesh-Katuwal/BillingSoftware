import './ManageUsers.css'
import UserForm from "../../components/ManageUsers/UserForm.jsx";
import UsersList from "../../components/ManageUsers/UsersList.jsx";

export const ManageUsers = () => {
  return (
    <div className="users-container text-light">
      <div className="user-form-column">
        <UserForm/>
      </div>
      <div className="user-list-column">
        <UsersList/>
      </div>
    </div>
  )
}
