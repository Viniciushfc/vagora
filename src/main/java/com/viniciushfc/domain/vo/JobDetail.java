package com.viniciushfc.domain.vo;

public record JobDetail(String title,
                        String description,
                        CompanyDetails companyDetails) {
    @Override
    public String toString() {
        return "JobDetail{" +
                "title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", companyDetails.name=" + companyDetails.name() +
                ", companyDetails.description=" + companyDetails.descrition() +
                '}';
    }
}
