package co.uptc.materias.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Operaciones sobre el bucket de AWS S3. Los archivos se envían directamente
 * desde memoria, sin escribirse en el disco del servidor.
 */
@Service
public class AlmacenamientoS3Service {

    // Vigencia de las URLs firmadas que se entregan al frontend
    private static final Duration URL_EXPIRA = Duration.ofHours(1);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;

    public AlmacenamientoS3Service(S3Client s3Client, S3Presigner s3Presigner,
                                   @Value("${aws.bucket-name}") String bucket) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = bucket;
    }

    public void subir(String clave, byte[] contenido, String tipoMime) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(clave)
                .contentType(tipoMime)
                .contentLength((long) contenido.length)
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(contenido));
    }

    public void eliminar(String clave) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(clave).build());
    }

    /**
     * El bucket es privado: el frontend accede al archivo mediante una URL firmada temporal.
     * La firma incluye Content-Disposition "attachment" para que el navegador descargue el
     * archivo con su nombre original en lugar de abrirlo (el atributo download de HTML no
     * funciona con URLs de otro dominio).
     */
    public String urlDescarga(String clave, String nombreArchivo) {
        GetObjectRequest getObject = GetObjectRequest.builder()
                .bucket(bucket)
                .key(clave)
                .responseContentDisposition(contentDisposition(nombreArchivo))
                .build();
        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                .signatureDuration(URL_EXPIRA)
                .getObjectRequest(getObject)
                .build();
        return s3Presigner.presignGetObject(request).url().toString();
    }

    // RFC 6266: filename ASCII de respaldo + filename* en UTF-8 para tildes y ñ
    static String contentDisposition(String nombreArchivo) {
        String ascii = nombreArchivo.replaceAll("[^\\x20-\\x7E]|[\"\\\\]", "_");
        String utf8 = URLEncoder.encode(nombreArchivo, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + utf8;
    }
}
