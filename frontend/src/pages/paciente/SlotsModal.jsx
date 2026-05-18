import { useState, useEffect } from 'react';
import { api } from '../../services/api';
import { useToast } from '../../contexts/ToastContext';
import { X, Clock } from 'lucide-react';

export default function SlotsModal({ medico, onClose }) {
  const [slotsByDay, setSlotsByDay] = useState([]);
  const [loading, setLoading] = useState(true);
  const [booking, setBooking] = useState(false);
  const toast = useToast();

  useEffect(() => {
    const fetchSlots = async () => {
      setLoading(true);
      const days = [];
      const today = new Date();
      for (let i = 0; i < 7; i++) {
        const d = new Date(today);
        d.setDate(today.getDate() + i);
        const iso = d.toISOString().split('T')[0];
        try {
          const slots = await api.listarSlots(medico.id, iso);
          if (slots && slots.length > 0) {
            const dayName = d.toLocaleDateString('pt-BR', { weekday: 'short', day: '2-digit', month: '2-digit' });
            days.push({ date: iso, label: dayName, slots });
          }
        } catch { /* skip */ }
      }
      setSlotsByDay(days);
      setLoading(false);
    };
    fetchSlots();
  }, [medico.id]);

  const agendar = async (dataHora) => {
    setBooking(true);
    try {
      await api.solicitarConsulta({ medicoId: medico.id, dataHora });
      toast.success('Consulta solicitada com sucesso!');
      onClose();
    } catch (err) {
      toast.error(err.message || 'Erro ao agendar');
    } finally { setBooking(false); }
  };

  const formatTime = (dt) => new Date(dt).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });

  return (
    <div className="modal-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal" style={{ maxWidth: 650 }}>
        <button className="modal-close" onClick={onClose}><X size={20} /></button>
        <h2>{medico.nomeCompleto}</h2>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginBottom: '1.2rem' }}>
          {medico.especialidade} · {medico.crm} — Selecione um horário disponível
        </p>

        {loading ? <div className="loading-spinner" /> : slotsByDay.length === 0 ? (
          <div className="empty-state">Nenhum horário disponível nos próximos 7 dias</div>
        ) : (
          <div style={{ maxHeight: '55vh', overflowY: 'auto' }}>
            {slotsByDay.map(day => (
              <div key={day.date} style={{ marginBottom: '1.2rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', marginBottom: '0.5rem' }}>
                  <Clock size={14} color="var(--primary)" />
                  <strong style={{ fontSize: '0.85rem' }}>{day.label}</strong>
                </div>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.4rem' }}>
                  {day.slots.map(s => (
                    <button key={s.dataHora} className="btn btn-outline btn-sm"
                      onClick={() => agendar(s.dataHora)} disabled={booking}
                      style={{ minWidth: 60 }}>
                      {formatTime(s.dataHora)}
                    </button>
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
