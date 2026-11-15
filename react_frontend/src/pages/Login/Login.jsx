import { useState } from 'react'
import './Login.css'

export const Login = () => {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading] = useState(false)

  const handleSubmit = (event) => {
    event.preventDefault()
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

