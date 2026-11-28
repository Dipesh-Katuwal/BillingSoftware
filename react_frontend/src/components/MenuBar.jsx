import styles from "./MenuBar.module.css";
import { assets } from "../assets/assets.js";
import { Link, useNavigate } from "react-router-dom";
import { IoLogOut } from "react-icons/io5";
import { useContext } from "react";
import { AppContext } from "../Context/AppContext.jsx";
import { toast } from "react-toastify";

const MenuBar = () => {
  const navigate = useNavigate();
  const {auth,setAuthData} = useContext(AppContext);
  const logout = () =>{
    localStorage.removeItem("token");
    localStorage.removeItem("role");
    setAuthData(null,null);
    toast.success("Logged out successfully !!!");
    navigate("/login");
  }

  return (
    <nav className={`navbar navbar-expand-xl navbar-dark ${styles.navbar}`}>
      <Link className={`navbar-brand ${styles.brand}`} to="/dashboard">
        <img
          src={assets.logo}
          alt="Billing software"
          className={styles.logoImage}
        />
      </Link>
      <button
        className="navbar-toggler"
        type="button"
        data-bs-toggle="collapse"
        data-bs-target="#navbarSupportedContent"
        aria-controls="navbarSupportedContent"
        aria-expanded="false"
      >
        <span className="navbar-toggler-icon"></span>
      </button>
      <div className="collapse navbar-collapse" id="navbarSupportedContent">
        <ul className={`navbar-nav ${styles.navList}`}>
          <li className="nav-item">
            <Link className={`nav-link ${styles.link}`} to="/dashboard">
              Dashboard
            </Link>
          </li>
          <li className="nav-item">
            <Link className={`nav-link ${styles.link}`} to="/explore">
              Explore
            </Link>
          </li>
          <li className="nav-item">
            <Link className={`nav-link ${styles.link}`} to="/items">
              Manage Items
            </Link>
          </li>
          <li className="nav-item">
            <Link className={`nav-link ${styles.link}`} to="/categories">
              Manage Categories
            </Link>
          </li>
          <li className="nav-item">
            <Link className={`nav-link ${styles.link}`} to="/users">
              Manage Users
            </Link>
          </li>
          <li className="nav-item">
            <Link className={`nav-link ${styles.link}`} to="/order-history">
              Order History
            </Link>
          </li>
        </ul>
        <ul className="navbar-nav ms-auto me-3 me-lg-4 align-items-xl-center">
          {auth.email && (
            <li className="nav-item">
              <span className={styles.accountEmail} title={auth.email}>{auth.email}</span>
            </li>
          )}
          <li className="nav-item dropdown">
            <a
              href="#"
              className="nav-link dropdown-toggle"
              id="navbarDropdown"
              role="button"
              data-bs-toggle="dropdown"
              aria-expanded="false"
            >
              <img
                src={assets.profile}
                alt=""
                height={32}
                width={32}
                className={styles.profileImage}
              />
            </a>
            <ul
              className="dropdown-menu dropdown-menu-end"
              aria-labelledby="navbarDropdown"
            >
              <li>
                <a href="#!" className="dropdown-item">
                  settings
                </a>
                <a href="#!" className="dropdown-item">
                  Activity log
                </a>
                <li>
                  <hr className="dropdown-divider" />
                </li>
                <a href="#!" className="dropdown-item" onClick={logout}>
                  log out <span>{<IoLogOut />}</span>
                </a>
              </li>
            </ul>
          </li>
        </ul>
      </div>
    </nav>
  );
};

export default MenuBar;
