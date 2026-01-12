package com.gastrocontrol.gastrocontrol.dto.staff;

import com.gastrocontrol.gastrocontrol.domain.enums.PaymentProvider;
import com.gastrocontrol.gastrocontrol.domain.enums.PaymentStatus;

public class ResumeCheckoutResponse {

    private Long orderId;
    private PaymentProvider provider;
    private PaymentStatus status;

    private String checkoutSessionId;
    private String checkoutUrl; // may be null if already paid
    private String paymentIntentId; // may be null

    private boolean alreadyPaid;

    public static ResumeCheckoutResponse paid(Long orderId, PaymentProvider provider, PaymentStatus status,
                                              String checkoutSessionId, String paymentIntentId) {
        ResumeCheckoutResponse r = new ResumeCheckoutResponse();
        r.orderId = orderId;
        r.provider = provider;
        r.status = status;
        r.checkoutSessionId = checkoutSessionId;
        r.paymentIntentId = paymentIntentId;
        r.checkoutUrl = null;
        r.alreadyPaid = true;
        return r;
    }

    public static ResumeCheckoutResponse started(Long orderId, PaymentProvider provider, PaymentStatus status,
                                                 String checkoutSessionId, String checkoutUrl, String paymentIntentId) {
        ResumeCheckoutResponse r = new ResumeCheckoutResponse();
        r.orderId = orderId;
        r.provider = provider;
        r.status = status;
        r.checkoutSessionId = checkoutSessionId;
        r.checkoutUrl = checkoutUrl;
        r.paymentIntentId = paymentIntentId;
        r.alreadyPaid = false;
        return r;
    }

    public Long getOrderId() { return orderId; }
    public PaymentProvider getProvider() { return provider; }
    public PaymentStatus getStatus() { return status; }
    public String getCheckoutSessionId() { return checkoutSessionId; }
    public String getCheckoutUrl() { return checkoutUrl; }
    public String getPaymentIntentId() { return paymentIntentId; }
    public boolean isAlreadyPaid() { return alreadyPaid; }
}
