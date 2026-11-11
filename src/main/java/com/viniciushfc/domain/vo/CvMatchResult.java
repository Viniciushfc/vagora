package com.viniciushfc.domain.vo;

import java.util.List;

public record CvMatchResult(boolean match,
                            int score,
                            List<String> motivos,
                            String curriculoAts) {
    @Override
    public String toString() {
        return "CvMatchResult{" +
                "match=" + match +
                ", score=" + score +
                ", motivos=" + motivos +
                ", curriculoAts='" + curriculoAts + '\'' +
                '}';
    }
}