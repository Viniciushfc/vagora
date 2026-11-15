package com.viniciushfc.domain.gateways;

import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.vo.JobDetail;

import java.util.List;
import java.util.Set;

public interface JobDetailsGateway {

    /**
     * @param showBrowser {@code true} abre o navegador na tela; {@code false} roda escondido
     */
    List<JobDetail> extractJobsDetails(Set<String> jobLinks, LinkedAccount linkedinAccount, boolean showBrowser);

    /** Captura da tela do navegador na última falha (PNG), ou null. */
    byte[] lastFailureScreenshot();

}
