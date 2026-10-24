package com.elmeftouhi.expense.paymentmethod.repository;

import com.elmeftouhi.expense.paymentmethod.entity.PaymentMethod;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    List<PaymentMethod> findByTenantIdOrderByIsDefaultDescNameAsc(Long tenantId);

    Optional<PaymentMethod> findByIdAndTenantId(Long id, Long tenantId);

    Optional<PaymentMethod> findByTenantIdAndNameIgnoreCase(Long tenantId, String name);

    Optional<PaymentMethod> findByTenantIdAndIsDefaultTrue(Long tenantId);
}

