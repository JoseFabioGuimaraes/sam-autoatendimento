import { useState, useMemo } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import './ConsultasCalendar.css';

export default function ConsultasCalendar({ consultas }) {
  const [currentDate, setCurrentDate] = useState(new Date());

  const daysInMonth = new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 0).getDate();
  const firstDayOfMonth = new Date(currentDate.getFullYear(), currentDate.getMonth(), 1).getDay();

  const prevMonth = () => setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() - 1, 1));
  const nextMonth = () => setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 1));

  const monthNames = ["Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"];

  // Mapping days to status
  // Status: 'APROVADA', 'AGUARDANDO_APROVACAO' -> azul/verde (Ativa)
  // Status: 'CANCELADA_PELO_MEDICO', 'CANCELADA_PELO_PACIENTE', 'RECUSADA' -> vermelho (Cancelada)
  // Status: 'REALIZADA' -> cinza escuro (Realizada)
  const consultasPorDia = useMemo(() => {
    const mapa = {};
    consultas.forEach(c => {
      const dataStr = c.dataHora.split('T')[0];
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
      const eventos = consultasPorDia[d] || [];
      
      // Determine dot colors
      const hasAtiva = eventos.includes('ativa');
      const hasCancelada = eventos.includes('cancelada');
      const hasRealizada = eventos.includes('realizada');

      days.push(
        <div key={d} className={`calendar-day ${isToday ? 'today' : ''} ${eventos.length > 0 ? 'has-events' : ''}`}>
          <span className="day-number">{d}</span>
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
    <div className="calendar-container card">
      <div className="calendar-header">
        <button className="btn btn-ghost btn-sm" onClick={prevMonth}><ChevronLeft size={18} /></button>
        <h3 style={{ margin: 0, fontSize: '1rem' }}>{monthNames[currentDate.getMonth()]} {currentDate.getFullYear()}</h3>
        <button className="btn btn-ghost btn-sm" onClick={nextMonth}><ChevronRight size={18} /></button>
      </div>
      <div className="calendar-weekdays">
        <div>Dom</div><div>Seg</div><div>Ter</div><div>Qua</div><div>Qui</div><div>Sex</div><div>Sáb</div>
      </div>
      <div className="calendar-grid">
        {renderDays()}
      </div>
    </div>
  );
}
