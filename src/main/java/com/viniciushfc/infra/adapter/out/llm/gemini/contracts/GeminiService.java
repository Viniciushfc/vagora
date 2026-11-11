package com.viniciushfc.infra.adapter.out.llm.gemini.contracts;

import com.viniciushfc.domain.vo.CvMatchResult;

import java.util.List;

public interface GeminiService {

    List<CvMatchResult> genareteContentText(String prompt);
}
