import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { useToast } from '../../contexts/ToastContext';
import { api } from '../../services/api';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const data = await api.login({ email, senha });
      login(data);
      toast.success('Login realizado com sucesso!');
      navigate(data.perfil === 'MEDICO' ? '/medico' : '/paciente');
    } catch (err) {
      toast.error(err.message || 'Credenciais inválidas');
    } finally {
      setLoading(false);
    }
  };

  const fill = (e, s) => { setEmail(e); setSenha(s); };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'linear-gradient(180deg, #ecfdf5 0%, #f0fdf4 100%)' }}>
      <div style={{ width: '100%', maxWidth: 420 }}>
        <div style={{ textAlign: 'center', marginBottom: '1.5rem' }}>
          <svg width="40" height="40" viewBox="0 0 32 32" fill="none" style={{ marginBottom: 8 }}>
            <circle cx="16" cy="16" r="16" fill="#10b981"/>
            <path d="M10 16 L14 20 L22 12" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" fill="none"/>
          </svg>
          <h1 style={{ fontSize: '1.5rem', fontWeight: 800 }}>SAM</h1>
        </div>

        <div className="card">
          <h2 style={{ fontSize: '1.4rem', fontWeight: 700, marginBottom: '0.3rem' }}>Entrar</h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', marginBottom: '1.5rem' }}>Acesse sua conta para continuar</p>

          <form onSubmit={handleSubmit} id="login-form">
            <div className="form-group">
              <label htmlFor="email">Email</label>
              <input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="seu@email.com" required />
            </div>
            <div className="form-group">
              <label htmlFor="senha">Senha</label>
              <input id="senha" type="password" value={senha} onChange={(e) => setSenha(e.target.value)} placeholder="Sua senha" required />
            </div>
            <button type="submit" className="btn btn-primary btn-full" disabled={loading} id="btn-login"
              style={{ padding: '0.8rem', fontSize: '0.95rem', marginTop: '0.5rem' }}>
              {loading ? '⏳' : 'Entrar'}
            </button>
          </form>

          <p style={{ textAlign: 'center', marginTop: '1rem', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
            Não tem conta? <Link to="/register">Cadastre-se</Link>
          </p>
        </div>

        <div style={{ marginTop: '1rem', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
          <p style={{ marginBottom: '0.5rem' }}>Contas de teste (senha 123456):</p>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem' }}>
            <button className="card" onClick={(e) => { e.preventDefault(); fill('paciente@teste.com', '123456'); }}
              style={{ padding: '0.6rem 0.8rem', cursor: 'pointer', textAlign: 'left', fontSize: '0.8rem' }}>
              <strong style={{ color: 'var(--text)' }}>Paciente</strong><br/>
              <span style={{ color: 'var(--text-muted)' }}>paciente@teste.com</span>
            </button>
            <button className="card" onClick={(e) => { e.preventDefault(); fill('medico@teste.com', '123456'); }}
              style={{ padding: '0.6rem 0.8rem', cursor: 'pointer', textAlign: 'left', fontSize: '0.8rem' }}>
              <strong style={{ color: 'var(--text)' }}>Médico</strong><br/>
              <span style={{ color: 'var(--text-muted)' }}>medico@teste.com</span>
            </button>
            <button className="card" onClick={(e) => { e.preventDefault(); /* mockup doesn't have these backed */ }}
              style={{ padding: '0.6rem 0.8rem', cursor: 'pointer', textAlign: 'left', fontSize: '0.8rem' }}>
              <strong style={{ color: 'var(--text)' }}>Recepção</strong><br/>
              <span style={{ color: 'var(--text-muted)' }}>recepcao@teste.com</span>
            </button>
            <button className="card" onClick={(e) => { e.preventDefault(); /* mockup doesn't have these backed */ }}
              style={{ padding: '0.6rem 0.8rem', cursor: 'pointer', textAlign: 'left', fontSize: '0.8rem' }}>
              <strong style={{ color: 'var(--text)' }}>Admin</strong><br/>
              <span style={{ color: 'var(--text-muted)' }}>admin@teste.com</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
