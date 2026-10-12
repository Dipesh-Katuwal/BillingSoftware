package in.dipesh.spring_backend.service.Impl;

import in.dipesh.spring_backend.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;


import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {
    private final S3Client s3client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    @Override
    public String uploadFile(MultipartFile file) {
        // Step 1: Figure out the file extension from the original name
        String originalFilename = file.getOriginalFilename();
        String extension="";
        if (originalFilename != null && originalFilename.contains(".")){
           extension=originalFilename.substring(originalFilename.indexOf("."));
        }
        String key = UUID.randomUUID().toString()+extension;
        try {
            // Step 2: Build a request describing what to upload where
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType()).build();
            // Step 3: Actually send the bytes to S3
            s3client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(),file.getSize()));
            // Step 4: Return the URL clients will use to see the image
            return String.format("https://%s.s3.%s.amazonaws.com/%s",
                    bucketName,region, key);

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to s3 :"+ e);
        }
    }

    @Override
    public boolean deleteFile(String imgUrl) {
        try {
            String key = imgUrl.substring(imgUrl.lastIndexOf("/")+1);

            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket("dipesh-billingsoftwae-2026")
                    .key(key).build();

            s3client.deleteObject(deleteRequest);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
