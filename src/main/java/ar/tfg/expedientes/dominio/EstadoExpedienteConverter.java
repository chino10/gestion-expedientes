package ar.tfg.expedientes.dominio;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Guarda el estado del expediente como un código de texto en la columna "estado".
 */
@Converter
public class EstadoExpedienteConverter implements AttributeConverter<EstadoExpediente, String> {

    @Override
    public String convertToDatabaseColumn(EstadoExpediente estado) {
        return estado == null ? null : estado.getCodigo();
    }

    @Override
    public EstadoExpediente convertToEntityAttribute(String codigo) {
        return codigo == null ? null : EstadoExpediente.desdeCodigo(codigo);
    }
}
