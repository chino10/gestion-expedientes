package ar.tfg.expedientes.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.security.crypto.password.PasswordEncoder;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

    protected Usuario() {
    }

    public Usuario(String nombre, String email, String passwordHash, Rol rol) {
        this.nombre = nombre;
        this.email = email.trim().toLowerCase();
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.activo = true;
        this.intentosFallidos = 0;
    }

    public boolean verificarClave(String clave, PasswordEncoder codificador) {
        return codificador.matches(clave, passwordHash);
    }

    public void registrarIntentoFallido() {
        intentosFallidos++;
    }

    public void reiniciarIntentos() {
        intentosFallidos = 0;
    }

    public boolean estaBloqueado(int maximoIntentos) {
        return intentosFallidos >= maximoIntentos;
    }

    public void cambiarClave(String nuevoHash) {
        this.passwordHash = nuevoHash;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Rol getRol() {
        return rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }
}
