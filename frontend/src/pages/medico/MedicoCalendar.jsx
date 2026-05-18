import { useState, useMemo } from 'react';
import { ChevronLeft, ChevronRight, Calendar, User, Clock, CheckCircle2, XCircle } from 'lucide-react';
import { statusBadge, formatDateTime, checkinBadge } from '../../utils/helpers';
import '../paciente/ConsultasCalendar.css'; // Reusing base CSS classes

export default function MedicoCalendar({ 
  consultas, 
  onAprovar, 
  onRecusar, 
  onRealizar, 
  onResponderCheckin,
  setCheckinId,
  setRecusaId
}) {
  const [currentDate, setCurrentDate] = useState(new Date());
  const [selectedDate, setSelectedDate] = useState(new Date());

  const daysInMonth = new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 0).getDate();
  const firstDayOfMonth = new Date(currentDate.getFullYear(), currentDate.getMonth(), 1).getDay();

  const prevMonth = () => setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() - 1, 1));
  const nextMonth = () => setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 1));

  const monthNames = ["Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"];

  // Mapping days to status types for dots
  const consultasPorDia = useMemo(() => {
    const mapa = {};
    consultas.forEach(c => {
      const dataObj = new Date(c.dataHora);
      if (dataObj.getFullYear() === currentDate.getFullYear() && dataObj.getMonth() === currentDate.getMonth()) {
        const dia = dataObj.getDate();
        if (!mapa[dia]) mapa[dia] = [];
        
        let tipo = 'neutro';
        if (['APROVADA', 'AGUARDANDO_APROVACAO'].includes(c.status)) tipo = 'ativa';
        else if (['CANCELADA_PELO_MEDICO', 'CANCELADA_PELO_PACIENTE', 'RECUSADA'].includes(c.status)) tipo = 'cancelada';
        else if (c.status === 'REALIZADA') tipo = 'realizada';
        
        mapa[dia].push(tipo);
      }
    });
    return mapa;
  }, [consultas, currentDate]);

  // Consultations for the selected date
  const consultasDoDiaSelecionado = useMemo(() => {
    return consultas.filter(c => {
      const d = new Date(c.dataHora);
      return d.getDate() === selectedDate.getDate() &&
             d.getMonth() === selectedDate.getMonth() &&
             d.getFullYear() === selectedDate.getFullYear();
    }).sort((a, b) => new Date(a.dataHora) - new Date(b.dataHora));
  }, [consultas, selectedDate]);

  const selectDay = (day) => {
    setSelectedDate(new Date(currentDate.getFullYear(), currentDate.getMonth(), day));
  };

  const renderDays = () => {
    const days = [];
    const today = new Date();
    
    // Fill empty slots before 1st day
    for (let i = 0; i < firstDayOfMonth; i++) {
      days.push(<div key={`empty-${i}`} className="calendar-day empty"></div>);
    }

    // Fill days of the month
    for (let d = 1; d <= daysInMonth; d++) {
      const isToday = d === today.getDate() && currentDate.getMonth() === today.getMonth() && currentDate.getFullYear() === today.getFullYear();
      const isSelected = d === selectedDate.getDate() && currentDate.getMonth() === selectedDate.getMonth() && currentDate.getFullYear() === selectedDate.getFullYear();
      const eventos = consultasPorDia[d] || [];
      
      const hasAtiva = eventos.includes('ativa');
      const hasCancelada = eventos.includes('cancelada');
      const hasRealizada = eventos.includes('realizada');

      days.push(
        <div 
          key={d} 
          onClick={() => selectDay(d)}
          className={`calendar-day ${isToday ? 'today' : ''} ${isSelected ? 'selected' : ''} ${eventos.length > 0 ? 'has-events' : ''}`}
          style={{ 
            cursor: 'pointer',
            border: isSelected ? '2px solid var(--primary)' : isToday ? '2px dotted var(--primary-light)' : 'none',
            backgroundColor: isSelected ? '#f0fdf4' : 'var(--bg-color)'
          }}
        >
          <span className="day-number" style={{ fontWeight: isSelected || isToday ? 'bold' : 'normal' }}>{d}</span>
          <div className="day-dots">
            {hasAtiva && <div className="dot dot-ativa"></div>}
            {hasCancelada && <div className="dot dot-cancelada"></div>}
            {hasRealizada && <div className="dot dot-realizada"></div>}
          </div>
        </div>
      );
    }

    return days;
  };

  return (
    <div style={{ display: 'flex', gap: '2rem', flexWrap: 'wrap', alignItems: 'flex-start' }}>
      
      {/* Calendar Area */}
      <div className="calendar-container card" style={{ flex: '1 1 350px' }}>
        <div className="calendar-header">
          <button className="btn btn-ghost btn-sm" onClick={prevMonth}><ChevronLeft size={18} /></button>
          <h3 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 600 }}>
            {monthNames[currentDate.getMonth()]} {currentDate.getFullYear()}
          </h3>
          <button className="btn btn-ghost btn-sm" onClick={nextMonth}><ChevronRight size={18} /></button>
        </div>
        <div className="calendar-weekdays">
          <div>Dom</div><div>Seg</div><div>Ter</div><div>Qua</div><div>Qui</div><div>Sex</div><div>Sáb</div>
        </div>
        <div className="calendar-grid">
          {renderDays()}
        </div>

        {/* Legend */}
        <div style={{ display: 'flex', gap: '1rem', marginTop: '1.5rem', fontSize: '0.75rem', justifyContent: 'center', color: 'var(--text-muted)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
            <div className="dot dot-ativa"></div> Agendada/Aprovada
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
            <div className="dot dot-realizada"></div> Realizada
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
            <div className="dot dot-cancelada"></div> Recusada/Cancelada
          </div>
        </div>
      </div>

      {/* Selected Day Agenda Detail */}
      <div className="card" style={{ flex: '1 1 450px', minHeight: '350px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1.5rem', borderBottom: '1px solid #e2e8f0', paddingBottom: '0.8rem' }}>
          <Calendar size={20} color="var(--primary)" />
          <h3 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 600 }}>
            Agenda de {selectedDate.toLocaleDateString('pt-BR', { day: '2-digit', month: 'long', year: 'numeric' })}
          </h3>
        </div>

        {consultasDoDiaSelecionado.length === 0 ? (
          <div className="empty-state" style={{ padding: '3rem 1rem' }}>
            Nenhuma consulta agendada para este dia.
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.8rem' }}>
            {consultasDoDiaSelecionado.map(c => {
              const badge = statusBadge(c.status);
              const hora = new Date(c.dataHora).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
              
              return (
                <div key={c.id} className="card" style={{ 
                  padding: '1rem', 
                  borderLeft: `4px solid ${c.status === 'APROVADA' ? 'var(--primary)' : c.status === 'REALIZADA' ? 'var(--text-muted)' : 'var(--danger)'}`,
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '0.6rem'
                }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '0.5rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <Clock size={16} color="var(--text-muted)" />
                      <strong style={{ fontSize: '0.95rem' }}>{hora}</strong>
                      <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>· {c.tipoConsulta === 'RETORNO' ? 'Retorno' : 'Consulta'}</span>
                    </div>
                    <span className={`badge ${badge.cls}`}>{badge.label}</span>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--text)' }}>
                    <User size={16} color="var(--text-secondary)" />
                    <span style={{ fontSize: '0.9rem', fontWeight: 500 }}>{c.pacienteNome}</span>
                  </div>

                  {/* Quick actions direct from calendar! */}
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: '0.4rem', justifyContent: 'flex-end', flexWrap: 'wrap' }}>
                    {c.status === 'AGUARDANDO_APROVACAO' && (
                      <>
                        <button className="btn btn-primary btn-sm" onClick={() => onAprovar(c.id)}>Aprovar</button>
                        <button className="btn btn-danger btn-sm" onClick={() => setRecusaId(c.id)}>Recusar</button>
                      </>
                    )}

                    {c.checkinStatus && (
                      <span className={`badge ${checkinBadge(c.checkinStatus).cls}`} style={{ marginRight: 'auto' }}>
                        {checkinBadge(c.checkinStatus).label}
                      </span>
                    )}

                    {c.checkinStatus === 'AGUARDANDO_CONFIRMACAO' && (
                      <>
                        <button className="btn btn-outline btn-sm" onClick={() => onResponderCheckin(c.id, 'PODE_ENTRAR')}>Pode Entrar</button>
                        <button className="btn btn-outline btn-sm" onClick={() => setCheckinId(c.id)}>Aguardar</button>
                      </>
                    )}

                    {c.status === 'APROVADA' && (
                      <button className="btn btn-outline btn-sm" onClick={() => onRealizar(c.id)}>Finalizar Consulta</button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

    </div>
  );
}
