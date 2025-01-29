package org.acme;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;

import io.quarkus.runtime.annotations.RegisterForReflection;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Path("/upload")
public class UploadResource {

    @Inject
    S3Client s3Client; // Injetado automaticamente pelo Quarkus

    // Configuração do bucket (pode ser injetada via @ConfigProperty)
    private static final String BUCKET_NAME = "meu-bucket-uploads";

    // Classe interna para representar o formulário de upload
    @RegisterForReflection
    public static class UploadForm {
        @RestForm("file") // Define o campo do arquivo no formulário
        @PartType(MediaType.APPLICATION_OCTET_STREAM) // Define o tipo de conteúdo
        public byte[] file; // Arquivo em bytes

        @RestForm("fileName") // Define o nome do arquivo
        @PartType(MediaType.TEXT_PLAIN)
        public String fileName; // Nome original do arquivo
    }

    // Endpoint para upload de arquivos
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA) // Aceita multipart/form-data
    @Produces(MediaType.APPLICATION_JSON) // Retorna JSON
    public Response uploadFile(UploadForm form) {
        System.out.println("Tamanho do arquivo recebido: " + (form.file != null ? form.file.length : "null"));
        try {
            // Gera um nome único para o arquivo
            String uniqueFileName = generateUniqueFileName(form.fileName);

            // Cria a requisição para o S3
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(BUCKET_NAME) // Nome do bucket
                    .key(uniqueFileName) // Nome do arquivo no S3
                    .contentType(MediaType.APPLICATION_OCTET_STREAM) // Tipo de conteúdo
                    .build();

            // Faz o upload do arquivo para o S3
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(form.file));

            // Retorna uma mensagem de sucesso
            return Response.ok()
                    .entity("{\"message\":\"Upload realizado com sucesso!\", \"fileName\":\"" + uniqueFileName + "\"}")
                    .build();
        } catch (Exception e) {
            // Retorna uma mensagem de erro em caso de falha
            return Response.serverError()
                    .entity("{\"error\":\"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    // Método para gerar um nome único para o arquivo
    private String generateUniqueFileName(String originalName) {
        return UUID.randomUUID().toString() + "-" + originalName;
    }

}
