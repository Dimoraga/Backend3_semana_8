package com.duoc.corebankingapi.service;

import com.duoc.corebankingapi.dto.MovimientoResponse;
import com.duoc.corebankingapi.dto.RetiroRequest;
import com.duoc.corebankingapi.dto.RetiroResultado;
import com.duoc.corebankingapi.dto.SaldoResponse;
import com.duoc.corebankingapi.exception.CuentaNoEncontradaException;
import com.duoc.corebankingapi.exception.IdempotencyKeyConflictException;
import com.duoc.corebankingapi.exception.SaldoInsuficienteException;
import com.duoc.corebankingapi.model.Cuenta;
import com.duoc.corebankingapi.model.MovimientoCuenta;
import com.duoc.corebankingapi.model.RetiroIdempotencia;
import com.duoc.corebankingapi.repository.CuentaRepository;
import com.duoc.corebankingapi.repository.MovimientoCuentaRepository;
import com.duoc.corebankingapi.repository.RetiroIdempotenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CuentaService {

    private final CuentaRepository cuentaRepository;
    private final MovimientoCuentaRepository movimientoCuentaRepository;
    private final RetiroIdempotenciaRepository retiroIdempotenciaRepository;

    @Transactional(readOnly = true)
    public SaldoResponse consultarSaldo(Long cuentaId) {
        Cuenta cuenta = cuentaRepository.findById(cuentaId)
                .orElseThrow(() -> new CuentaNoEncontradaException(cuentaId));
        return toSaldoResponse(cuenta);
    }

    @Transactional(readOnly = true)
    public List<MovimientoResponse> movimientosRecientes(Long cuentaId, int limite) {
        if (!cuentaRepository.existsById(cuentaId)) {
            throw new CuentaNoEncontradaException(cuentaId);
        }
        return movimientoCuentaRepository
                .findByCuentaIdOrderByFechaDesc(cuentaId, PageRequest.of(0, limite))
                .stream()
                .map(this::toMovimientoResponse)
                .toList();
    }

    /**
     * Retiro concurrente-seguro: bloquea la fila de la cuenta (SELECT ... FOR
     * UPDATE via CuentaRepository.findByIdForUpdate) durante toda la
     * transaccion, valida saldo, descuenta, y deja constancia en
     * MovimientoCuenta. Si viene idempotencyKey y ya fue usado con el mismo
     * monto, devuelve el resultado ya calculado sin volver a tocar el saldo;
     * si fue usado con un monto distinto, rechaza con
     * IdempotencyKeyConflictException en vez de asumir que es un reintento
     * valido del mismo retiro.
     */
    @Transactional
    public RetiroResultado retirar(Long cuentaId, RetiroRequest request) {
        String idempotencyKey = normalizar(request.idempotencyKey());

        if (idempotencyKey != null) {
            Optional<RetiroResultado> resultadoPrevio = buscarRetiroPrevio(cuentaId, request, idempotencyKey);
            if (resultadoPrevio.isPresent()) {
                return resultadoPrevio.get();
            }
        }

        Cuenta cuenta = cuentaRepository.findByIdForUpdate(cuentaId)
                .orElseThrow(() -> new CuentaNoEncontradaException(cuentaId));

        BigDecimal saldoAnterior = cuenta.getSaldo();
        if (saldoAnterior.compareTo(request.monto()) < 0) {
            throw new SaldoInsuficienteException(cuentaId, saldoAnterior, request.monto());
        }

        BigDecimal saldoNuevo = saldoAnterior.subtract(request.monto());
        cuenta.setSaldo(saldoNuevo);
        cuentaRepository.save(cuenta);

        MovimientoCuenta movimiento = new MovimientoCuenta();
        movimiento.setCuenta(cuenta);
        movimiento.setTipoMovimiento("RETIRO");
        movimiento.setCanal(request.canal());
        movimiento.setMonto(request.monto());
        movimiento.setSaldoResultante(saldoNuevo);
        movimiento.setUsuario(request.usuario());
        movimiento.setIdempotencyKey(idempotencyKey);
        movimiento = movimientoCuentaRepository.save(movimiento);

        if (idempotencyKey != null) {
            RetiroIdempotencia registro = new RetiroIdempotencia();
            registro.setIdempotencyKey(idempotencyKey);
            registro.setCuentaId(cuentaId);
            registro.setMonto(request.monto());
            registro.setSaldoResultante(saldoNuevo);
            registro.setMovimientoId(movimiento.getId());
            retiroIdempotenciaRepository.save(registro);
        }

        return new RetiroResultado(movimiento.getId(), saldoAnterior, saldoNuevo, movimiento.getFecha());
    }

    private Optional<RetiroResultado> buscarRetiroPrevio(Long cuentaId, RetiroRequest request, String idempotencyKey) {
        return retiroIdempotenciaRepository.findById(idempotencyKey).map(previo -> {
            boolean mismaOperacion = previo.getCuentaId().equals(cuentaId)
                    && previo.getMonto().compareTo(request.monto()) == 0;
            if (!mismaOperacion) {
                throw new IdempotencyKeyConflictException(idempotencyKey);
            }
            BigDecimal saldoAnterior = previo.getSaldoResultante().add(previo.getMonto());
            return new RetiroResultado(previo.getMovimientoId(), saldoAnterior,
                    previo.getSaldoResultante(), previo.getFechaCreacion());
        });
    }

    private String normalizar(String idempotencyKey) {
        return (idempotencyKey == null || idempotencyKey.isBlank()) ? null : idempotencyKey.trim();
    }

    private SaldoResponse toSaldoResponse(Cuenta cuenta) {
        return new SaldoResponse(cuenta.getId(), cuenta.getNumeroCuenta(), cuenta.getTitular(),
                cuenta.getTipo(), cuenta.getSaldo(), cuenta.getMoneda());
    }

    private MovimientoResponse toMovimientoResponse(MovimientoCuenta m) {
        return new MovimientoResponse(m.getId(), m.getTipoMovimiento(), m.getCanal(), m.getMonto(),
                m.getSaldoResultante(), m.getFecha(), m.getUsuario());
    }
}
