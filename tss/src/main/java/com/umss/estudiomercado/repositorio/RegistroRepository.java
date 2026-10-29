package com.umss.estudiomercado.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.transaction.annotation.Transactional;

@NoRepositoryBean
public interface RegistroRepository<T> extends JpaRepository<T, Long> {
    @Transactional
    long deleteByOrigenDato(String origenDato);

    long countByOrigenDato(String origenDato);
}
