import { useContext, useState } from 'react'
import './Login.css'
import { toast } from 'react-toastify'
import { AuthAPI } from '../../api/AuthAPI'
import { useNavigate } from 'react-router-dom'
import { AppContext } from '../../Context/AppContext'

export const Login = () => {
  const navigate = useNavigate();
  const {setAuthData} = useContext(AppContext);

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading,setLoading] = useState(false)

  const handleSubmit = async (event) => {
    event.preventDefault()
    setLoading(true);
    try {
      const response = await AuthAPI.login({email,password})
      if(response.status === 200 ){
        toast.success("Login Successful");
        setLoading(false);
        localStorage.setItem("token",response.data.token);
        localStorage.setItem("role", response.data.role);
        setAuthData(response.data.token,response.data.role);
        navigate("/dashboard");
      }
    } catch (error) {
      console.error(error)
      toast.error("Invalid Email/Password")
    }finally{
      setLoading(false)
    }
  }

  return (
    <>
        <div className="bg-light d-flex align-items-center justify-content-center 100vh login-background">
          <div className="card shadow-lg w-100" style={{maxWidth:'480'}}>
            <div className="card-body">
              <div className="text-center">
                <h1 className="card-title">Sign In</h1>
                <p className="card-text text-muted">
                  Sign In to Continue
                </p>
              </div>
              <div className="mt-4">
                <form onSubmit={handleSubmit}>
                  <div className="mb-4">
                    <label htmlFor="email" className="form-label text-muted">
                      Email address
                    </label>
                    <input type="text" name="email" id="email" placeholder='yourname@example.com' className='form-control' value={email} onChange={(event) => setEmail(event.target.value)} />
                  </div>
                  <div className="mb-4">
                    <label htmlFor="password" className="form-label text-muted">
                      Password
                    </label>
                    <input type="password" name="password" id="password" placeholder='**********' className='form-control' value={password} onChange={(event) => setPassword(event.target.value)} />
                  </div>
                  <div className="d-grid">
                    <button type='submit' className="btn btn-dark btn-lg" disabled={loading}>
                      {loading ? 'Signing in...' : 'Sign in'}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        </div>
    </>
  )
}

