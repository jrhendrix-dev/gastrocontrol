// src/main/java/com/gastrocontrol/gastrocontrol/service/table/GetDiningTableUseCase.java
package com.gastrocontrol.gastrocontrol.service.table;

import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.dto.staff.DiningTableResponse;
import com.gastrocontrol.gastrocontrol.repository.DiningTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetDiningTableUseCase {

    private final DiningTableRepository diningTableRepository;

    public GetDiningTableUseCase(DiningTableRepository diningTableRepository) {
        this.diningTableRepository = diningTableRepository;
    }

    @Transactional(readOnly = true)
    public DiningTableResponse handle(Long id) {
        var t = diningTableRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Dining table not found: " + id));

        DiningTableResponse r = new DiningTableResponse();
        r.setId(t.getId());
        r.setLabel(t.getLabel());
        return r;
    }
}
