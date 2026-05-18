import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { Activity, Calendar, Clock, LogOut, Stethoscope, User, List } from 'lucide-react';
import './Sidebar.css';

export default function Sidebar() {
  const { perfil, email, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => { logout(); navigate('/login'); };

  const pacienteLinks = [
    { to: '/paciente/medicos', icon: <Stethoscope size={18} />, label: 'Médicos' },
    { to: '/paciente/consultas', icon: <Calendar size={18} />, label: 'Minhas Consultas' },
  ];

  const medicoLinks = [
    { to: '/medico/pendentes', icon: <Clock size={18} />, label: 'Pendentes' },
    { to: '/medico/consultas', icon: <List size={18} />, label: 'Todas Consultas' },
    { to: '/medico/disponibilidade', icon: <Calendar size={18} />, label: 'Minha Agenda' },
  ];

  const links = perfil === 'MEDICO' ? medicoLinks : pacienteLinks;

  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <Activity size={28} className="brand-icon" />
        <div>
          <h1>SAM</h1>
          <span>Agendamento Médico</span>
        </div>
      </div>

      <nav className="sidebar-nav">
        {links.map((l) => (
          <NavLink key={l.to} to={l.to} className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
            {l.icon}
            <span>{l.label}</span>
          </NavLink>
        ))}
      </nav>

      <div className="sidebar-footer">
        <div className="user-info">
          <User size={16} />
          <div>
            <p className="user-email">{email}</p>
            <span className="user-perfil">{perfil}</span>
          </div>
        </div>
        <button className="btn btn-ghost" onClick={handleLogout} id="btn-logout">
          <LogOut size={16} /> Sair
        </button>
      </div>
    </aside>
  );
}
