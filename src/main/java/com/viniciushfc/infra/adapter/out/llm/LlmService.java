package com.viniciushfc.infra.adapter.out.llm;

import com.viniciushfc.domain.vo.CvMatchResult;

import java.util.List;

public interface LlmService {

    List<CvMatchResult> analyze(String prompt, int jobs, String apiKey);

    String adaptCurriculum(String prompt, String apiKey);
}
