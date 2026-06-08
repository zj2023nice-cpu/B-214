package com.warehouse.controller;

import com.warehouse.common.ApiResponse;
import com.warehouse.config.WebMvcConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FileUploadController {

    private final WebMvcConfig webMvcConfig;

    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024;
    private static final int MAX_FILE_COUNT = 5;
    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png");
    private static final List<String> ALLOWED_EXTENSIONS = List.of(".jpg", ".jpeg", ".png");

    @PostMapping("/images")
    public ApiResponse<List<String>> uploadImages(@RequestParam("files") List<MultipartFile> files) {
        if (files.size() > MAX_FILE_COUNT) {
            return ApiResponse.error(400, "最多上传 " + MAX_FILE_COUNT + " 张图片");
        }

        String uploadPath = webMvcConfig.getUploadPath();
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                return ApiResponse.error(400, "图片 " + file.getOriginalFilename() + " 超过2MB大小限制");
            }

            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
                return ApiResponse.error(400, "图片 " + file.getOriginalFilename() + " 格式不支持，仅允许 jpg/png");
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                return ApiResponse.error(400, "文件名不能为空");
            }

            String extension = "";
            String lowerName = originalFilename.toLowerCase();
            for (String ext : ALLOWED_EXTENSIONS) {
                if (lowerName.endsWith(ext)) {
                    extension = ext;
                    break;
                }
            }
            if (extension.isEmpty()) {
                return ApiResponse.error(400, "图片 " + originalFilename + " 格式不支持，仅允许 jpg/png");
            }

            String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;
            Path filePath = Paths.get(uploadPath, newFilename);

            try {
                Files.copy(file.getInputStream(), filePath);
                urls.add("/uploads/" + newFilename);
            } catch (IOException e) {
                return ApiResponse.error(500, "图片上传失败: " + originalFilename);
            }
        }

        return ApiResponse.success(urls);
    }
}
