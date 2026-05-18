import { useState, useEffect, useCallback } from 'react';
import { api } from '../../services/api';
import { useToast } from '../../contexts/ToastContext';
import { statusBadge, formatDateTime, getTodayISO, checkinBadge } from '../../utils/helpers';
import { Calendar, X, RotateCcw, Clock, CheckCircle, AlertCircle } from 'lucide-react';
import SlotsModal from './SlotsModal';
import ConsultasCalendar from './ConsultasCalendar';

export default function PacienteDashboard() {
  const [medicos, setMedicos] = useState([]);
  const [consultas, setConsultas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedMedico, setSelectedMedico] = useState(null);
  const [filtroEspec, setFiltroEspec] = useState('');
  const [activeTab, setActiveTab] = useState('HOJE');
  const [notifiedCheckins, setNotifiedCheckins] = useState([]);
  const [entradaAutorizadaConsulta, setEntradaAutorizadaConsulta] = useState(null);
  const toast = useToast();

  const carregar = useCallback(async () => {
    try {
      const [m, c] = await Promise.all([
        api.listarMedicos(0, 50),
        api.listarConsultasPaciente('page=0&size=50&sort=dataHora,desc'),
      ]);
      setMedicos(m.content || []);
      
      const newConsultas = c.content || [];
      newConsultas.forEach(consulta => {
        if (consulta.checkinStatus === 'PODE_ENTRAR' && consulta.status === 'APROVADA') {
          setNotifiedCheckins(prev => {
            if (!prev.includes(consulta.id)) {
              toast.success(`Entrada Autorizada! O(A) ${consulta.medicoNome} está lhe aguardando no consultório.`);
              setEntradaAutorizadaConsulta(consulta);
              return [...prev, consulta.id];
            }
            return prev;
          });
        }
      });

      setConsultas(newConsultas);
    } catch { toast.error('Erro ao carregar dados'); }
    finally { setLoading(false); }
  }, [toast]);

  useEffect(() => {
    carregar();
    const interval = setInterval(() => {
      carregar();
    }, 4000);
    return () => clearInterval(interval);
  }, [carregar]);

  const cancelar = async (id) => {
    if (!confirm('Cancelar esta consulta?')) return;
    try { await api.cancelarConsulta(id); toast.success('Consulta cancelada'); carregar(); }
    catch (err) { toast.error(err.message || 'Erro'); }
  };

  const fazerCheckin = async (id) => {
    try {
      await api.realizarCheckin(id);
      toast.success('Check-in realizado com sucesso!');
      carregar();
    } catch (err) {
      toast.error(err.message || 'Erro ao realizar check-in');
    }
  };

  const especialidades = [...new Set(medicos.map(m => m.especialidade))];
  const medicosFiltrados = filtroEspec ? medicos.filter(m => m.especialidade === filtroEspec) : medicos;
  const podeCancelar = (s) => ['AGUARDANDO_APROVACAO', 'APROVADA'].includes(s);

  if (loading) return <div className="loading-spinner" />;

  return (
    <div>
      <h1 style={{ fontSize: '1.8rem', fontWeight: 700, marginBottom: '0.3rem' }}>Olá!</h1>
      <p style={{ color: 'var(--text-secondary)', marginBottom: '2rem' }}>Encontre um especialista e agende sua consulta.</p>

      {/* Alerta Real-time de Entrada Autorizada */}
      {consultas.some(c => c.checkinStatus === 'PODE_ENTRAR' && c.status === 'APROVADA' && c.dataHora.startsWith(getTodayISO())) && (
        <div className="card" style={{ background: '#f0fdf4', border: '1px solid #bbf7d0', padding: '1.2rem', marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '1rem', borderRadius: '10px' }}>
          <div style={{ backgroundColor: '#dcfce7', padding: '0.6rem', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <CheckCircle size={28} color="#15803d" />
          </div>
          <div>
            <h3 style={{ margin: 0, color: '#166534', fontSize: '1.1rem', fontWeight: 600 }}>Sua entrada foi autorizada!</h3>
            <p style={{ margin: '0.2rem 0 0 0', color: '#15803d', fontSize: '0.9rem' }}>
              O médico está aguardando você. Pode entrar no consultório!
            </p>
          </div>
        </div>
      )}

      {/* Médicos */}
      <div className="card" style={{ marginBottom: '1.5rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem', flexWrap: 'wrap', gap: '0.5rem' }}>
          <h2 className="section-title" style={{ margin: 0 }}>Médicos disponíveis</h2>
          <select value={filtroEspec} onChange={(e) => setFiltroEspec(e.target.value)}
            style={{ width: 'auto', minWidth: 180, padding: '0.5rem 0.8rem', fontSize: '0.85rem' }}>
            <option value="">Todas especialidades</option>
            {especialidades.map(e => <option key={e} value={e}>{e}</option>)}
          </select>
        </div>
        <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
          {medicosFiltrados.map(m => (
            <div key={m.id} className="card" style={{ display: 'flex', alignItems: 'center', gap: '1rem', flex: '1 1 300px', padding: '1rem' }}>
              <div>
                <strong style={{ fontSize: '0.95rem' }}>{m.nomeCompleto}</strong>
                <p style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>{m.especialidade} · {m.crm}</p>
              </div>
              <button className="btn btn-primary btn-sm" onClick={() => setSelectedMedico(m)} style={{ marginLeft: 'auto' }}>
                Ver horários
              </button>
            </div>
          ))}
        </div>
      </div>

      <div style={{ display: 'flex', gap: '1.5rem', flexWrap: 'wrap', alignItems: 'flex-start' }}>
        {/* Minhas Consultas - Lado Esquerdo */}
        <div className="card" style={{ flex: '1 1 600px' }}>
          <h2 className="section-title">Minhas consultas</h2>
          
          {/* Navegação de Abas */}
          <div style={{ display: 'flex', gap: '1rem', borderBottom: '1px solid #e2e8f0', marginBottom: '1.5rem' }}>
            <button 
              onClick={() => setActiveTab('HOJE')}
              style={{ background: 'none', border: 'none', borderBottom: activeTab === 'HOJE' ? '2px solid var(--primary)' : '2px solid transparent', padding: '0.5rem 1rem', fontWeight: activeTab === 'HOJE' ? 600 : 400, color: activeTab === 'HOJE' ? 'var(--primary)' : 'var(--text-muted)', cursor: 'pointer' }}
            >
              Hoje
            </button>
            <button 
              onClick={() => setActiveTab('AGENDADAS')}
              style={{ background: 'none', border: 'none', borderBottom: activeTab === 'AGENDADAS' ? '2px solid var(--primary)' : '2px solid transparent', padding: '0.5rem 1rem', fontWeight: activeTab === 'AGENDADAS' ? 600 : 400, color: activeTab === 'AGENDADAS' ? 'var(--primary)' : 'var(--text-muted)', cursor: 'pointer' }}
            >
              Agendadas
            </button>
            <button 
              onClick={() => setActiveTab('HISTORICO')}
              style={{ background: 'none', border: 'none', borderBottom: activeTab === 'HISTORICO' ? '2px solid var(--primary)' : '2px solid transparent', padding: '0.5rem 1rem', fontWeight: activeTab === 'HISTORICO' ? 600 : 400, color: activeTab === 'HISTORICO' ? 'var(--primary)' : 'var(--text-muted)', cursor: 'pointer' }}
            >
              Histórico / Canceladas
            </button>
          </div>

          {/* Listagem Baseada na Aba */}
          {(() => {
            let filtradas = [];
            if (activeTab === 'HOJE') {
              filtradas = consultas.filter(c => c.dataHora.startsWith(getTodayISO()) && !['CANCELADA_PELO_PACIENTE', 'CANCELADA_PELO_MEDICO', 'RECUSADA'].includes(c.status));
            } else if (activeTab === 'AGENDADAS') {
              filtradas = consultas.filter(c => !c.dataHora.startsWith(getTodayISO()) && ['AGUARDANDO_APROVACAO', 'APROVADA'].includes(c.status));
            } else {
              filtradas = consultas.filter(c => ['CANCELADA_PELO_PACIENTE', 'CANCELADA_PELO_MEDICO', 'RECUSADA', 'REALIZADA'].includes(c.status) || (!c.dataHora.startsWith(getTodayISO()) && !['AGUARDANDO_APROVACAO', 'APROVADA'].includes(c.status) && !['CANCELADA_PELO_PACIENTE', 'CANCELADA_PELO_MEDICO', 'RECUSADA', 'REALIZADA'].includes(c.status)));
            }

            if (filtradas.length === 0) {
              return <div className="empty-state">Nenhuma consulta encontrada nesta categoria.</div>;
            }

            return filtradas.map(c => {
              const badge = statusBadge(c.status);
              return (
                <div key={c.id} className="card" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '1rem', marginBottom: '0.8rem', flexWrap: 'wrap', gap: '0.8rem', borderLeft: `4px solid ${c.status === 'APROVADA' ? 'var(--primary)' : c.status === 'RECUSADA' ? 'var(--danger)' : '#cbd5e1'}` }}>
                  <div style={{ display: 'flex', alignItems: 'flex-start', gap: '0.8rem' }}>
                    <div style={{ padding: '0.6rem', backgroundColor: '#f8fafc', borderRadius: '8px' }}>
                      <Calendar size={20} color={c.status.includes('CANCELADA') || c.status === 'RECUSADA' ? '#94a3b8' : 'var(--primary)'} />
                    </div>
                    <div>
                      <strong style={{ fontSize: '1rem', display: 'block', marginBottom: '0.2rem', color: c.status.includes('CANCELADA') ? '#94a3b8' : 'inherit' }}>{formatDateTime(c.dataHora)}</strong>
                      <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', margin: 0 }}>{c.medicoNome} · {c.especialidade} · {c.tipoConsulta === 'RETORNO' ? 'Retorno' : 'Consulta'}</p>
                    </div>
                  </div>
                  
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', flexWrap: 'wrap' }}>
                    <span className={`badge ${badge.cls}`}>{badge.label}</span>
                    {podeCancelar(c.status) && !c.checkinStatus && activeTab !== 'HISTORICO' && (
                      <button className="btn btn-outline btn-sm" onClick={() => cancelar(c.id)} title="Cancelar">Cancelar</button>
                    )}
                    {c.status === 'APROVADA' && !c.checkinStatus && c.dataHora.startsWith(getTodayISO()) && (
                      <button className="btn btn-primary btn-sm" onClick={() => fazerCheckin(c.id)}>Fazer Check-in</button>
                    )}
                    {c.checkinStatus && (
                      <span className={`badge ${checkinBadge(c.checkinStatus).cls}`}>
                        {checkinBadge(c.checkinStatus).label}
                      </span>
                    )}
                    {c.checkinStatus === 'AGUARDAR' && (
                      <button className="btn btn-primary btn-sm" onClick={() => fazerCheckin(c.id)}>
                        Pedir para entrar novamente
                      </button>
                    )}
                  </div>
                  {c.checkinStatus === 'AGUARDAR' && c.checkinJustificativaEspera && (
                    <div style={{ width: '100%', marginTop: '0.5rem', padding: '0.6rem 0.8rem', background: '#fffbeb', border: '1px solid #fef3c7', borderRadius: '6px', fontSize: '0.85rem', color: '#b45309', display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                      <Clock size={14} /> <span><strong>Orientação do médico:</strong> {c.checkinJustificativaEspera}</span>
                    </div>
                  )}
                  {c.status === 'RECUSADA' && c.justificativaRecusa && (
                    <div style={{ width: '100%', marginTop: '0.5rem', padding: '0.6rem 0.8rem', background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '6px', fontSize: '0.85rem', color: '#991b1b', display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                      <AlertCircle size={14} /> <span><strong>Motivo da recusa:</strong> {c.justificativaRecusa}</span>
                    </div>
                  )}
                </div>
              );
            });
          })()}
        </div>

        {/* Lado Direito - Calendário */}
        <div style={{ flex: '1 1 300px', minWidth: '300px' }}>
          <ConsultasCalendar consultas={consultas} />
        </div>
      </div>

      {selectedMedico && <SlotsModal medico={selectedMedico} onClose={() => { setSelectedMedico(null); carregar(); }} />}

      {entradaAutorizadaConsulta && (
        <div className="modal-overlay" style={{ zIndex: 9999 }}>
          <div className="modal" style={{ maxWidth: '400px', textAlign: 'center', padding: '2.5rem 1.5rem', borderRadius: '16px' }}>
            <div style={{
              width: '64px',
              height: '64px',
              backgroundColor: '#dcfce7',
              color: '#15803d',
              borderRadius: '50%',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 1.2rem auto',
              boxShadow: '0 0 0 8px #f0fdf4'
            }}>
              <CheckCircle size={36} />
            </div>
            
            <h2 style={{ fontSize: '1.4rem', fontWeight: 700, color: 'var(--text)', marginBottom: '0.6rem' }}>
              Entrada Autorizada!
            </h2>
            
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '2rem', lineHeight: '1.5' }}>
              O(A) <strong>{entradaAutorizadaConsulta.medicoNome}</strong> já está de prontidão e aguarda sua entrada na sala de consulta.
            </p>
            
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem' }}>
              <button 
                className="btn btn-primary btn-full" 
                onClick={() => setEntradaAutorizadaConsulta(null)}
                style={{ padding: '0.75rem', fontWeight: 600 }}
              >
                Entrar no Consultório
              </button>
              <button 
                className="btn btn-outline btn-full" 
                onClick={() => setEntradaAutorizadaConsulta(null)}
                style={{ padding: '0.75rem' }}
              >
                Fechar Aviso
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
