package com.viniciushfc.infra.adapter.out.llm;

import com.viniciushfc.domain.vo.CvMatchResult;

import java.util.List;

public interface LlmService {

    List<CvMatchResult> analyze(String prompt, int jobs, String apiKey, String model);

    String adaptCurriculum(String prompt, String apiKey, String model);

    String chat(String prompt, String apiKey, String model);

    String defaultModel(String apiKey);

    List<String> models(String apiKey);
}
