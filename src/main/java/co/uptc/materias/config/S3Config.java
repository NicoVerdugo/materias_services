package co.uptc.materias.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * Configuración del cliente de AWS S3.
 * <p>
 * Credenciales, región y bucket se leen únicamente de variables de entorno
 * (ver application.yml). Se usan credenciales estáticas explícitas para que el
 * SDK no recurra a perfiles locales (~/.aws) ni a otras fuentes.
 */
@Configuration
public class S3Config {

    private final StaticCredentialsProvider credenciales;
    private final Region region;
    private final String endpoint;

    public S3Config(@Value("${aws.access-key-id}") String accessKeyId,
                    @Value("${aws.secret-access-key}") String secretAccessKey,
                    @Value("${aws.session-token}") String sessionToken,
                    @Value("${aws.region}") String region,
                    @Value("${aws.s3-endpoint}") String endpoint) {
        AwsCredentials awsCredentials = sessionToken.isBlank()
                ? AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                : AwsSessionCredentials.create(accessKeyId, secretAccessKey, sessionToken);
        this.credenciales = StaticCredentialsProvider.create(awsCredentials);
        this.region = Region.of(region);
        this.endpoint = endpoint;
    }

    @Bean
    public S3Client s3Client() {
        var builder = S3Client.builder()
                .region(region)
                .credentialsProvider(credenciales)
                .httpClientBuilder(UrlConnectionHttpClient.builder());
        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint)).forcePathStyle(true);
        }
        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        var builder = S3Presigner.builder()
                .region(region)
                .credentialsProvider(credenciales);
        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint))
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        }
        return builder.build();
    }
}
