import './Login.css'

export const Login = () => {
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
                <form action="">
                  <div className="mb-4">
                    <label htmlFor="email" className="form-label text-muted">
                      Email address
                    </label>
                    
                  </div>
                </form>
              </div>
            </div>
          </div>
        </div>
    </>
  )
}

