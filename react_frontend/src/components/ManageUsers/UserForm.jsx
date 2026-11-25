import { useContext, useState } from "react";
import { toast } from "react-toastify";
import { AppContext } from "../../Context/AppContext";
import { UsersAPI } from "../../api/UsersAPI";

const UserForm = () => {
  const { setUsers } = useContext(AppContext);
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState("USER");
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setIsSaving(true);

    try {
      const newUser = await UsersAPI.addUser({ name, email, password, role });
      setUsers((currentUsers) => [newUser, ...currentUsers]);
      setName("");
      setEmail("");
      setPassword("");
      setRole("USER");
      toast.success(`User ${name} added successfully.`);
    } catch (submitError) {
      setError(
        submitError.response?.data?.message ||
          submitError.message ||
          "Unable to save the user.",
      );
      toast.error("Unable to save the user.");
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <form className="card user-form-card" onSubmit={handleSubmit}>
      <div className="card-body p-4">
        <div className="user-form-heading">
          <h2>Create a user</h2>
          <p>Add a user account and choose its access role.</p>
        </div>

        <div className="mb-3">
          <label htmlFor="user-name" className="form-label fw-semibold">
            Name
          </label>
          <input
            id="user-name"
            type="text"
            className="form-control"
            placeholder="User name"
            value={name}
            onChange={(event) => setName(event.target.value)}
            required
          />
        </div>

        <div className="mb-3">
          <label htmlFor="user-email" className="form-label fw-semibold">
            Email
          </label>
          <input
            id="user-email"
            type="email"
            className="form-control"
            placeholder="name@example.com"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
          />
        </div>

        <div className="mb-3">
          <label htmlFor="user-password" className="form-label fw-semibold">
            Password
          </label>
          <input
            id="user-password"
            type="password"
            className="form-control"
            placeholder="Enter a password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
        </div>

        <div className="mb-3">
          <label htmlFor="user-role" className="form-label fw-semibold">
            Role
          </label>
          <select
            id="user-role"
            className="form-control"
            value={role}
            onChange={(event) => setRole(event.target.value)}
            required
          >
            <option value="USER">User</option>
            <option value="ADMIN">Admin</option>
          </select>
        </div>

        {error && <div className="alert alert-danger py-2">{error}</div>}

        <button
          type="submit"
          className="btn btn-primary w-100 user-save-button"
          disabled={isSaving}
        >
          {isSaving ? "Saving..." : "Save"}
        </button>
      </div>
    </form>
  );
};

export default UserForm;