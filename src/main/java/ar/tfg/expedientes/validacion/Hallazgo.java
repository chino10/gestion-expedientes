package ar.tfg.expedientes.validacion;

import ar.tfg.expedientes.dominio.Requisito;

/** Resultado de aplicar las reglas de validación a un requisito del expediente. */
public record Hallazgo(Requisito requisito, ResultadoRequisito resultado, String mensaje) {
}
