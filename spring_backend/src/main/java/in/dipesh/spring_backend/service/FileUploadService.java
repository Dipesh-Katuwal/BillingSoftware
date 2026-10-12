package in.dipesh.spring_backend.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {
    public String uploadFile(MultipartFile file);
    boolean deleteFile(String imgUrl);
}
