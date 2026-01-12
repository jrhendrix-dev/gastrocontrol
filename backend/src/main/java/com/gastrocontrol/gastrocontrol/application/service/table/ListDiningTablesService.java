// src/main/java/com/gastrocontrol/gastrocontrol/service/table/ListDiningTablesUseCase.java
package com.gastrocontrol.gastrocontrol.application.service.table;

import com.gastrocontrol.gastrocontrol.dto.common.PagedResponse;
import com.gastrocontrol.gastrocontrol.dto.staff.DiningTableResponse;
import com.gastrocontrol.gastrocontrol.infrastructure.persistence.repository.DiningTableRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListDiningTablesService {

    private final DiningTableRepository diningTableRepository;

    public ListDiningTablesService(DiningTableRepository diningTableRepository) {
        this.diningTableRepository = diningTableRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<DiningTableResponse> handle(String q, Pageable pageable) {
        var page = diningTableRepository.findAll(TableSpecifications.labelContains(q), pageable)
                .map(t -> {
                    DiningTableResponse r = new DiningTableResponse();
                    r.setId(t.getId());
                    r.setLabel(t.getLabel());
                    return r;
                });

        return PagedResponse.from(page);
    }
}
