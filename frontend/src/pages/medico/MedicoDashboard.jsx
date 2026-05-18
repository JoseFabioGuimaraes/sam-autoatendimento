import { useState, useEffect, useCallback } from 'react';
import { api } from '../../services/api';
import { useToast } from '../../contexts/ToastContext';
import { statusBadge, formatDateTime, checkinBadge } from '../../utils/helpers';
import { Calendar, CheckCircle, XCircle, Clock, User, FileText, AlertCircle } from 'lucide-react';
import MedicoCalendar from './MedicoCalendar';

export default function MedicoDashboard() {
  const [pendentes, setPendentes] = useState([]);
  const [consultas, setConsultas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [recusaId, setRecusaId] = useState(null);
  const [justificativa, setJustificativa] = useState('');
  const [activeTab, setActiveTab] = useState('ATENDIMENTO');
  const toast = useToast();

  const carregar = useCallback(async () => {
    try {
      const [p, c] = await Promise.all([
        api.listarPendentes(0),
        api.listarConsultasMedico('page=0&size=100&sort=dataHora,desc'),
      ]);
      setPendentes(p.content || []);
      setConsultas(c.content || []);
    } catch { toast.error('Erro ao carregar dados'); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { carregar(); }, [carregar]);

  const aprovar = async (id) => {
    try { await api.aprovarConsulta(id); toast.success('Consulta aprovada!'); carregar(); }
    catch (err) { toast.error(err.message || 'Erro'); }
  };

  const recusar = async () => {
    if (!justificativa.trim()) { toast.error('Justificativa obrigatória'); return; }
    try { await api.recusarConsulta(recusaId, justificativa); toast.success('Consulta recusada'); setRecusaId(null); setJustificativa(''); carregar(); }
    catch (err) { toast.error(err.message || 'Erro'); }
  };

  const realizar = async (id) => {
    try { await api.realizarConsulta(id); toast.success('Consulta realizada!'); carregar(); }
    catch (err) { toast.error(err.message || 'Erro'); }
  };

  const [checkinId, setCheckinId] = useState(null);
  const [checkinJustif, setCheckinJustif] = useState('');

  const responderCheckin = async (id, status, justificativa = '') => {
    try {
      await api.confirmarCheckin(id, { statusCheckin: status, justificativaEspera: justificativa });
      toast.success('Check-in respondido!');
      setCheckinId(null);
      setCheckinJustif('');
      carregar();
    } catch (err) {
      toast.error(err.message || 'Erro ao responder check-in');
    }
  };

  const today = new Date().toLocaleDateString('pt-BR', { weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric' });

  const stats = {
    hoje: consultas.filter(c => { const d = new Date(c.dataHora); const t = new Date(); return d.toDateString() === t.toDateString(); }).length,
    pendentes: pendentes.length,
    aprovadas: consultas.filter(c => c.status === 'APROVADA').length,
    concluidas: consultas.filter(c => c.status === 'REALIZADA').length,
    canceladas: consultas.filter(c => ['CANCELADA_PELO_PACIENTE', 'CANCELADA_PELO_MEDICO', 'RECUSADA'].includes(c.status)).length,
  };

  const historicoConsultas = consultas.filter(c => 
    ['REALIZADA', 'RECUSADA', 'CANCELADA_PELO_PACIENTE', 'CANCELADA_PELO_MEDICO'].includes(c.status)
  );

  if (loading) return <div className="loading-spinner" />;

  return (
    <div>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '1.8rem', fontWeight: 700, marginBottom: '0.2rem' }}>Sua agenda</h1>
          <p style={{ color: 'var(--text-muted)', textTransform: 'capitalize', margin: 0 }}>{today}</p>
        </div>
        <button className="btn btn-outline" onClick={() => window.location.href = '/medico/disponibilidade'}>
          Gerenciar Horários
        </button>
      </div>

      {/* Navigation Tabs */}
      <div style={{ display: 'flex', gap: '1rem', borderBottom: '1px solid #e2e8f0', marginBottom: '1.5rem' }}>
        <button 
          onClick={() => setActiveTab('ATENDIMENTO')}
          style={{ background: 'none', border: 'none', borderBottom: activeTab === 'ATENDIMENTO' ? '2px solid var(--primary)' : '2px solid transparent', padding: '0.5rem 1rem', fontWeight: activeTab === 'ATENDIMENTO' ? 600 : 400, color: activeTab === 'ATENDIMENTO' ? 'var(--primary)' : 'var(--text-muted)', cursor: 'pointer' }}
        >
          Painel de Atendimento
        </button>
        <button 
          onClick={() => setActiveTab('CALENDARIO')}
          style={{ background: 'none', border: 'none', borderBottom: activeTab === 'CALENDARIO' ? '2px solid var(--primary)' : '2px solid transparent', padding: '0.5rem 1rem', fontWeight: activeTab === 'CALENDARIO' ? 600 : 400, color: activeTab === 'CALENDARIO' ? 'var(--primary)' : 'var(--text-muted)', cursor: 'pointer' }}
        >
          Calendário de Horários
        </button>
        <button 
          onClick={() => setActiveTab('HISTORICO')}
          style={{ background: 'none', border: 'none', borderBottom: activeTab === 'HISTORICO' ? '2px solid var(--primary)' : '2px solid transparent', padding: '0.5rem 1rem', fontWeight: activeTab === 'HISTORICO' ? 600 : 400, color: activeTab === 'HISTORICO' ? 'var(--primary)' : 'var(--text-muted)', cursor: 'pointer' }}
        >
          Histórico & Recusas
        </button>
      </div>

      {/* RENDER ACTIVE TAB */}
      {activeTab === 'ATENDIMENTO' && (
        <div>
          {/* Stats */}
          <div style={{ display: 'flex', gap: '0.75rem', marginBottom: '2rem', flexWrap: 'wrap' }}>
            {[
              { label: 'HOJE', value: stats.hoje, color: 'var(--text)' },
              { label: 'PENDENTES', value: stats.pendentes, color: 'var(--primary)' },
              { label: 'APROVADAS', value: stats.aprovadas, color: 'var(--primary)' },
              { label: 'CONCLUÍDAS', value: stats.concluidas, color: 'var(--text)' },
              { label: 'CANCELADAS / RECUSADAS', value: stats.canceladas, color: 'var(--text)' },
            ].map(s => (
              <div key={s.label} className="card" style={{ flex: '1', minWidth: 120, textAlign: 'left', padding: '1rem' }}>
                <div style={{ fontSize: '0.65rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: '0.3rem' }}>{s.label}</div>
                <div style={{ fontSize: '1.5rem', fontWeight: 700, color: s.color }}>{s.value}</div>
              </div>
            ))}
          </div>

          {/* Fila de atendimento / Pendentes */}
          <div className="card" style={{ marginBottom: '1.5rem' }}>
            <h2 className="section-title">Fila de atendimento (Solicitações pendentes)</h2>
            {pendentes.length === 0 ? (
              <div className="empty-state">Nenhum paciente na fila hoje.</div>
            ) : pendentes.map(c => (
              <div key={c.id} className="card" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0.8rem 1rem', marginBottom: '0.5rem', flexWrap: 'wrap', gap: '0.5rem' }}>
                <div>
                  <strong style={{ fontSize: '0.9rem' }}>{formatDateTime(c.dataHora)} · {c.pacienteNome}</strong>
                  <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>{c.tipoConsulta === 'RETORNO' ? 'Retorno' : 'Consulta'}</p>
                </div>
                <div style={{ display: 'flex', gap: '0.4rem' }}>
                  <button className="btn btn-primary btn-sm" onClick={() => aprovar(c.id)}><CheckCircle size={14} /> Aprovar</button>
                  <button className="btn btn-danger btn-sm" onClick={() => setRecusaId(c.id)}><XCircle size={14} /> Recusar</button>
                </div>
              </div>
            ))}
          </div>

          {/* Próximas consultas ativas */}
          <div className="card">
            <h2 className="section-title">Próximas consultas ativas</h2>
            {consultas.filter(c => ['APROVADA', 'AGUARDANDO_APROVACAO'].includes(c.status)).length === 0 ? (
              <div className="empty-state">Nenhuma consulta ativa agendada.</div>
            ) : consultas.filter(c => ['APROVADA', 'AGUARDANDO_APROVACAO'].includes(c.status)).map(c => {
              const badge = statusBadge(c.status);
              return (
                <div key={c.id} className="card" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0.8rem 1rem', marginBottom: '0.5rem', flexWrap: 'wrap', gap: '0.5rem' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <Calendar size={14} color="var(--primary)" />
                    <span style={{ fontSize: '0.9rem' }}>{formatDateTime(c.dataHora)} · {c.pacienteNome}</span>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <span className={`badge ${badge.cls}`}>{badge.label}</span>
                    {c.checkinStatus && (
                      <span className={`badge ${checkinBadge(c.checkinStatus).cls}`}>
                        {checkinBadge(c.checkinStatus).label}
                      </span>
                    )}
                    {c.checkinStatus === 'AGUARDANDO_CONFIRMACAO' && (
                      <>
                        <button className="btn btn-outline btn-sm" onClick={() => responderCheckin(c.id, 'PODE_ENTRAR')}>Pode Entrar</button>
                        <button className="btn btn-outline btn-sm" onClick={() => setCheckinId(c.id)}>Aguardar</button>
                      </>
                    )}
                    {c.status === 'APROVADA' && (
                      <button className="btn btn-outline btn-sm" onClick={() => realizar(c.id)}>Realizada</button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {activeTab === 'CALENDARIO' && (
        <MedicoCalendar
          consultas={consultas}
          onAprovar={aprovar}
          onRecusar={recusar}
          onRealizar={realizar}
          onResponderCheckin={responderCheckin}
          setCheckinId={setCheckinId}
          setRecusaId={setRecusaId}
        />
      )}

      {activeTab === 'HISTORICO' && (
        <div className="card">
          <h2 className="section-title" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1.5rem' }}>
            <FileText size={20} color="var(--primary)" /> Histórico Geral de Atendimentos
          </h2>

          {historicoConsultas.length === 0 ? (
            <div className="empty-state">Nenhuma consulta finalizada ou cancelada registrada no histórico.</div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.8rem' }}>
              {historicoConsultas.map(c => {
                const badge = statusBadge(c.status);
                
                return (
                  <div key={c.id} className="card" style={{ 
                    padding: '1.2rem', 
                    borderLeft: `4px solid ${
                      c.status === 'REALIZADA' ? '#94a3b8' : 'var(--danger)'
                    }`,
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '0.5rem'
                  }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.8rem' }}>
                        <div style={{ padding: '0.5rem', backgroundColor: '#f8fafc', borderRadius: '6px' }}>
                          <Clock size={16} color="var(--text-secondary)" />
                        </div>
                        <div>
                          <strong style={{ fontSize: '0.95rem', display: 'block' }}>{formatDateTime(c.dataHora)}</strong>
                          <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>{c.tipoConsulta === 'RETORNO' ? 'Retorno' : 'Consulta'}</span>
                        </div>
                      </div>
                      <span className={`badge ${badge.cls}`}>{badge.label}</span>
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: '0.2rem' }}>
                      <User size={16} color="var(--text-secondary)" />
                      <span style={{ fontSize: '0.9rem', fontWeight: 500 }}>Paciente: {c.pacienteNome}</span>
                    </div>

                    {/* Motivo da recusa ou cancelamento caso exista */}
                    {c.status === 'RECUSADA' && c.justificativaRecusa && (
                      <div style={{ 
                        marginTop: '0.5rem', 
                        padding: '0.6rem 0.8rem', 
                        background: '#fef2f2', 
                        border: '1px solid #fecaca', 
                        borderRadius: '6px', 
                        fontSize: '0.85rem', 
                        color: '#991b1b', 
                        display: 'flex', 
                        gap: '0.5rem', 
                        alignItems: 'center' 
                      }}>
                        <AlertCircle size={14} /> 
                        <span><strong>Motivo da recusa:</strong> {c.justificativaRecusa}</span>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* Modal Check-in Aguardar */}
      {checkinId && (
        <div className="modal-overlay" onClick={(e) => e.target === e.currentTarget && setCheckinId(null)}>
          <div className="modal">
            <h2>Pedir para Aguardar</h2>
            <div className="form-group" style={{ marginTop: '1rem' }}>
              <label>Justificativa de espera (obrigatória)</label>
              <textarea value={checkinJustif} onChange={(e) => setCheckinJustif(e.target.value)}
                placeholder="Informe o motivo da espera..." style={{ minHeight: 80, resize: 'vertical' }} />
            </div>
            <div className="modal-actions">
              <button className="btn btn-outline" onClick={() => { setCheckinId(null); setCheckinJustif(''); }}>Cancelar</button>
              <button className="btn btn-primary" onClick={() => responderCheckin(checkinId, 'AGUARDAR', checkinJustif)}>Confirmar</button>
            </div>
          </div>
        </div>
      )}

      {/* Modal Recusa */}
      {recusaId && (
        <div className="modal-overlay" onClick={(e) => e.target === e.currentTarget && setRecusaId(null)}>
          <div className="modal">
            <h2>Recusar Consulta</h2>
            <div className="form-group" style={{ marginTop: '1rem' }}>
              <label>Justificativa (obrigatória)</label>
              <textarea value={justificativa} onChange={(e) => setJustificativa(e.target.value)}
                placeholder="Informe o motivo da recusa..." style={{ minHeight: 80, resize: 'vertical' }} />
            </div>
            <div className="modal-actions">
              <button className="btn btn-outline" onClick={() => { setRecusaId(null); setJustificativa(''); }}>Cancelar</button>
              <button className="btn btn-primary" onClick={recusar}>Confirmar Recusa</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
