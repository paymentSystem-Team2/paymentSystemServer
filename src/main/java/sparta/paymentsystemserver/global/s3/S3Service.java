package sparta.paymentsystemserver.global.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    // 여러 이미지 업로드
    public List<String> uploadImages(List<MultipartFile> files) {
        List<String> imageUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            String fileName = "products/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            try {
                amazonS3.putObject(bucket, fileName, file.getInputStream(), metadata);
            } catch (IOException e) {
                throw new RuntimeException("S3 업로드 실패", e);
            }

            imageUrls.add(amazonS3.getUrl(bucket, fileName).toString());
        }

        return imageUrls;
    }

    // 이미지 1개 삭제
    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        String key = extractKeyFromUrl(imageUrl);
        amazonS3.deleteObject(bucket, key);
    }

    // URL -> S3 key 추출
    private String extractKeyFromUrl(String imageUrl) {
        int index = imageUrl.indexOf(".com/");
        if (index == -1) {
            throw new IllegalArgumentException("잘못된 S3 URL 형식입니다.");
        }
        return imageUrl.substring(index + 5);
    }
}