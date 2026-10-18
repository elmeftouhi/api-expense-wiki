package com.elmeftouhi.expense.exercice.service;

import com.elmeftouhi.expense.exercice.entity.Exercice;
import com.elmeftouhi.expense.exercice.repository.ExerciceRepository;
import com.elmeftouhi.expense.exercice.ExerciceStatus;
import com.elmeftouhi.expense.exercice.dto.ExerciceCreateRequest;
import com.elmeftouhi.expense.exercice.dto.ExerciceResponse;
import com.elmeftouhi.expense.multitenancy.TenantContext;
import com.elmeftouhi.expense.shared.exception.BadRequestException;
import com.elmeftouhi.expense.shared.exception.ResourceNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExerciceService {

    private final ExerciceRepository exerciceRepository;

    @Transactional(readOnly = true)
    public List<ExerciceResponse> findAll() {
        Long tenantId = TenantContext.getRequiredTenantId();
        return exerciceRepository.findAllByTenantIdOrderByStartDateDesc(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ExerciceResponse create(ExerciceCreateRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();

        if (request.startDate().isAfter(request.endDate())) {
            throw new BadRequestException("Start date must be before or equal to end date");
        }

        // Check date overlap boundaries
        List<Exercice> overlaps = exerciceRepository.findOverlappingExercices(
                tenantId, request.startDate(), request.endDate()
        );
        if (!overlaps.isEmpty()) {
            throw new BadRequestException("Fiscal year dates overlap with an existing Exercice: " + overlaps.get(0).getName());
        }

        Exercice exercice = new Exercice();
        exercice.setName(request.name().trim());
        exercice.setStartDate(request.startDate());
        exercice.setEndDate(request.endDate());
        exercice.setStatus(ExerciceStatus.OPEN);

        Exercice saved = exerciceRepository.save(exercice);
        return toResponse(saved);
    }

    @Transactional
    public ExerciceResponse updateStatus(Long id, ExerciceStatus status) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Exercice exercice = exerciceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Exercice not found"));

        exercice.setStatus(status);
        exercice.setUpdatedAt(java.time.Instant.now());
        return toResponse(exerciceRepository.save(exercice));
    }

    private ExerciceResponse toResponse(Exercice entity) {
        return new ExerciceResponse(
                entity.getId(),
                entity.getName(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getStatus(),
                entity.getTenantId()
        );
    }
}
