package com.viniciushfc.infra.adapter.out.llm.gemini.contracts;

import com.viniciushfc.domain.vo.CvMatchResult;

public interface GeminiService {

    CvMatchResult genareteContentText(String prompt);
}
