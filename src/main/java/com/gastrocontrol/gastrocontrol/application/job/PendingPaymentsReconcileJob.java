// src/main/java/com/gastrocontrol/gastrocontrol/application/job/PendingPaymentsReconcileJob.java
package com.gastrocontrol.gastrocontrol.application.job;

import com.gastrocontrol.gastrocontrol.application.service.payment.ReconcilePendingPaymentsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
@ConditionalOnProperty(
        prefix = "payments.reconcile",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class PendingPaymentsReconcileJob {

    private final ReconcilePendingPaymentsService service;

    public PendingPaymentsReconcileJob(ReconcilePendingPaymentsService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${payments.reconcile.fixed-delay-ms:60000}")
    public void run() {
        var r = service.reconcilePending(25);

        if (r.attempted() > 0) {
            log.info("Pending payment reconcile job: attempted={}, updated={}, ok={}, failed={}",
                    r.attempted(), r.updated(), r.alreadyOk(), r.failed());
        }
    }
}
