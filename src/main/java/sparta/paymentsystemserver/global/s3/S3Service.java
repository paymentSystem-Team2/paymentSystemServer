package sparta.paymentsystemserver.global.s3;

import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Template s3Template;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    // 여러 이미지 업로드
    public List<String> uploadImages(List<MultipartFile> files) {
        List<String> imageUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            String fileKey = "products/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
            try {
                s3Template.upload(bucketName, fileKey, file.getInputStream());
            } catch (IOException e) {
                throw new RuntimeException("S3 업로드 실패", e);
            }

            imageUrls.add(s3Template.createSignedGetURL(bucketName, fileKey, Duration.ofDays(7)).toString());
        }
        return imageUrls;
    }

    public String uploadImage(MultipartFile file) {
            String fileKey = "products/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
            try {
                s3Template.upload("payment-front-server", fileKey, file.getInputStream());
            } catch (IOException e) {
                throw new RuntimeException("S3 업로드 실패", e);
            }
        return fileKey;
    }

    public URL getImageUrl(String key) {
        return s3Template.createSignedGetURL(bucketName, key, Duration.ofDays(7));
    }

}