package ar.tfg.expedientes.servicio;

import ar.tfg.expedientes.config.PropiedadesApp;
import ar.tfg.expedientes.dominio.ReglaDeNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

/**
 * Guarda los archivos de los documentos en el servidor de aplicaciones. Cada versión se
 * almacena con un nombre único, por lo que nunca se sobrescribe un archivo anterior (RNF03).
 */
@Service
public class AlmacenamientoArchivos {

    static final long TAMANIO_MAXIMO = 10L * 1024 * 1024;

    private final Path raiz;

    public AlmacenamientoArchivos(PropiedadesApp propiedades) {
        this.raiz = Path.of(propiedades.almacenamiento()).toAbsolutePath().normalize();
    }

    /** Verifica formato y tamaño (CU05, paso 3) y guarda el archivo. Devuelve la ruta relativa. */
    public String guardar(String numeroExpediente, MultipartFile archivo) {
        verificarFormatoYTamanio(archivo);
        String nombreOriginal = limpiarNombre(archivo.getOriginalFilename());
        Path carpeta = raiz.resolve(numeroExpediente);
        Path destino = carpeta.resolve(UUID.randomUUID() + "__" + nombreOriginal);
        try (InputStream entrada = archivo.getInputStream()) {
            Files.createDirectories(carpeta);
            Files.copy(entrada, destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el archivo", e);
        }
        return raiz.relativize(destino).toString().replace('\\', '/');
    }

    public Path ubicar(String rutaRelativa) {
        Path ruta = raiz.resolve(rutaRelativa).normalize();
        if (!ruta.startsWith(raiz)) {
            throw new ReglaDeNegocioException("Ruta de archivo inválida.");
        }
        return ruta;
    }

    void verificarFormatoYTamanio(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaDeNegocioException("Debe adjuntar un archivo.");
        }
        if (archivo.getSize() > TAMANIO_MAXIMO) {
            throw new ReglaDeNegocioException("El archivo supera el tamaño máximo de 10 MB.");
        }
        String nombre = archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename().toLowerCase(Locale.ROOT);
        byte[] cabecera;
        try (InputStream entrada = archivo.getInputStream()) {
            cabecera = entrada.readNBytes(8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo", e);
        }
        boolean esPdf = nombre.endsWith(".pdf") && empiezaCon(cabecera, 0x25, 0x50, 0x44, 0x46);
        boolean esPng = nombre.endsWith(".png") && empiezaCon(cabecera, 0x89, 0x50, 0x4E, 0x47);
        boolean esJpg = (nombre.endsWith(".jpg") || nombre.endsWith(".jpeg")) && empiezaCon(cabecera, 0xFF, 0xD8, 0xFF);
        if (!esPdf && !esPng && !esJpg) {
            throw new ReglaDeNegocioException("Formato no permitido. Se admiten archivos PDF, JPG o PNG.");
        }
    }

    private static boolean empiezaCon(byte[] datos, int... firma) {
        if (datos.length < firma.length) {
            return false;
        }
        return Arrays.equals(Arrays.copyOf(datos, firma.length),
                toBytes(firma));
    }

    private static byte[] toBytes(int... valores) {
        byte[] bytes = new byte[valores.length];
        for (int i = 0; i < valores.length; i++) {
            bytes[i] = (byte) valores[i];
        }
        return bytes;
    }

    private static String limpiarNombre(String nombre) {
        String base = nombre == null ? "archivo" : Path.of(nombre.replace('\\', '/')).getFileName().toString();
        return base.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
