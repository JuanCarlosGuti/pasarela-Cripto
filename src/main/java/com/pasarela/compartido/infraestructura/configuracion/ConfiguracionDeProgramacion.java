package com.pasarela.compartido.infraestructura.configuracion;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Habilita los jobs programados del monolito (expiración, reconciliación). */
@Configuration
@EnableScheduling
public class ConfiguracionDeProgramacion {

}
