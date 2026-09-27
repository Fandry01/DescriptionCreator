package com.descriptioncreator.backend.description;

import com.descriptioncreator.backend.generator.DescriptionGeneratorService;
import com.descriptioncreator.backend.generator.ProductFacts;
import com.descriptioncreator.backend.generator.ProductFactsMapper;
import com.descriptioncreator.backend.shopify.ShopifyClient;
import com.descriptioncreator.backend.shopify.ShopifyProductMetafieldsDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
        ShopifyProductMetafieldsDto product = shopifyClient
                .fetchProductMetafieldsByHandle(handle)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Shopify product not found"
                ));
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
}
