import { useNavigate, Link, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { LogOut } from 'lucide-react';

export default function Navbar() {
  const { perfil, email, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const handleLogout = () => { logout(); navigate('/login'); };

  const isActive = (path) => location.pathname === path;

  return (
    <nav className="navbar">
      <div style={{ display: 'flex', alignItems: 'center', gap: '2rem' }}>
        <div className="navbar-brand" onClick={() => navigate(perfil === 'MEDICO' ? '/medico' : '/paciente')} style={{ cursor: 'pointer' }}>
          <svg width="32" height="32" viewBox="0 0 32 32" fill="none">
            <circle cx="16" cy="16" r="16" fill="#10b981"/>
            <path d="M10 16 L14 20 L22 12" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" fill="none"/>
          </svg>
          SAM
        </div>
        <span className="navbar-perfil" style={{ marginLeft: '-1.4rem' }}>/ {perfil === 'MEDICO' ? 'Médico' : 'Paciente'}</span>

        <div className="navbar-links">
          {perfil === 'PACIENTE' ? (
            <>
              <Link to="/paciente" className={`nav-link ${isActive('/paciente') ? 'active' : ''}`}>Início</Link>
              <Link to="/paciente/consultas" className={`nav-link ${isActive('/paciente/consultas') ? 'active' : ''}`} id="nav-paciente-consultas">Minhas Consultas</Link>
            </>
          ) : (
            <>
              <Link to="/medico" className={`nav-link ${isActive('/medico') ? 'active' : ''}`}>Início</Link>
              <Link to="/medico/consultas" className={`nav-link ${isActive('/medico/consultas') ? 'active' : ''}`} id="nav-medico-consultas">Minhas Consultas</Link>
              <Link to="/medico/disponibilidade" className={`nav-link ${isActive('/medico/disponibilidade') ? 'active' : ''}`}>Disponibilidade</Link>
            </>
          )}
        </div>
      </div>
      <div className="navbar-right">
        <span className="navbar-email">{email}</span>
        <button className="btn btn-ghost" onClick={handleLogout} id="btn-logout">
          <LogOut size={16} /> Sair
        </button>
      </div>
    </nav>
  );
}
