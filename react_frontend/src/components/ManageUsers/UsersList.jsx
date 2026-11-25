import { useContext, useEffect, useState } from "react";
import { MdDelete } from "react-icons/md";
import { toast } from "react-toastify";
import { AppContext } from "../../Context/AppContext";
import { UsersAPI } from "../../api/UsersAPI";

const UsersList = () => {
  const { users, setUsers } = useContext(AppContext);
  const [searchTerm, setSearchTerm] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  const visibleUsers = users.filter((user) =>
    `${user.name} ${user.email}`
      .toLowerCase()
      .includes(searchTerm.trim().toLowerCase()),
  );

  useEffect(() => {
    const loadUsers = async () => {
      try {
        const allUsers = await UsersAPI.getUsers();
        setUsers(allUsers);
      } catch (error) {
        console.error("Unable to load users:", error);
        toast.error("Unable to load users.");
      } finally {
        setIsLoading(false);
      }
    };

    loadUsers();
  }, [setUsers]);

  const deleteUser = async (user) => {
    try {
      await UsersAPI.deleteUser(user.userId);
      setUsers((currentUsers) =>
        currentUsers.filter((currentUser) => currentUser.userId !== user.userId),
      );
      toast.success(`"${user.name}" deleted successfully.`);
    } catch (error) {
      console.error("Unable to delete user:", error);
      toast.error(`Could not delete "${user.name}". Please try again.`);
    }
  };

  return (
    <div className="users-list-container">
      <h2 className="users-list-title">Users</h2>
      <div className="users-search">
        <input
          className="form-control"
          type="search"
          placeholder="Search users"
          value={searchTerm}
          onChange={(event) => setSearchTerm(event.target.value)}
        />
      </div>
      <div className="user-cards">
        {isLoading ? (
          <p className="users-empty-state">Loading users...</p>
        ) : (
          <>
            {visibleUsers.map((user) => (
              <div className="user-card" key={user.userId}>
                <div className="user-card-content">
                  <div className="user-avatar" aria-hidden="true">
                    {user.name.charAt(0).toUpperCase()}
                  </div>
                  <div className="user-card-details">
                    <h2>{user.name}</h2>
                    <p>{user.email}</p>
                    <span className="user-role">{user.role}</span>
                  </div>
                  <button
                    className="btn btn-danger user-delete-button"
                    type="button"
                    aria-label={`Delete ${user.name}`}
                    onClick={() => deleteUser(user)}
                  >
                    <MdDelete />
                  </button>
                </div>
              </div>
            ))}
            {visibleUsers.length === 0 && (
              <p className="users-empty-state">
                {users.length === 0
                  ? "No users yet."
                  : "No matching users."}
              </p>
            )}
          </>
        )}
      </div>
    </div>
  );
};

export default UsersList;