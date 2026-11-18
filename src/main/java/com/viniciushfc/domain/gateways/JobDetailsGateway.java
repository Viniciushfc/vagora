package com.viniciushfc.domain.gateways;

import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.enums.AlertSource;
import com.viniciushfc.domain.vo.JobDetail;

import java.util.Set;
import java.util.function.Consumer;

public interface JobDetailsGateway {

    void extractJobsDetails(AlertSource source, Set<String> jobLinks, LinkedAccount linkedinAccount, boolean showBrowser, Consumer<JobDetail> onJobRead);

    byte[] lastFailureScreenshot();

    void openLoginWindow(LinkedAccount linkedinAccount, Runnable onSessionSaved);

}
