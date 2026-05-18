import { useState } from 'react';
import { api } from '../../services/api';
import { useToast } from '../../contexts/ToastContext';
import { Calendar, Clock } from 'lucide-react';
import { getTodayISO } from '../../utils/helpers';
import './Paciente.css';

export default function AgendarModal({ medico, onClose }) {
  const [data, setData] = useState(getTodayISO());
  const [slots, setSlots] = useState([]);
  const [selected, setSelected] = useState(null);
  const [loading, setLoading] = useState(false);
  const [loadingSlots, setLoadingSlots] = useState(false);
  const toast = useToast();

  const buscarSlots = async () => {
    if (!data) return;
    setLoadingSlots(true);
    setSelected(null);
    try {
      const res = await api.listarSlots(medico.id, data);
      setSlots(res || []);
      if (res.length === 0) toast.info('Nenhum slot disponível nesta data');
    } catch {
      toast.error('Erro ao buscar horários');
    } finally {
      setLoadingSlots(false);
    }
  };

  const confirmar = async () => {
    if (!selected) return;
    setLoading(true);
    try {
      await api.solicitarConsulta({ medicoId: medico.id, dataHora: selected });
      toast.success('Consulta solicitada com sucesso!');
      onClose();
    } catch (err) {
      toast.error(err.message || 'Erro ao solicitar consulta');
    } finally {
      setLoading(false);
    }
  };

  const formatSlotTime = (dt) => {
    const d = new Date(dt);
    return d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
  };

  return (
    <div className="modal-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal">
        <h2><Calendar size={20} style={{ marginRight: 8, verticalAlign: 'middle' }} />Agendar com {medico.nomeCompleto}</h2>
        <p style={{ color: 'var(--primary-light)', fontSize: '0.85rem', marginBottom: '1rem' }}>{medico.especialidade}</p>

        <div className="form-group">
          <label>Selecione a data</label>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <input type="date" value={data} onChange={(e) => setData(e.target.value)} min={getTodayISO()} />
            <button className="btn btn-outline" onClick={buscarSlots} disabled={loadingSlots}>
              {loadingSlots ? '...' : 'Buscar'}
            </button>
          </div>
        </div>

        {slots.length > 0 && (
          <div>
            <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '0.5rem', display: 'block' }}>
              <Clock size={14} style={{ verticalAlign: 'middle', marginRight: 4 }} />{slots.length} horários disponíveis
            </label>
            <div className="slots-grid">
              {slots.map((s) => (
                <button key={s.dataHora} className={`slot-btn ${selected === s.dataHora ? 'selected' : ''}`}
                  onClick={() => setSelected(s.dataHora)}>
                  {formatSlotTime(s.dataHora)}
                </button>
              ))}
            </div>
          </div>
        )}

        <div className="modal-actions">
          <button className="btn btn-outline" onClick={onClose}>Cancelar</button>
          <button className="btn btn-primary" onClick={confirmar} disabled={!selected || loading} id="btn-confirmar-consulta">
            {loading ? 'Solicitando...' : 'Confirmar Consulta'}
          </button>
        </div>
      </div>
    </div>
  );
}
