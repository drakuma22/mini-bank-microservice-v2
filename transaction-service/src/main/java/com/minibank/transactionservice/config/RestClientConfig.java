package com.minibank.transactionservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public RestClient accountRestClient(@Value("${account-service.url}") String accountServiceUrl,
                                        RestClient.Builder loadBalancedRestClientBuilder) {

        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings
                .defaults()
                .withConnectTimeout(Duration.ofSeconds(3))
                .withReadTimeout(Duration.ofSeconds(5));

        ClientHttpRequestFactory factory = ClientHttpRequestFactoryBuilder
                .detect()
                .build(settings);

        return loadBalancedRestClientBuilder
                .baseUrl(accountServiceUrl)
                .requestFactory(factory)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    //Nel caso in cui si volesse creare un altro client per comunicare con un altro servizio basta aggiungere un altro bean

    //    @Bean
    //    public RestClient customerRestClient(@Value("${customer-service.url}") String customerServiceUrl,
    //                                        RestClient.Builder loadBalancedRestClientBuilder) {
    //
    //        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings
    //                .defaults()
    //                .withConnectTimeout(Duration.ofSeconds(3))
    //                .withReadTimeout(Duration.ofSeconds(5));
    //
    //        ClientHttpRequestFactory factory = ClientHttpRequestFactoryBuilder
    //                .detect()
    //                .build(settings);
    //
    //        return loadBalancedRestClientBuilder
    //                .baseUrl(customerServiceUrl)
    //                .requestFactory(factory)
    //                .defaultHeader("Content-Type", "application/json")
    //                .build();
    //    }
}
