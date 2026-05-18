import { useState, useEffect, useCallback } from 'react';
import { api } from '../../services/api';
import { useToast } from '../../contexts/ToastContext';
import { statusBadge, formatDateTime } from '../../utils/helpers';
import { Calendar, Clock, User, XCircle, RotateCcw } from 'lucide-react';
import AgendarModal from './AgendarModal';
import './Paciente.css';

export default function ConsultasPacientePage() {
  const [consultas, setConsultas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('');
  const [retornoModal, setRetornoModal] = useState(null);
  const toast = useToast();

  const carregar = useCallback(async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams({ page: '0', size: '50', sort: 'dataHora,desc' });
      if (statusFilter) params.set('status', statusFilter);
      const res = await api.listarConsultasPaciente(params.toString());
      setConsultas(res.content || []);
    } catch { toast.error('Erro ao carregar consultas'); }
    finally { setLoading(false); }
  }, [statusFilter]);

  useEffect(() => { carregar(); }, [carregar]);

  const cancelar = async (id) => {
    if (!confirm('Deseja realmente cancelar esta consulta?')) return;
    try {
      await api.cancelarConsulta(id);
      toast.success('Consulta cancelada');
      carregar();
    } catch (err) { toast.error(err.message || 'Erro ao cancelar'); }
  };

  const podeCancelar = (s) => ['AGUARDANDO_APROVACAO', 'APROVADA'].includes(s);
  const podeRetorno = (s) => s === 'REALIZADA';

  return (
    <div>
      <div className="page-header">
        <h1><Calendar size={28} style={{ verticalAlign: 'middle', marginRight: 8 }} />Minhas Consultas</h1>
        <p>Acompanhe suas consultas e agendamentos</p>
      </div>

      <div className="filter-bar">
        <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} id="filter-status">
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
        <div className="empty-state"><Calendar size={48} /><p>Nenhuma consulta encontrada</p></div>
      ) : (
        consultas.map((c) => {
          const badge = statusBadge(c.status);
          return (
            <div key={c.id} className="card consulta-card">
              <div className="consulta-header">
                <h3><User size={16} style={{ marginRight: 4 }} />{c.medicoNome}</h3>
                <span className={`badge ${badge.cls}`}>{badge.label}</span>
              </div>
              <div className="consulta-meta">
                <span><Clock size={14} />{formatDateTime(c.dataHora)}</span>
                <span>Tipo: {c.tipoConsulta === 'RETORNO' ? '🔄 Retorno' : 'Consulta'}</span>
              </div>
              {c.justificativaRecusa && (
                <div className="consulta-justificativa">💬 {c.justificativaRecusa}</div>
              )}
              <div className="consulta-actions">
                {podeCancelar(c.status) && (
                  <button className="btn btn-danger btn-sm" onClick={() => cancelar(c.id)}>
                    <XCircle size={14} /> Cancelar
                  </button>
                )}
                {podeRetorno(c.status) && (
                  <button className="btn btn-outline btn-sm" onClick={() => setRetornoModal(c)}>
                    <RotateCcw size={14} /> Solicitar Retorno
                  </button>
                )}
              </div>
            </div>
          );
        })
      )}

      {retornoModal && (
        <AgendarModal
          medico={{ id: retornoModal.medicoId, nomeCompleto: retornoModal.medicoNome, especialidade: 'Retorno' }}
          consultaOrigemId={retornoModal.id}
          onClose={() => { setRetornoModal(null); carregar(); }}
        />
      )}
    </div>
  );
}
