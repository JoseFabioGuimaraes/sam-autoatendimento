import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { useToast } from '../../contexts/ToastContext';
import { api } from '../../services/api';
import { Activity, UserPlus } from 'lucide-react';
import './Auth.css';

export default function RegisterPage() {
  const [form, setForm] = useState({ nome: '', email: '', senha: '' });
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();

  const set = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const data = await api.register(form);
      login(data);
      toast.success('Conta criada com sucesso!');
      navigate('/paciente/medicos');
    } catch (err) {
      toast.error(err.message || 'Erro ao cadastrar');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-container">
        <div className="auth-header">
          <Activity size={40} className="brand-icon" />
          <h1>SAM</h1>
          <p>Crie sua conta de paciente</p>
        </div>
        <form onSubmit={handleSubmit} className="auth-form" id="register-form">
          <h2>Criar conta</h2>
          <div className="form-group">
            <label htmlFor="nome">Nome completo</label>
            <input id="nome" value={form.nome} onChange={set('nome')} placeholder="Seu nome completo" required minLength={3} />
          </div>
          <div className="form-group">
            <label htmlFor="reg-email">E-mail</label>
            <input id="reg-email" type="email" value={form.email} onChange={set('email')} placeholder="seu@email.com" required />
          </div>
          <div className="form-group">
            <label htmlFor="reg-senha">Senha</label>
            <input id="reg-senha" type="password" value={form.senha} onChange={set('senha')} placeholder="Mínimo 8 caracteres" required minLength={8} />
          </div>
          <button type="submit" className="btn btn-primary btn-full" disabled={loading} id="btn-register">
            {loading ? 'Criando...' : <><UserPlus size={18} /> Cadastrar</>}
          </button>
          <p className="auth-switch">
            Já tem conta? <Link to="/login">Entrar</Link>
          </p>
        </form>
      </div>
    </div>
  );
}
