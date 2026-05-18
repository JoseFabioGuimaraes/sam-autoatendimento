import { useState, useEffect, useCallback } from 'react';
import { api } from '../../services/api';
import { useToast } from '../../contexts/ToastContext';
import { DIAS_SEMANA, formatTime } from '../../utils/helpers';
import { Calendar, Plus, Trash2, Edit2, ArrowLeft } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import './Medico.css';

export default function DisponibilidadePage() {
  const [faixas, setFaixas] = useState([]);
  const [cancelamentos, setCancelamentos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modal, setModal] = useState(false);
  const [modalCancelamento, setModalCancelamento] = useState(false);
  const [editId, setEditId] = useState(null);
  const [form, setForm] = useState({ diaSemana: 'SEG', horaInicio: '08:00', horaFim: '12:00', duracaoSlotMin: 30 });
  const [formCancelamento, setFormCancelamento] = useState({ tipoCancelamento: 'DIA_COMPLETO', data: '', horaInicio: '', horaFim: '', motivoCancelamento: '' });
  const toast = useToast();
  const navigate = useNavigate();

  const carregar = useCallback(async () => {
    setLoading(true);
    try {
      const [resDisp, resCanc] = await Promise.all([
        api.listarDisponibilidade(),
        api.listarCancelamentos()
      ]);
      setFaixas(resDisp || []);
      setCancelamentos(resCanc || []);
    } catch { toast.error('Erro ao carregar dados da agenda'); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { carregar(); }, [carregar]);

  const set = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  const openNew = () => {
    setEditId(null);
    setForm({ diaSemana: 'SEG', horaInicio: '08:00', horaFim: '12:00', duracaoSlotMin: 30 });
    setModal(true);
  };

  const openEdit = (f) => {
    setEditId(f.id);
    setForm({ diaSemana: f.diaSemana, horaInicio: formatTime(f.horaInicio), horaFim: formatTime(f.horaFim), duracaoSlotMin: f.duracaoSlotMin });
    setModal(true);
  };

  const salvar = async () => {
    const payload = { ...form, horaInicio: form.horaInicio + ':00', horaFim: form.horaFim + ':00', duracaoSlotMin: Number(form.duracaoSlotMin) };
    try {
      if (editId) {
        await api.atualizarDisponibilidade(editId, payload);
        toast.success('Faixa atualizada!');
      } else {
        await api.criarDisponibilidade(payload);
        toast.success('Faixa criada!');
      }
      setModal(false);
      carregar();
    } catch (err) { toast.error(err.message || 'Erro ao salvar'); }
  };

  const desativar = async (id) => {
    if (!confirm('Deseja desativar esta faixa?')) return;
    try {
      await api.desativarDisponibilidade(id);
      toast.success('Faixa desativada');
      carregar();
    } catch (err) { toast.error(err.message || 'Erro ao desativar'); }
  };

  const setCanc = (field) => (e) => setFormCancelamento({ ...formCancelamento, [field]: e.target.value });

  const salvarCancelamento = async () => {
    if (formCancelamento.motivoCancelamento.length < 10) {
      toast.error('Motivo deve ter no mínimo 10 caracteres'); return;
    }
    const payload = { ...formCancelamento };
    if (payload.horaInicio) payload.horaInicio += ':00';
    if (payload.horaFim) payload.horaFim += ':00';
    try {
      await api.registrarCancelamento(payload);
      toast.success('Agenda cancelada com sucesso!');
      setModalCancelamento(false);
      carregar();
    } catch (err) { toast.error(err.message || 'Erro ao cancelar agenda'); }
  };

  const reverterCanc = async (id) => {
    if (!confirm('Deseja reverter este cancelamento? As consultas voltarão a ficar ativas.')) return;
    try {
      await api.reverterCancelamento(id);
      toast.success('Cancelamento revertido');
      carregar();
    } catch (err) { toast.error(err.message || 'Erro ao reverter'); }
  };

  return (
    <div>
      <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap' }}>
        <div>
          <h1><Calendar size={28} style={{ verticalAlign: 'middle', marginRight: 8 }} />Minha Agenda</h1>
          <p>Gerencie suas faixas de disponibilidade</p>
        </div>
        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <button className="btn btn-outline" onClick={() => navigate('/medico')}>
            <ArrowLeft size={16} /> Voltar ao Início
          </button>
          <button className="btn btn-primary" onClick={openNew} id="btn-nova-disp">
            <Plus size={16} /> Nova Faixa
          </button>
        </div>
      </div>

      {loading ? <div className="loading-spinner" /> : faixas.length === 0 ? (
        <div className="empty-state"><Calendar size={48} /><p>Nenhuma disponibilidade cadastrada</p></div>
      ) : (
        <div className="grid-2">
          {faixas.map((f) => (
            <div key={f.id} className="card disp-card">
              <div className="disp-info">
                <h3>{DIAS_SEMANA[f.diaSemana] || f.diaSemana}</h3>
                <p>{formatTime(f.horaInicio)} — {formatTime(f.horaFim)} · {f.duracaoSlotMin}min</p>
                <span className={`disp-badge ${f.ativo ? 'active' : 'inactive'}`}>
                  {f.ativo ? 'Ativo' : 'Inativo'}
                </span>
              </div>
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <button className="btn btn-ghost btn-sm" onClick={() => openEdit(f)}><Edit2 size={14} /></button>
                {f.ativo && <button className="btn btn-ghost btn-sm" onClick={() => desativar(f.id)} style={{ color: 'var(--danger)' }}><Trash2 size={14} /></button>}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Cancelamentos */}
      <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', marginTop: '2rem' }}>
        <div>
          <h2>Bloqueios e Cancelamentos</h2>
          <p>Dias ou faixas onde você não atenderá</p>
        </div>
        <button className="btn btn-outline" onClick={() => {
          setFormCancelamento({ tipoCancelamento: 'DIA_COMPLETO', data: '', horaInicio: '', horaFim: '', motivoCancelamento: '' });
          setModalCancelamento(true);
        }}>
          Bloquear Agenda
        </button>
      </div>

      {cancelamentos.length === 0 ? (
        <div className="empty-state"><p>Nenhum cancelamento futuro registrado</p></div>
      ) : (
        <div className="grid-2">
          {cancelamentos.map((c) => (
            <div key={c.id} className="card disp-card">
              <div className="disp-info">
                <h3>{c.data.split('-').reverse().join('/')}</h3>
                <p>
                  {c.tipoCancelamento === 'DIA_COMPLETO' ? 'Dia completo' :
                   c.tipoCancelamento === 'TURNO' ? `Turno (${formatTime(c.horaInicio)} — ${formatTime(c.horaFim)})` :
                   `Slot específico (${formatTime(c.horaInicio)})`}
                </p>
                <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Motivo: {c.motivoCancelamento}</p>
              </div>
              <button className="btn btn-ghost btn-sm" style={{ color: 'var(--danger)' }} onClick={() => reverterCanc(c.id)}>
                <Trash2 size={14} /> Reverter
              </button>
            </div>
          ))}
        </div>
      )}

      {/* Modal Cancelamento */}
      {modalCancelamento && (
        <div className="modal-overlay" onClick={(e) => e.target === e.currentTarget && setModalCancelamento(false)}>
          <div className="modal">
            <h2>Bloquear Agenda</h2>
            <div className="form-group">
              <label>Tipo de cancelamento</label>
              <select value={formCancelamento.tipoCancelamento} onChange={setCanc('tipoCancelamento')}>
                <option value="DIA_COMPLETO">Dia Completo</option>
                <option value="TURNO">Turno / Faixa de horário</option>
                <option value="HORARIO_ESPECIFICO">Horário Específico (Slot)</option>
              </select>
            </div>
            <div className="form-group">
              <label>Data</label>
              <input type="date" value={formCancelamento.data} onChange={setCanc('data')} min={new Date().toISOString().split('T')[0]} />
            </div>
            
            {formCancelamento.tipoCancelamento !== 'DIA_COMPLETO' && (
              <div style={{ display: 'flex', gap: '0.75rem' }}>
                <div className="form-group" style={{ flex: 1 }}>
                  <label>Hora Início</label>
                  <input type="time" value={formCancelamento.horaInicio} onChange={setCanc('horaInicio')} />
                </div>
                {formCancelamento.tipoCancelamento === 'TURNO' && (
                  <div className="form-group" style={{ flex: 1 }}>
                    <label>Hora Fim</label>
                    <input type="time" value={formCancelamento.horaFim} onChange={setCanc('horaFim')} />
                  </div>
                )}
              </div>
            )}

            <div className="form-group">
              <label>Motivo do cancelamento (mín. 10 caracteres)</label>
              <textarea value={formCancelamento.motivoCancelamento} onChange={setCanc('motivoCancelamento')} 
                placeholder="Ex: Congresso médico em São Paulo" style={{ minHeight: 80, resize: 'vertical' }} />
            </div>

            <div className="modal-actions">
              <button className="btn btn-outline" onClick={() => setModalCancelamento(false)}>Voltar</button>
              <button className="btn btn-primary" onClick={salvarCancelamento}>Confirmar Bloqueio</button>
            </div>
          </div>
        </div>
      )}

      {/* Modal Faixa de Disponibilidade */}
      {modal && (
        <div className="modal-overlay" onClick={(e) => e.target === e.currentTarget && setModal(false)}>
          <div className="modal">
            <h2>{editId ? 'Editar Faixa' : 'Nova Faixa de Horário'}</h2>
            <div className="form-group">
              <label>Dia da semana</label>
              <select value={form.diaSemana} onChange={set('diaSemana')}>
                {Object.entries(DIAS_SEMANA).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
              </select>
            </div>
            <div style={{ display: 'flex', gap: '0.75rem' }}>
              <div className="form-group" style={{ flex: 1 }}>
                <label>Início</label>
                <input type="time" value={form.horaInicio} onChange={set('horaInicio')} />
              </div>
              <div className="form-group" style={{ flex: 1 }}>
                <label>Fim</label>
                <input type="time" value={form.horaFim} onChange={set('horaFim')} />
              </div>
            </div>
            <div className="form-group">
              <label>Duração do slot (minutos)</label>
              <input type="number" value={form.duracaoSlotMin} onChange={set('duracaoSlotMin')} min={10} />
            </div>
            <div className="modal-actions">
              <button className="btn btn-outline" onClick={() => setModal(false)}>Cancelar</button>
              <button className="btn btn-primary" onClick={salvar} id="btn-salvar-disp">Salvar</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
