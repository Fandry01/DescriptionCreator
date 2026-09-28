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

@Service
public class ProductDescriptionService {

    private final ShopifyClient shopifyClient;
    private final ProductFactsMapper productFactsMapper;
    private final DescriptionGeneratorService descriptionGeneratorService;

    public ProductDescriptionService(
            ShopifyClient shopifyClient,
            ProductFactsMapper productFactsMapper,
            DescriptionGeneratorService descriptionGeneratorService
    ) {
        this.shopifyClient = shopifyClient;
        this.productFactsMapper = productFactsMapper;
        this.descriptionGeneratorService = descriptionGeneratorService;
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

        return new PublishDescriptionResponse(
                product.id(),
                product.handle(),
                publishedDescriptionHtml
        );
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
