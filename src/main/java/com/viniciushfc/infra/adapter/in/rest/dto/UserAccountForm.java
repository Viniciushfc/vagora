package com.viniciushfc.infra.adapter.in.rest.dto;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

public class UserAccountForm {

    @RestForm
    public String name;

    @RestForm
    public String username;

    @RestForm
    public String password;

    @RestForm
    public FileUpload curriculum;
}
