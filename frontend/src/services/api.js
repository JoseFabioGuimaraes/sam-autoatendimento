const API_BASE = import.meta.env.VITE_API_BASE || '/api/v1';

function getToken() {
  const auth = JSON.parse(localStorage.getItem('sam_auth') || 'null');
  return auth?.token || null;
}

function headers(withAuth = true) {
  const h = { 'Content-Type': 'application/json' };
  if (withAuth) {
    const token = getToken();
    if (token) h['Authorization'] = `Bearer ${token}`;
  }
  return h;
}

async function request(method, path, body = null, auth = true) {
  const opts = { method, headers: headers(auth) };
  if (body) opts.body = JSON.stringify(body);
  const res = await fetch(`${API_BASE}${path}`, opts);
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: 'Erro inesperado' }));
    throw { status: res.status, ...err };
  }
  if (res.status === 204) return null;
  return res.json();
}

export const api = {
  // Auth
  register: (data) => request('POST', '/auth/register', data, false),
  login: (data) => request('POST', '/auth/login', data, false),

  // Médicos
  listarMedicos: (page = 0, size = 20) => request('GET', `/medicos?page=${page}&size=${size}`),
  buscarMedico: (id) => request('GET', `/medicos/${id}`),
  listarSlots: (medicoId, data) => request('GET', `/medicos/${medicoId}/slots?data=${data}`),

  // Disponibilidade (Médico)
  listarDisponibilidade: () => request('GET', '/medicos/disponibilidade'),
  criarDisponibilidade: (data) => request('POST', '/medicos/disponibilidade', data),
  atualizarDisponibilidade: (id, data) => request('PUT', `/medicos/disponibilidade/${id}`, data),
  desativarDisponibilidade: (id) => request('DELETE', `/medicos/disponibilidade/${id}`),

  // Consultas — Paciente
  solicitarConsulta: (data) => request('POST', '/pacientes/consultas', data),
  listarConsultasPaciente: (params = '') => request('GET', `/pacientes/consultas?${params}`),
  cancelarConsulta: (id) => request('PATCH', `/pacientes/consultas/${id}/cancelar`),
  solicitarRetorno: (id, data) => request('POST', `/pacientes/consultas/${id}/retorno`, data),

  // Consultas — Médico
  listarConsultasMedico: (params = '') => request('GET', `/medicos/consultas?${params}`),
  listarPendentes: (page = 0) => request('GET', `/medicos/consultas/pendentes?page=${page}`),
  aprovarConsulta: (id) => request('PATCH', `/medicos/consultas/${id}/aprovar`),
  recusarConsulta: (id, justificativa) => request('PATCH', `/medicos/consultas/${id}/recusar`, { justificativa }),
  realizarConsulta: (id) => request('PATCH', `/medicos/consultas/${id}/realizar`),

  // Check-in — Paciente
  realizarCheckin: (consultaId) => request('POST', `/pacientes/consultas/${consultaId}/checkin`),

  // Check-in — Médico
  consultarCheckin: (consultaId) => request('GET', `/medicos/consultas/${consultaId}/checkin`),
  confirmarCheckin: (consultaId, data) => request('PATCH', `/medicos/consultas/${consultaId}/checkin/confirmar`, data),

  // Cancelamento de Agenda — Médico
  registrarCancelamento: (data) => request('POST', '/medicos/agenda/cancelamentos', data),
  listarCancelamentos: () => request('GET', '/medicos/agenda/cancelamentos'),
  reverterCancelamento: (id) => request('DELETE', `/medicos/agenda/cancelamentos/${id}`),
};
