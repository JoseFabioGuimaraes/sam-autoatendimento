import { useState, useEffect } from 'react';
import { api } from '../../services/api';
import { useToast } from '../../contexts/ToastContext';
import { Stethoscope, Search, Calendar } from 'lucide-react';
import AgendarModal from './AgendarModal';
import './Paciente.css';

export default function MedicosPage() {
  const [medicos, setMedicos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [selectedMedico, setSelectedMedico] = useState(null);
  const toast = useToast();

  useEffect(() => {
    api.listarMedicos(0, 50)
      .then((res) => setMedicos(res.content || []))
      .catch(() => toast.error('Erro ao carregar médicos'))
      .finally(() => setLoading(false));
  }, []);

  const filtered = medicos.filter((m) =>
    m.nomeCompleto.toLowerCase().includes(search.toLowerCase()) ||
    m.especialidade.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div>
      <div className="page-header">
        <h1><Stethoscope size={28} style={{ verticalAlign: 'middle', marginRight: 8 }} />Médicos Disponíveis</h1>
        <p>Encontre um profissional e agende sua consulta</p>
      </div>

      <div className="search-bar">
        <Search size={18} className="search-icon" />
        <input
          type="text" placeholder="Buscar por nome ou especialidade..."
          value={search} onChange={(e) => setSearch(e.target.value)} id="search-medicos"
        />
      </div>

      {loading ? <div className="loading-spinner" /> : (
        <div className="grid-2">
          {filtered.length === 0 ? (
            <div className="empty-state"><p>Nenhum médico encontrado</p></div>
          ) : filtered.map((m) => (
            <div key={m.id} className="card medico-card">
              <div className="medico-avatar">{m.nomeCompleto.charAt(0)}</div>
              <div className="medico-info">
                <h3>{m.nomeCompleto}</h3>
                <p className="medico-espec">{m.especialidade}</p>
                <p className="medico-crm">CRM: {m.crm}</p>
              </div>
              <button className="btn btn-primary btn-sm" onClick={() => setSelectedMedico(m)} id={`btn-agendar-${m.id}`}>
                <Calendar size={14} /> Agendar
              </button>
            </div>
          ))}
        </div>
      )}

      {selectedMedico && (
        <AgendarModal medico={selectedMedico} onClose={() => setSelectedMedico(null)} />
      )}
    </div>
  );
}
