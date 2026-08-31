package com.elmeftouhi.expense.exercice;

import com.elmeftouhi.expense.exercice.dto.ExerciceCreateRequest;
import com.elmeftouhi.expense.exercice.dto.ExerciceResponse;
import com.elmeftouhi.expense.exercice.dto.ExerciceStatusUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/exercices")
@RequiredArgsConstructor
public class ExerciceController {

    private final ExerciceService exerciceService;

    @GetMapping
    public List<ExerciceResponse> findAll() {
        return exerciceService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExerciceResponse create(@Valid @RequestBody ExerciceCreateRequest request) {
        return exerciceService.create(request);
    }

    @PutMapping("/{id}/status")
    public ExerciceResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ExerciceStatusUpdateRequest request
    ) {
        return exerciceService.updateStatus(id, request.status());
    }
}
