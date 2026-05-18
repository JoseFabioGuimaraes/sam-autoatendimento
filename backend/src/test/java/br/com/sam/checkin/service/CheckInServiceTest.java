package br.com.sam.checkin.service;

import br.com.sam.checkin.dto.CheckInResponse;
import br.com.sam.checkin.dto.ConfirmarCheckinRequest;
import br.com.sam.checkin.model.CheckIn;
import br.com.sam.checkin.model.StatusCheckin;
import br.com.sam.checkin.repository.CheckInRepository;
import br.com.sam.consulta.model.Consulta;
import br.com.sam.consulta.model.StatusConsulta;
import br.com.sam.consulta.repository.ConsultaRepository;
import br.com.sam.medico.model.Medico;
import br.com.sam.shared.exception.*;
import br.com.sam.usuario.model.PerfilUsuario;
import br.com.sam.usuario.model.Usuario;
import br.com.sam.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CheckInService — Testes Unitários")
class CheckInServiceTest {

    @Mock
    private CheckInRepository checkInRepository;

    @Mock
    private ConsultaRepository consultaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CheckInService checkInService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(checkInService, "janelaMinutos", 30);
    }

    private Usuario criarPaciente() {
        return Usuario.builder().id("paciente-1").email("paciente@test.com").perfil(PerfilUsuario.PACIENTE).build();
    }

    private Usuario criarMedicoUsuario() {
        return Usuario.builder().id("medico-1").email("medico@test.com").perfil(PerfilUsuario.MEDICO).build();
    }

    private Consulta criarConsulta(Usuario paciente, Usuario medicoUsuario) {
        Medico medico = new Medico();
        medico.setId(medicoUsuario.getId());
        medico.setUsuario(medicoUsuario);

        Consulta consulta = new Consulta();
        consulta.setId("consulta-1");
        consulta.setPaciente(paciente);
        consulta.setMedico(medico);
        consulta.setStatus(StatusConsulta.APROVADA);
        consulta.setDataHora(LocalDateTime.now().plusMinutes(15)); // Dentro da janela
        return consulta;
    }

    // T46
    @Test
    @DisplayName("T46 — Paciente faz check-in dentro da janela permitida: AGUARDANDO_CONFIRMACAO")
    void t46_checkinDentroDaJanela() {
        Usuario paciente = criarPaciente();
        Consulta consulta = criarConsulta(paciente, criarMedicoUsuario());

        when(usuarioRepository.findByEmail(paciente.getEmail())).thenReturn(Optional.of(paciente));
        when(consultaRepository.findById(consulta.getId())).thenReturn(Optional.of(consulta));
        when(checkInRepository.findByConsultaId(consulta.getId())).thenReturn(Optional.empty());
        when(checkInRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CheckInResponse response = checkInService.realizarCheckin(paciente.getEmail(), consulta.getId());

        assertThat(response.statusCheckin()).isEqualTo(StatusCheckin.AGUARDANDO_CONFIRMACAO);
    }

    // T47
    @Test
    @DisplayName("T47 — Paciente faz check-in fora da janela (muito cedo): CheckinForaDaJanelaException")
    void t47_checkinForaDaJanela() {
        Usuario paciente = criarPaciente();
        Consulta consulta = criarConsulta(paciente, criarMedicoUsuario());
        consulta.setDataHora(LocalDateTime.now().plusHours(2));

        when(usuarioRepository.findByEmail(paciente.getEmail())).thenReturn(Optional.of(paciente));
        when(consultaRepository.findById(consulta.getId())).thenReturn(Optional.of(consulta));

        assertThatThrownBy(() -> checkInService.realizarCheckin(paciente.getEmail(), consulta.getId()))
                .isInstanceOf(CheckinForaDaJanelaException.class);
    }

    // T48
    @Test
    @DisplayName("T48 — Paciente faz check-in em consulta não APROVADA: TransicaoInvalidaException")
    void t48_checkinConsultaNaoAprovada() {
        Usuario paciente = criarPaciente();
        Consulta consulta = criarConsulta(paciente, criarMedicoUsuario());
        consulta.setStatus(StatusConsulta.AGUARDANDO_APROVACAO);

        when(usuarioRepository.findByEmail(paciente.getEmail())).thenReturn(Optional.of(paciente));
        when(consultaRepository.findById(consulta.getId())).thenReturn(Optional.of(consulta));

        assertThatThrownBy(() -> checkInService.realizarCheckin(paciente.getEmail(), consulta.getId()))
                .isInstanceOf(TransicaoInvalidaException.class);
    }

    // T49
    @Test
    @DisplayName("T49 — Paciente faz check-in duplicado: CheckinDuplicadoException")
    void t49_checkinDuplicado() {
        Usuario paciente = criarPaciente();
        Consulta consulta = criarConsulta(paciente, criarMedicoUsuario());

        when(usuarioRepository.findByEmail(paciente.getEmail())).thenReturn(Optional.of(paciente));
        when(consultaRepository.findById(consulta.getId())).thenReturn(Optional.of(consulta));
        when(checkInRepository.findByConsultaId(consulta.getId())).thenReturn(Optional.of(new CheckIn()));

        assertThatThrownBy(() -> checkInService.realizarCheckin(paciente.getEmail(), consulta.getId()))
                .isInstanceOf(CheckinDuplicadoException.class);
    }

    // T50
    @Test
    @DisplayName("T50 — Paciente faz check-in em consulta de outro paciente: AcessoNegadoException")
    void t50_checkinConsultaOutroPaciente() {
        Usuario paciente = criarPaciente();
        Usuario outroPaciente = Usuario.builder().id("paciente-2").build();
        Consulta consulta = criarConsulta(outroPaciente, criarMedicoUsuario());

        when(usuarioRepository.findByEmail(paciente.getEmail())).thenReturn(Optional.of(paciente));
        when(consultaRepository.findById(consulta.getId())).thenReturn(Optional.of(consulta));

        assertThatThrownBy(() -> checkInService.realizarCheckin(paciente.getEmail(), consulta.getId()))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    @DisplayName("Paciente faz re-checkin quando está no status AGUARDAR: transita para AGUARDANDO_CONFIRMACAO")
    void checkinAguardarPermiteRecheckin() {
        Usuario paciente = criarPaciente();
        Consulta consulta = criarConsulta(paciente, criarMedicoUsuario());
        CheckIn existingCheckIn = CheckIn.builder()
                .id("checkin-1")
                .consulta(consulta)
                .paciente(paciente)
                .statusCheckin(StatusCheckin.AGUARDAR)
                .justificativaEspera("Atraso de 15 minutos")
                .build();

        when(usuarioRepository.findByEmail(paciente.getEmail())).thenReturn(Optional.of(paciente));
        when(consultaRepository.findById(consulta.getId())).thenReturn(Optional.of(consulta));
        when(checkInRepository.findByConsultaId(consulta.getId())).thenReturn(Optional.of(existingCheckIn));
        when(checkInRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CheckInResponse response = checkInService.realizarCheckin(paciente.getEmail(), consulta.getId());

        assertThat(response.statusCheckin()).isEqualTo(StatusCheckin.AGUARDANDO_CONFIRMACAO);
        assertThat(existingCheckIn.getJustificativaEspera()).isNull();
    }

    // T51
    @Test
    @DisplayName("T51 — Médico responde PODE_ENTRAR a check-in pendente: status PODE_ENTRAR")
    void t51_medicoRespondePodeEntrar() {
        Usuario medico = criarMedicoUsuario();
        Consulta consulta = criarConsulta(criarPaciente(), medico);
        CheckIn checkIn = CheckIn.builder()
                .consulta(consulta)
                .paciente(consulta.getPaciente())
                .statusCheckin(StatusCheckin.AGUARDANDO_CONFIRMACAO).build();

        when(usuarioRepository.findByEmail(medico.getEmail())).thenReturn(Optional.of(medico));
        when(consultaRepository.findById(consulta.getId())).thenReturn(Optional.of(consulta));
        when(checkInRepository.findByConsultaId(consulta.getId())).thenReturn(Optional.of(checkIn));

        ConfirmarCheckinRequest req = new ConfirmarCheckinRequest(StatusCheckin.PODE_ENTRAR, null);
        CheckInResponse response = checkInService.confirmarCheckin(medico.getEmail(), consulta.getId(), req);

        assertThat(response.statusCheckin()).isEqualTo(StatusCheckin.PODE_ENTRAR);
    }

    // T52
    @Test
    @DisplayName("T52 — Médico responde AGUARDAR com justificativa válida: status AGUARDAR")
    void t52_medicoRespondeAguardarComJustificativa() {
        Usuario medico = criarMedicoUsuario();
        Consulta consulta = criarConsulta(criarPaciente(), medico);
        CheckIn checkIn = CheckIn.builder()
                .consulta(consulta)
                .paciente(consulta.getPaciente())
                .statusCheckin(StatusCheckin.AGUARDANDO_CONFIRMACAO).build();

        when(usuarioRepository.findByEmail(medico.getEmail())).thenReturn(Optional.of(medico));
        when(consultaRepository.findById(consulta.getId())).thenReturn(Optional.of(consulta));
        when(checkInRepository.findByConsultaId(consulta.getId())).thenReturn(Optional.of(checkIn));

        ConfirmarCheckinRequest req = new ConfirmarCheckinRequest(StatusCheckin.AGUARDAR, "Atraso de 10 min.");
        CheckInResponse response = checkInService.confirmarCheckin(medico.getEmail(), consulta.getId(), req);

        assertThat(response.statusCheckin()).isEqualTo(StatusCheckin.AGUARDAR);
        assertThat(checkIn.getJustificativaEspera()).isEqualTo("Atraso de 10 min.");
    }

    // T53
    @Test
    @DisplayName("T53 — Médico responde AGUARDAR sem justificativa: JustificativaObrigatoriaException")
    void t53_medicoRespondeAguardarSemJustificativa() {
        Usuario medico = criarMedicoUsuario();
        Consulta consulta = criarConsulta(criarPaciente(), medico);
        CheckIn checkIn = CheckIn.builder()
                .consulta(consulta)
                .paciente(consulta.getPaciente())
                .statusCheckin(StatusCheckin.AGUARDANDO_CONFIRMACAO).build();

        when(usuarioRepository.findByEmail(medico.getEmail())).thenReturn(Optional.of(medico));
        when(consultaRepository.findById(consulta.getId())).thenReturn(Optional.of(consulta));
        when(checkInRepository.findByConsultaId(consulta.getId())).thenReturn(Optional.of(checkIn));

        ConfirmarCheckinRequest req = new ConfirmarCheckinRequest(StatusCheckin.AGUARDAR, "Curta");
        
        assertThatThrownBy(() -> checkInService.confirmarCheckin(medico.getEmail(), consulta.getId(), req))
                .isInstanceOf(JustificativaObrigatoriaException.class);
    }

    // T54
    @Test
    @DisplayName("T54 — Médico tenta responder a check-in já respondido: CheckinJaRespondidoException")
    void t54_medicoTentaResponderCheckinJaRespondido() {
        Usuario medico = criarMedicoUsuario();
        Consulta consulta = criarConsulta(criarPaciente(), medico);
        CheckIn checkIn = CheckIn.builder()
                .consulta(consulta)
                .paciente(consulta.getPaciente())
                .statusCheckin(StatusCheckin.PODE_ENTRAR).build();

        when(usuarioRepository.findByEmail(medico.getEmail())).thenReturn(Optional.of(medico));
        when(consultaRepository.findById(consulta.getId())).thenReturn(Optional.of(consulta));
        when(checkInRepository.findByConsultaId(consulta.getId())).thenReturn(Optional.of(checkIn));

        ConfirmarCheckinRequest req = new ConfirmarCheckinRequest(StatusCheckin.PODE_ENTRAR, null);

        assertThatThrownBy(() -> checkInService.confirmarCheckin(medico.getEmail(), consulta.getId(), req))
                .isInstanceOf(CheckinJaRespondidoException.class);
    }

    // T55
    @Test
    @DisplayName("T55 — Médico tenta responder check-in de consulta de outro médico: AcessoNegadoException")
    void t55_medicoTentaResponderOutroMedico() {
        Usuario medico = criarMedicoUsuario();
        Usuario outroMedico = Usuario.builder().id("medico-2").email("outro@test.com").perfil(PerfilUsuario.MEDICO).build();
        Consulta consulta = criarConsulta(criarPaciente(), outroMedico);

        when(usuarioRepository.findByEmail(medico.getEmail())).thenReturn(Optional.of(medico));
        when(consultaRepository.findById(consulta.getId())).thenReturn(Optional.of(consulta));

        ConfirmarCheckinRequest req = new ConfirmarCheckinRequest(StatusCheckin.PODE_ENTRAR, null);

        assertThatThrownBy(() -> checkInService.confirmarCheckin(medico.getEmail(), consulta.getId(), req))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    @DisplayName("Médico tenta responder PODE_ENTRAR quando já possui paciente em atendimento: MedicoOcupadoException")
    void medicoTentaResponderPodeEntrarOcupado() {
        Usuario medico = criarMedicoUsuario();
        Consulta consulta = criarConsulta(criarPaciente(), medico);
        CheckIn checkIn = CheckIn.builder()
                .consulta(consulta)
                .paciente(consulta.getPaciente())
                .statusCheckin(StatusCheckin.AGUARDANDO_CONFIRMACAO).build();

        when(usuarioRepository.findByEmail(medico.getEmail())).thenReturn(Optional.of(medico));
        when(consultaRepository.findById(consulta.getId())).thenReturn(Optional.of(consulta));
        when(checkInRepository.findByConsultaId(consulta.getId())).thenReturn(Optional.of(checkIn));
        when(checkInRepository.existsCheckinAtivoParaMedico(medico.getId())).thenReturn(true);

        ConfirmarCheckinRequest req = new ConfirmarCheckinRequest(StatusCheckin.PODE_ENTRAR, null);

        assertThatThrownBy(() -> checkInService.confirmarCheckin(medico.getEmail(), consulta.getId(), req))
                .isInstanceOf(MedicoOcupadoException.class);
    }
}
