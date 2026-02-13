package com.bancario.compensacion.repositorio;

import com.bancario.compensacion.modelo.DetalleCompensacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DetalleCompensacionRepositorio extends JpaRepository<DetalleCompensacion, Long> {
<<<<<<< HEAD
    List<DetalleCompensacion> findByCicloIdCiclo(Integer idCiclo);
=======
    List<DetalleCompensacion> findByCicloId(Integer cicloId);
>>>>>>> e802ba3afe0f10f8ab8394d63f83f08d1ca3003a

    List<DetalleCompensacion> findByIdInstruccion(UUID idInstruccion);
}
