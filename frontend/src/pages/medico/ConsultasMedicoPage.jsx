import { useState, useEffect, useCallback } from 'react';
import { api } from '../../services/api';
import { useToast } from '../../contexts/ToastContext';
import { statusBadge, formatDateTime } from '../../utils/helpers';
import { List, Clock, User, CheckCircle } from 'lucide-react';
import './Medico.css';
import '../paciente/Paciente.css';

export default function ConsultasMedicoPage() {
  const [consultas, setConsultas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('');
  const toast = useToast();

  const carregar = useCallback(async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams({ page: '0', size: '50', sort: 'dataHora,desc' });
      if (statusFilter) params.set('status', statusFilter);
      const res = await api.listarConsultasMedico(params.toString());
      setConsultas(res.content || []);
    } catch { toast.error('Erro ao carregar consultas'); }
    finally { setLoading(false); }
  }, [statusFilter]);

  useEffect(() => { carregar(); }, [carregar]);

  const realizar = async (id) => {
    try {
      await api.realizarConsulta(id);
      toast.success('Consulta marcada como realizada!');
      carregar();
    } catch (err) { toast.error(err.message || 'Erro ao realizar'); }
  };

  return (
    <div>
      <div className="page-header">
        <h1><List size={28} style={{ verticalAlign: 'middle', marginRight: 8 }} />Todas as Consultas</h1>
        <p>Histórico completo das suas consultas</p>
      </div>

      <div className="filter-bar">
        <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <option value="">Todos os status</option>
          <option value="AGUARDANDO_APROVACAO">Aguardando</option>
          <option value="APROVADA">Aprovada</option>
          <option value="RECUSADA">Recusada</option>
          <option value="CANCELADA_PELO_PACIENTE">Cancelada</option>
          <option value="REALIZADA">Realizada</option>
        </select>
        <button className="btn btn-outline btn-sm" onClick={carregar}>Atualizar</button>
      </div>

      {loading ? <div className="loading-spinner" /> : consultas.length === 0 ? (
        <div className="empty-state"><List size={48} /><p>Nenhuma consulta encontrada</p></div>
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
              {c.justificativaRecusa && <div className="consulta-justificativa">💬 {c.justificativaRecusa}</div>}
              {c.status === 'APROVADA' && (
                <div className="consulta-actions">
                  <button className="btn btn-primary btn-sm" onClick={() => realizar(c.id)}>
                    <CheckCircle size={14} /> Marcar como Realizada
                  </button>
                </div>
              )}
            </div>
          );
        })
      )}
    </div>
  );
}
