import {useState} from "react";

const UserForm = () => {
    const [name, setName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [isSaving, setIsSaving] = useState(false);

    const handleSubmit = async (event) => {
        event.preventDefault();

    }
    return (
        <form className="card col-8 h-100 align-self-start text-dark" onSubmit={handleSubmit}>
            <div className="card-body p-4">
                <div className="mb-3">
                    <label htmlFor="user-name" className="form-label fw-semibold">
                        Name
                    </label>
                    <input
                        id="user-name"
                        type="text"
                        className="form-control"
                        placeholder="user Name"
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
                        type="text"
                        className="form-control"
                        placeholder="user Email"
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
                        placeholder="***********"
                        value={password}
                        onChange={(event) => setPassword(event.target.value)}
                        required
                    />
                </div>
                <button type="submit" className="btn btn-warning w-100">
                    {isSaving ? 'Saving...' : 'Save'}
                </button>
            </div>
        </form>
    )
}
export default UserForm;