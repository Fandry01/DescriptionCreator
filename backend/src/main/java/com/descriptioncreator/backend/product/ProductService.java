package com.descriptioncreator.backend.product;

import com.descriptioncreator.backend.shopify.ShopifyClient;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ProductService {

    private final ShopifyClient shopifyClient;

    public ProductService(ShopifyClient shopifyClient) {
        this.shopifyClient = shopifyClient;
    }

    public List<ProductResponse> getProducts(boolean missingDescription) {
        return shopifyClient.fetchProducts().stream()
                .filter(product -> !missingDescription || isDescriptionMissing(product.descriptionHtml()))
                .map(ProductResponse::from)
                .toList();
    }

    public ProductMetafieldsResponse getProductMetafields(String handle) {
        return shopifyClient.fetchProductMetafieldsByHandle(handle)
                .map(ProductMetafieldsResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND,
                        "Shopify product not found for handle: " + handle
                ));
    }

    private boolean isDescriptionMissing(String descriptionHtml) {
        if (descriptionHtml == null || descriptionHtml.isBlank()) {
            return true;
        }

        String textContent = descriptionHtml
                .replaceAll("(?is)<!--.*?-->", "")
                .replaceAll("(?i)<br\\s*/?>", "")
                .replaceAll("<[^>]*>", "")
                .replace("&nbsp;", " ")
                .replace("&#160;", " ")
                .trim();

        return textContent.isEmpty();
    }
}
