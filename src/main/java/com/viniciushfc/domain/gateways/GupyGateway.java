package com.viniciushfc.domain.gateways;

import com.viniciushfc.domain.entities.GupySearch;
import com.viniciushfc.domain.vo.GupyJob;

import java.time.LocalDate;
import java.util.List;

public interface GupyGateway {
    List<GupyJob> search(GupySearch search, LocalDate since);
}
