package com.medisphere.medispherebackend.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FhirConfig {

    @Value("${medisphere.fhir.base-url:https://r4.quality.hl7.org/fhir}")
    private String fhirServerUrl;

    @Value("${medisphere.fhir.connect-timeout:5000}")
    private int connectTimeout;

    @Value("${medisphere.fhir.read-timeout:10000}")
    private int readTimeout;

    @Bean
    public FhirContext fhirContext() {
        FhirContext fhirContext = FhirContext.forR4();
        fhirContext.getRestfulClientFactory().setConnectTimeout(connectTimeout);
        fhirContext.getRestfulClientFactory().setSocketTimeout(readTimeout);
        return fhirContext;
    }

    @Bean
    public IGenericClient fhirClient(FhirContext fhirContext) {
        return fhirContext.newRestfulGenericClient(fhirServerUrl);
    }
}