import './ManageUsers.css'
import UserForm from "../../components/ManageUsers/UserForm.jsx";

export const ManageUsers = () => {
  return (
    <div className="users-container text-light">
      <div className="left-column">
        <UserForm/>
      </div>
      <div className="right-column">
        list of users
      </div>
    </div>
  )
}
