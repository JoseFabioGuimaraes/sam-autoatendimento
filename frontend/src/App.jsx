import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import { ToastProvider } from './contexts/ToastContext';
import Layout from './components/Layout';
import LoginPage from './pages/auth/LoginPage';
import RegisterPage from './pages/auth/RegisterPage';
import PacienteDashboard from './pages/paciente/PacienteDashboard';
import MedicoDashboard from './pages/medico/MedicoDashboard';
import DisponibilidadePage from './pages/medico/DisponibilidadePage';

function PrivateRoute({ children, allowedPerfil }) {
  const { isAuthenticated, perfil } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (allowedPerfil && perfil !== allowedPerfil) {
    return <Navigate to={perfil === 'MEDICO' ? '/medico' : '/paciente'} replace />;
  }
  return children;
}

function AppRoutes() {
  const { isAuthenticated, perfil } = useAuth();
  const home = perfil === 'MEDICO' ? '/medico' : '/paciente';

  return (
    <Routes>
      <Route path="/login" element={isAuthenticated ? <Navigate to={home} /> : <LoginPage />} />
      <Route path="/register" element={isAuthenticated ? <Navigate to="/paciente" /> : <RegisterPage />} />

      <Route element={<PrivateRoute><Layout /></PrivateRoute>}>
        <Route path="/paciente" element={<PrivateRoute allowedPerfil="PACIENTE"><PacienteDashboard /></PrivateRoute>} />
        <Route path="/medico" element={<PrivateRoute allowedPerfil="MEDICO"><MedicoDashboard /></PrivateRoute>} />
        <Route path="/medico/disponibilidade" element={<PrivateRoute allowedPerfil="MEDICO"><DisponibilidadePage /></PrivateRoute>} />
      </Route>

      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <ToastProvider>
        <AuthProvider>
          <AppRoutes />
        </AuthProvider>
      </ToastProvider>
    </BrowserRouter>
  );
}
