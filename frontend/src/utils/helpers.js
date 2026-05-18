export function statusBadge(status) {
  const map = {
    AGUARDANDO_APROVACAO: { label: 'Agendado', cls: 'badge-aguardando' },
    APROVADA: { label: 'Agendado', cls: 'badge-aprovada' },
    RECUSADA: { label: 'Recusada', cls: 'badge-recusada' },
    CANCELADA_PELO_PACIENTE: { label: 'Cancelado', cls: 'badge-cancelada' },
    CANCELADA_PELO_MEDICO: { label: 'Cancelado (Médico)', cls: 'badge-cancelada' },
    REALIZADA: { label: 'Concluída', cls: 'badge-realizada' },
  };
  return map[status] || { label: status, cls: '' };
}

export function checkinBadge(status) {
  const map = {
    AGUARDANDO_CONFIRMACAO: { label: 'Aguardando Confirmação', cls: 'badge-aguardando' },
    PODE_ENTRAR: { label: 'Pode Entrar', cls: 'badge-aprovada' },
    AGUARDAR: { label: 'Aguardar', cls: 'badge-recusada' },
  };
  return map[status] || { label: status, cls: '' };
}

export function formatDateTime(dt) {
  if (!dt) return '—';
  const d = new Date(dt);
  return d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' })
    + ' às '
    + d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
}

export function formatDate(dt) {
  if (!dt) return '—';
  return new Date(dt).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

export function formatTime(t) {
  if (!t) return '—';
  return t.substring(0, 5);
}

export const DIAS_SEMANA = {
  SEG: 'Segunda', TER: 'Terça', QUA: 'Quarta',
  QUI: 'Quinta', SEX: 'Sexta', SAB: 'Sábado', DOM: 'Domingo',
};

export function getTodayISO() {
  return new Date().toISOString().split('T')[0];
}
