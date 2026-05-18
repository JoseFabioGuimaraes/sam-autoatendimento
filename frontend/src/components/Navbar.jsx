import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { LogOut } from 'lucide-react';

export default function Navbar() {
  const { perfil, email, logout } = useAuth();
  const navigate = useNavigate();
  const handleLogout = () => { logout(); navigate('/login'); };

  return (
    <nav className="navbar">
      <div style={{ display: 'flex', alignItems: 'center' }}>
        <div className="navbar-brand">
          <svg width="32" height="32" viewBox="0 0 32 32" fill="none">
            <circle cx="16" cy="16" r="16" fill="#10b981"/>
            <path d="M10 16 L14 20 L22 12" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" fill="none"/>
          </svg>
          SAM
        </div>
        <span className="navbar-perfil">/ {perfil === 'MEDICO' ? 'Médico' : 'Paciente'}</span>
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
