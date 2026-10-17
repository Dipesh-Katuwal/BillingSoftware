import styles from './MenuBar.module.css'
import {assets} from '../assets/assets.js'
import { Link } from 'react-router-dom'
const MenuBar = () => {
  return (
      <nav className={`navbar navbar-expand-lg navbar-dark ${styles.navbar}`}>
        <Link
          className={`navbar-brand ${styles.brand}`}
          to="/dashboard"
        >
          <img src={assets.logo} alt="Billing software" className={styles.logoImage} />
        </Link>
        <div className="collapse navbar-collapse" id="navbarSupportedContent">
          <ul className={`navbar-nav ${styles.navList}`}>
            <li className="nav-item">
              <Link
                className={`nav-link ${styles.link}`}
                to="/dashboard"
              >
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
          </ul>
        </div>
      </nav>
  )
}

export default MenuBar