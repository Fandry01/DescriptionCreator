package com.descriptioncreator.backend.description;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductDescriptionController {

    private final ProductDescriptionService productDescriptionService;

    public ProductDescriptionController(ProductDescriptionService productDescriptionService) {
        this.productDescriptionService = productDescriptionService;
    }

    @PostMapping("/handle/{handle}/generate-description")
    public ProductDescriptionDraftResponse generateDescription(@PathVariable String handle) {
        return productDescriptionService.generateDraft(handle);
    }

    @PostMapping("/handle/{handle}/publish-description")
    public PublishDescriptionResponse publishDescription(
            @PathVariable String handle,
            @RequestBody PublishDescriptionRequest request
    ) {
        return productDescriptionService.publishDescription(handle, request);
    }
}
