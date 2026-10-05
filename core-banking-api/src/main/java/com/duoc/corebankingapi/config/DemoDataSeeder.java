package com.duoc.corebankingapi.config;

import com.duoc.corebankingapi.model.Cuenta;
import com.duoc.corebankingapi.model.Tarjeta;
import com.duoc.corebankingapi.model.Usuario;
import com.duoc.corebankingapi.repository.CuentaRepository;
import com.duoc.corebankingapi.repository.TarjetaRepository;
import com.duoc.corebankingapi.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Datos de demo idempotentes (solo inserta si las tablas estan vacias).
 * Los tres id de cuenta (105, 110, 118) se reutilizan a proposito de
 * intereses.csv / cuentas_anuales.csv de bankbatchprocessor para que el
 * dashboard de bbf-web pueda mostrar saldo en vivo junto a los reportes
 * historicos reales de esa cuenta. El titular/saldo aqui son datos propios
 * de esta cuenta viva: intereses.csv trae nombres aleatorios repetidos por
 * cuenta (es data de prueba, no una relacion 1:1 cuenta-titular), asi que
 * no se puede derivar un titular unico desde ahi.
 *
 * Credenciales de demo (documentadas a proposito, es un proyecto de curso):
 * usuario105/usuario110/usuario118, clave "demo1234"; tarjetas
 * 4000-1234-5678-0105 / -0110 / -0118, PIN 1234 / 2345 / 3456.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder implements CommandLineRunner {

    private final CuentaRepository cuentaRepository;
    private final UsuarioRepository usuarioRepository;
    private final TarjetaRepository tarjetaRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        Cuenta c105 = seedCuenta(105L, "CTA-000105", "Diana Prince", "AHORRO", "250000");
        Cuenta c110 = seedCuenta(110L, "CTA-000110", "Charlie Green", "CORRIENTE", "480000");
        Cuenta c118 = seedCuenta(118L, "CTA-000118", "John Doe", "AHORRO", "95000");

        seedUsuario("usuario105", "demo1234", c105);
        seedUsuario("usuario110", "demo1234", c110);
        seedUsuario("usuario118", "demo1234", c118);

        seedTarjeta("4000-1234-5678-0105", "1234", c105);
        seedTarjeta("4000-1234-5678-0110", "2345", c110);
        seedTarjeta("4000-1234-5678-0118", "3456", c118);
    }

    private Cuenta seedCuenta(Long id, String numeroCuenta, String titular, String tipo, String saldo) {
        if (cuentaRepository.existsById(id)) {
            return cuentaRepository.findById(id).orElseThrow();
        }
        Cuenta cuenta = new Cuenta();
        cuenta.setId(id);
        cuenta.setNumeroCuenta(numeroCuenta);
        cuenta.setTitular(titular);
        cuenta.setTipo(tipo);
        cuenta.setSaldo(new BigDecimal(saldo));
        cuenta = cuentaRepository.save(cuenta);
        log.info("Cuenta demo creada: {} ({})", cuenta.getNumeroCuenta(), cuenta.getTitular());
        return cuenta;
    }

    private void seedUsuario(String username, String rawPassword, Cuenta cuenta) {
        if (usuarioRepository.findByUsernameAndActivoTrue(username).isPresent()) {
            return;
        }
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPasswordHash(passwordEncoder.encode(rawPassword));
        usuario.setCuenta(cuenta);
        usuarioRepository.save(usuario);
    }

    private void seedTarjeta(String numeroTarjeta, String pin, Cuenta cuenta) {
        if (tarjetaRepository.findByNumeroTarjetaAndActivaTrue(numeroTarjeta).isPresent()) {
            return;
        }
        Tarjeta tarjeta = new Tarjeta();
        tarjeta.setNumeroTarjeta(numeroTarjeta);
        tarjeta.setPinHash(passwordEncoder.encode(pin));
        tarjeta.setCuenta(cuenta);
        tarjetaRepository.save(tarjeta);
    }
}
