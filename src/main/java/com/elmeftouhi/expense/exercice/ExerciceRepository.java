package com.elmeftouhi.expense.exercice;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExerciceRepository extends JpaRepository<Exercice, Long> {

    List<Exercice> findAllByTenantIdOrderByStartDateDesc(Long tenantId);

    Optional<Exercice> findByIdAndTenantId(Long id, Long tenantId);

    @Query("SELECT e FROM Exercice e WHERE e.tenantId = :tenantId AND e.startDate <= :endDate AND e.endDate >= :startDate")
    List<Exercice> findOverlappingExercices(
            @Param("tenantId") Long tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT e FROM Exercice e WHERE e.tenantId = :tenantId AND e.startDate <= :date AND e.endDate >= :date")
    Optional<Exercice> findExerciceForDate(
            @Param("tenantId") Long tenantId,
            @Param("date") LocalDate date
    );
}
