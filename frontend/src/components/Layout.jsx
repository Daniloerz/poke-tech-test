import { Link, NavLink, Outlet } from 'react-router';
import { useAuth } from '../auth/AuthContext.jsx';
import styles from './Layout.module.css';

export default function Layout() {
  const { isAuthenticated, username, logout } = useAuth();

  return (
    <>
      <header className={styles.header}>
        <div className={`container ${styles.bar}`}>
          <Link to="/pokemon" className={styles.brand}>
            Poke Tech Test
          </Link>
          <nav className={styles.nav} aria-label="Main">
            <NavLink to="/pokemon" end className={navLinkClass}>
              Catalog
            </NavLink>
            <NavLink to="/my-pokemon" className={navLinkClass}>
              My Pokemon
            </NavLink>
          </nav>
          <div className={styles.session}>
            {isAuthenticated ? (
              <>
                <span className={styles.username}>{username}</span>
                <button type="button" className="button secondary" onClick={logout}>
                  Log out
                </button>
              </>
            ) : (
              <Link to="/login" className="button">
                Log in
              </Link>
            )}
          </div>
        </div>
      </header>
      <main className="container">
        <Outlet />
      </main>
    </>
  );
}

function navLinkClass({ isActive }) {
  return isActive ? `${styles.link} ${styles.active}` : styles.link;
}
