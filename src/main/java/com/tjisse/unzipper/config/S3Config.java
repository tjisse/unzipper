package com.tjisse.unzipper.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class S3Config {

    @Bean
    public AmazonS3 s3client(
            @Value("${r2.endpoint}") String r2ServiceEndpoint,
            @Value("${r2.accountId}") String accountIdValue,
            @Value("${r2.accessKey}") String accessKeyValue,
            @Value("${r2.secretKey}") String secretKeyValue) {
        String accountR2Url = String.format(r2ServiceEndpoint, accountIdValue);
        AWSCredentials credentials = new BasicAWSCredentials(accessKeyValue, secretKeyValue);

        EndpointConfiguration endpointConfiguration = new EndpointConfiguration(accountR2Url, null);
        AmazonS3 s3client = AmazonS3ClientBuilder
                .standard()
                .withEndpointConfiguration(endpointConfiguration)
                .withPathStyleAccessEnabled(true)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .build();

        return s3client;
    }
}
