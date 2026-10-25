package com.elmeftouhi.expense.expense;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.elmeftouhi.expense.expense.controller.SubscriptionPaymentController;
import com.elmeftouhi.expense.expense.dto.SubscriptionPaymentGenerationResponse;
import com.elmeftouhi.expense.expense.dto.SubscriptionPaymentBackfillRequest;
import com.elmeftouhi.expense.expense.dto.SubscriptionPaymentPageResponse;
import com.elmeftouhi.expense.expense.dto.SubscriptionPaymentResponse;
import com.elmeftouhi.expense.expense.service.SubscriptionPaymentService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class SubscriptionPaymentControllerTests {

    @Mock
    private SubscriptionPaymentService subscriptionPaymentService;

    @InjectMocks
    private SubscriptionPaymentController subscriptionPaymentController;

    @Test
    void getSubscriptionPayments_returnsPageResponseFromServicePage() {
        Long subscriptionId = 42L;
        Pageable pageable = PageRequest.of(0, 20);

        SubscriptionPaymentResponse payment = samplePayment(100L, subscriptionId, 200L);
        Page<SubscriptionPaymentResponse> page = new PageImpl<>(List.of(payment), pageable, 1);

        when(subscriptionPaymentService.getPaymentHistoryBySubscription(subscriptionId, pageable)).thenReturn(page);

        SubscriptionPaymentPageResponse response = subscriptionPaymentController.getSubscriptionPayments(subscriptionId, pageable);

        assertEquals(1, response.content().size());
        assertEquals(100L, response.content().get(0).id());
        assertEquals(0, response.page());
        assertEquals(20, response.size());
        assertEquals(1L, response.totalElements());
        assertEquals(1, response.totalPages());
        verify(subscriptionPaymentService).getPaymentHistoryBySubscription(subscriptionId, pageable);
    }

    @Test
    void getSubscriptionPaymentById_delegatesToService() {
        Long paymentId = 100L;
        SubscriptionPaymentResponse payment = samplePayment(paymentId, 42L, 200L);

        when(subscriptionPaymentService.getPaymentById(paymentId)).thenReturn(payment);

        SubscriptionPaymentResponse response = subscriptionPaymentController.getSubscriptionPaymentById(paymentId);

        assertEquals(paymentId, response.id());
        assertEquals(42L, response.subscriptionId());
        assertEquals(200L, response.expenseId());
        verify(subscriptionPaymentService).getPaymentById(paymentId);
    }

    @Test
    void generateNextSubscriptionPayment_delegatesToService() {
        Long subscriptionId = 42L;
        SubscriptionPaymentGenerationResponse generationResponse = new SubscriptionPaymentGenerationResponse(
                true,
                false,
                "Subscription payment generated successfully",
                501L,
                601L,
                LocalDate.of(2026, 11, 1)
        );

        when(subscriptionPaymentService.generateNextPayment(subscriptionId)).thenReturn(generationResponse);

        SubscriptionPaymentGenerationResponse response =
                subscriptionPaymentController.generateNextSubscriptionPayment(subscriptionId);

        assertEquals(true, response.generated());
        assertEquals(false, response.alreadyExists());
        assertEquals(501L, response.paymentId());
        verify(subscriptionPaymentService).generateNextPayment(subscriptionId);
    }

    @Test
    void backfillSubscriptionPayment_delegatesToService() {
        Long subscriptionId = 42L;
        SubscriptionPaymentBackfillRequest request = new SubscriptionPaymentBackfillRequest(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 1));
        SubscriptionPaymentGenerationResponse serviceResponse = new SubscriptionPaymentGenerationResponse(
                true,
                false,
                "Subscription backfill payment created successfully",
                510L,
                610L,
                LocalDate.of(2026, 9, 1)
        );

        when(subscriptionPaymentService.backfillPayment(subscriptionId, request)).thenReturn(serviceResponse);

        SubscriptionPaymentGenerationResponse response =
                subscriptionPaymentController.backfillSubscriptionPayment(subscriptionId, request);

        assertEquals(true, response.generated());
        assertEquals(510L, response.paymentId());
        verify(subscriptionPaymentService).backfillPayment(subscriptionId, request);
    }

    private SubscriptionPaymentResponse samplePayment(Long id, Long subscriptionId, Long expenseId) {
        return new SubscriptionPaymentResponse(
                id,
                subscriptionId,
                LocalDate.of(2026, 11, 1),
                expenseId,
                BigDecimal.valueOf(19.99),
                "USD",
                ExpenseStatus.PENDING,
                LocalDate.of(2026, 11, 1),
                null,
                Instant.parse("2026-10-21T00:00:00Z")
        );
    }
}

