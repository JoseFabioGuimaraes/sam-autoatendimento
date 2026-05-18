package br.com.sam.shared.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "SAM — Sistema de Agendamento Médico",
                version = "1.0.0",
                description = """
                        API REST do SAM (Sistema de Agendamento Médico), MVP v1.0.
                        
                        ## Autenticação
                        Utilize **POST /api/v1/auth/login** para obter um token JWT.
                        Em seguida, clique em **Authorize** e insira: `Bearer <token>`.
                        
                        ## Perfis
                        - **PACIENTE**: solicita e cancela consultas, solicita retorno.
                        - **MEDICO**: gerencia disponibilidade, aprova/recusa/realiza consultas.
                        
                        ## Fluxo Principal
                        1. Paciente se cadastra → `POST /auth/register`
                        2. Paciente faz login → `POST /auth/login`
                        3. Paciente consulta médicos → `GET /medicos`
                        4. Paciente verifica slots → `GET /medicos/{id}/slots?data=YYYY-MM-DD`
                        5. Paciente solicita consulta → `POST /pacientes/consultas`
                        6. Médico aprova → `PATCH /medicos/consultas/{id}/aprovar`
                        7. Médico marca como realizada → `PATCH /medicos/consultas/{id}/realizar`
                        8. Paciente solicita retorno → `POST /pacientes/consultas/{id}/retorno`
                        """,
                contact = @Contact(name = "SAM Backend", email = "dev@sam.com"),
                license = @License(name = "MIT")
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Servidor local de desenvolvimento")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT Bearer token obtido via POST /api/v1/auth/login"
)
public class OpenApiConfig {
}
