import { Navigate, Route, Routes } from 'react-router';
import RequireAuth from './auth/RequireAuth.jsx';
import Layout from './components/Layout.jsx';
import StatusMessage from './components/StatusMessage.jsx';
import CatalogPage from './pages/CatalogPage.jsx';
import LocalPokemonEditPage from './pages/LocalPokemonEditPage.jsx';
import LocalPokemonListPage from './pages/LocalPokemonListPage.jsx';
import LoginPage from './pages/LoginPage.jsx';
import PokemonDetailPage from './pages/PokemonDetailPage.jsx';
import RegisterPage from './pages/RegisterPage.jsx';

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Navigate to="/pokemon" replace />} />
        <Route path="pokemon" element={<CatalogPage />} />
        <Route path="pokemon/:idOrName" element={<PokemonDetailPage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />
        <Route element={<RequireAuth />}>
          <Route path="my-pokemon" element={<LocalPokemonListPage />} />
          <Route path="my-pokemon/:id/edit" element={<LocalPokemonEditPage />} />
        </Route>
        <Route path="*" element={<StatusMessage>Page not found.</StatusMessage>} />
      </Route>
    </Routes>
  );
}
