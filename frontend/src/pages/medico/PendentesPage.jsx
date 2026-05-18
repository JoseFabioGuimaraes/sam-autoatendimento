import { useState, useEffect, useCallback } from 'react';
import { api } from '../../services/api';
import { useToast } from '../../contexts/ToastContext';
import { statusBadge, formatDateTime } from '../../utils/helpers';
import { Clock, CheckCircle, XCircle, User } from 'lucide-react';
import './Medico.css';
import '../paciente/Paciente.css';

export default function PendentesPage() {
  const [consultas, setConsultas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [recusaId, setRecusaId] = useState(null);
  const [justificativa, setJustificativa] = useState('');
  const toast = useToast();

  const carregar = useCallback(async () => {
    setLoading(true);
    try {
      const res = await api.listarPendentes(0);
      setConsultas(res.content || []);
    } catch { toast.error('Erro ao carregar pendentes'); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { carregar(); }, [carregar]);

  const aprovar = async (id) => {
    try {
      await api.aprovarConsulta(id);
      toast.success('Consulta aprovada!');
      carregar();
    } catch (err) { toast.error(err.message || 'Erro ao aprovar'); }
  };

  const recusar = async () => {
    if (!justificativa.trim()) { toast.error('Justificativa obrigatória'); return; }
    try {
      await api.recusarConsulta(recusaId, justificativa);
      toast.success('Consulta recusada');
      setRecusaId(null);
      setJustificativa('');
      carregar();
    } catch (err) { toast.error(err.message || 'Erro ao recusar'); }
  };

  return (
    <div>
      <div className="page-header">
        <h1><Clock size={28} style={{ verticalAlign: 'middle', marginRight: 8 }} />Consultas Pendentes</h1>
        <p>Consultas aguardando sua aprovação</p>
      </div>

      <div className="stats-row">
        <div className="stat-card">
          <div className="stat-value" style={{ color: 'var(--warning)' }}>{consultas.length}</div>
          <div className="stat-label">Pendentes</div>
        </div>
      </div>

      {loading ? <div className="loading-spinner" /> : consultas.length === 0 ? (
        <div className="empty-state"><Clock size={48} /><p>Nenhuma consulta pendente</p></div>
      ) : (
        consultas.map((c) => {
          const badge = statusBadge(c.status);
          return (
            <div key={c.id} className="card consulta-card">
              <div className="consulta-header">
                <h3><User size={16} style={{ marginRight: 4 }} />{c.pacienteNome}</h3>
                <span className={`badge ${badge.cls}`}>{badge.label}</span>
              </div>
              <div className="consulta-meta">
                <span><Clock size={14} />{formatDateTime(c.dataHora)}</span>
                <span>Tipo: {c.tipoConsulta === 'RETORNO' ? '🔄 Retorno' : 'Consulta'}</span>
              </div>
              <div className="consulta-actions">
                <button className="btn btn-success btn-sm" onClick={() => aprovar(c.id)}>
                  <CheckCircle size={14} /> Aprovar
                </button>
                <button className="btn btn-danger btn-sm" onClick={() => setRecusaId(c.id)}>
                  <XCircle size={14} /> Recusar
                </button>
              </div>
            </div>
          );
        })
      )}

      {recusaId && (
        <div className="modal-overlay" onClick={(e) => e.target === e.currentTarget && setRecusaId(null)}>
          <div className="modal">
            <h2>Recusar Consulta</h2>
            <div className="form-group">
              <label>Justificativa (obrigatória)</label>
              <textarea className="recusa-textarea" value={justificativa}
                onChange={(e) => setJustificativa(e.target.value)}
                placeholder="Informe o motivo da recusa..." />
            </div>
            <div className="modal-actions">
              <button className="btn btn-outline" onClick={() => { setRecusaId(null); setJustificativa(''); }}>Cancelar</button>
              <button className="btn btn-danger" onClick={recusar} id="btn-confirmar-recusa">Recusar</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
