package com.gastrocontrol.gastrocontrol.application.service.payment;

import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.dto.staff.OrderPaymentResponse;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetOrderPaymentService {

    private final PaymentRepository paymentRepository;

    public GetOrderPaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public OrderPaymentResponse handle(Long orderId) {
        var p = paymentRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new NotFoundException("Payment not found for order: " + orderId));

        return new OrderPaymentResponse(
                orderId,
                p.getProvider(),
                p.getStatus(),
                p.getAmountCents(),
                p.getCurrency(),
                p.getCheckoutSessionId(),
                p.getPaymentIntentId()
        );
    }
}
