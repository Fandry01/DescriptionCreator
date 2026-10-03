package com.descriptioncreator.backend.description;

import com.descriptioncreator.backend.generator.DescriptionGeneratorService;
import com.descriptioncreator.backend.generator.ProductFacts;
import com.descriptioncreator.backend.generator.ProductFactsMapper;
import com.descriptioncreator.backend.shopify.ShopifyClient;
import com.descriptioncreator.backend.shopify.ShopifyProductMetafieldsDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.List;

@Service
public class ProductDescriptionService {

    private final ShopifyClient shopifyClient;
    private final ProductFactsMapper productFactsMapper;
    private final DescriptionGeneratorService descriptionGeneratorService;
    private final DescriptionVersionRepository descriptionVersionRepository;

    public ProductDescriptionService(
            ShopifyClient shopifyClient,
            ProductFactsMapper productFactsMapper,
            DescriptionGeneratorService descriptionGeneratorService,
            DescriptionVersionRepository descriptionVersionRepository
    ) {
        this.shopifyClient = shopifyClient;
        this.productFactsMapper = productFactsMapper;
        this.descriptionGeneratorService = descriptionGeneratorService;
        this.descriptionVersionRepository = descriptionVersionRepository;
    }

    public ProductDescriptionDraftResponse generateDraft(String handle) {
        ShopifyProductMetafieldsDto product = fetchProduct(handle);
        ProductFacts facts = productFactsMapper.map(product);
        String generatedDescription = descriptionGeneratorService.generate(product.title(), facts);

        return new ProductDescriptionDraftResponse(
                product.id(),
                product.title(),
                product.handle(),
                product.descriptionHtml(),
                generatedDescription,
                facts
        );
    }

    public PublishDescriptionResponse publishDescription(
            String handle,
            PublishDescriptionRequest request
    ) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Publish request is required");
        }

        ShopifyProductMetafieldsDto product = fetchProduct(handle);
        if (!Objects.equals(
                product.descriptionHtml(),
                request.expectedExistingDescriptionHtml()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The Shopify description changed after the draft was generated"
            );
        }

        String publishedDescriptionHtml = toSafeDescriptionHtml(request.description());
        shopifyClient.updateProductDescription(product.id(), publishedDescriptionHtml);
        saveHistory(DescriptionVersionEntity.publish(
                product.id(),
                product.handle(),
                product.descriptionHtml(),
                publishedDescriptionHtml
        ));

        return new PublishDescriptionResponse(
                product.id(),
                product.handle(),
                publishedDescriptionHtml
        );
    }

    public List<DescriptionVersionResponse> getDescriptionHistory(String handle) {
        return descriptionVersionRepository.findAllByHandleOrderByCreatedAtDescIdDesc(handle)
                .stream()
                .map(DescriptionVersionResponse::from)
                .toList();
    }

    public RestoreDescriptionResponse restoreDescription(
            String handle,
            Long versionId,
            RestoreDescriptionRequest request
    ) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Restore request is required");
        }

        DescriptionVersionEntity sourceVersion = descriptionVersionRepository
                .findByIdAndHandle(versionId, handle)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Description version not found for this product"
                ));

        ShopifyProductMetafieldsDto product = fetchProduct(handle);
        if (!Objects.equals(product.descriptionHtml(), request.expectedExistingDescriptionHtml())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The Shopify description changed after this page was loaded"
            );
        }

        String restoredDescriptionHtml = sourceVersion.getPublishedDescriptionHtml();
        shopifyClient.updateProductDescription(product.id(), restoredDescriptionHtml);
        DescriptionVersionEntity restoredVersion = saveHistory(DescriptionVersionEntity.restore(
                product.id(),
                product.handle(),
                product.descriptionHtml(),
                restoredDescriptionHtml,
                sourceVersion.getId()
        ));

        return new RestoreDescriptionResponse(
                product.id(),
                product.handle(),
                restoredDescriptionHtml,
                sourceVersion.getId(),
                restoredVersion.getId()
        );
    }

    private DescriptionVersionEntity saveHistory(DescriptionVersionEntity version) {
        try {
            return descriptionVersionRepository.saveAndFlush(version);
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Shopify description was updated, but version history could not be saved",
                    exception
            );
        }
    }

    private ShopifyProductMetafieldsDto fetchProduct(String handle) {
        return shopifyClient.fetchProductMetafieldsByHandle(handle)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Shopify product not found"
                ));
    }

    private String toSafeDescriptionHtml(String description) {
        if (description == null || description.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Approved description must not be blank"
            );
        }

        String normalizedDescription = description.trim().replaceAll("\\s+", " ");
        return "<p>" + HtmlUtils.htmlEscape(normalizedDescription) + "</p>";
    }
}
