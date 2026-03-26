package sparta.paymentsystemserver.domain.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sparta.paymentsystemserver.domain.product.dto.UploadProductImageRequest;
import sparta.paymentsystemserver.domain.product.entity.Product;
import sparta.paymentsystemserver.domain.product.entity.ProductImage;
import sparta.paymentsystemserver.domain.product.exception.ProductException;
import sparta.paymentsystemserver.domain.product.repository.ProductImageRepository;
import sparta.paymentsystemserver.domain.product.repository.ProductRepository;
import sparta.paymentsystemserver.global.s3.S3Service;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static sparta.paymentsystemserver.global.exception.ErrorCode.PRODUCT_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class ProductImageService {
    private final ProductImageRepository productImageRepository;
    private final S3Service s3Service;
    private final ProductRepository productRepository;

    public void productImageUpload(UploadProductImageRequest uploadProductImageRequest) {
        Product product = productRepository.findByProductId(uploadProductImageRequest.productId()).orElseThrow(
                () -> new ProductException(PRODUCT_NOT_FOUND)
        );

        List<ProductImage> productImages = new ArrayList<>();

        uploadProductImageRequest.uploadProductImages().forEach(imageRequest -> {
            String fileKey = s3Service.uploadImage(imageRequest.file());
            ProductImage productImage = new ProductImage(
                    product,
                    fileKey,
                    imageRequest.order(),
                    imageRequest.isThumbnail()
            );

            productImages.add(productImage);
        });

        productImageRepository.saveAll(productImages);
    }

    public Map<String,Integer> getProductImage(Product product) {
        List<ProductImage> productImages =  productImageRepository.findAllByProduct(product);

        return productImages.stream()
                .collect(Collectors.toMap(
                        image -> s3Service.getImageUrl(image.getFileKey()).toString(),
                        ProductImage::getSortOrder,
                        (existing, duplicate) -> existing
                ));
    }

    public URL getProductThumbnail(Product product) {
        Optional<ProductImage> productImage = productImageRepository.findByProductAndThumbnail(product, true);
        return productImage.map(image -> s3Service.getImageUrl(image.getFileKey())).orElse(null);
    }
}
