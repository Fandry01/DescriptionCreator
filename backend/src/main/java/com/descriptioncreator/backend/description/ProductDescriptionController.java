package com.descriptioncreator.backend.description;

import com.descriptioncreator.backend.usage.UsageService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductDescriptionController {

    private final ProductDescriptionService productDescriptionService;
    private final UsageService usageService;

    public ProductDescriptionController(ProductDescriptionService productDescriptionService,
            UsageService usageService) {
        this.productDescriptionService = productDescriptionService;
        this.usageService = usageService;
    }

    @PostMapping("/handle/{handle}/generate-description")
    public ProductDescriptionDraftResponse generateDescription(@PathVariable String handle,
            Authentication authentication) {
        usageService.assertGenerationAllowed(authentication.getName());
        ProductDescriptionDraftResponse response = productDescriptionService.generateDraft(handle);
        usageService.recordGeneration(authentication.getName(), response.productId(), response.handle());
        return response;
    }

    @PostMapping("/handle/{handle}/publish-description")
    public PublishDescriptionResponse publishDescription(
            @PathVariable String handle,
            @RequestBody PublishDescriptionRequest request
    ) {
        return productDescriptionService.publishDescription(handle, request);
    }

    @GetMapping("/handle/{handle}/description-history")
    public List<DescriptionVersionResponse> getDescriptionHistory(@PathVariable String handle) {
        return productDescriptionService.getDescriptionHistory(handle);
    }

    @PostMapping("/handle/{handle}/description-history/{versionId}/restore")
    public RestoreDescriptionResponse restoreDescription(
            @PathVariable String handle,
            @PathVariable Long versionId,
            @RequestBody RestoreDescriptionRequest request
    ) {
        return productDescriptionService.restoreDescription(handle, versionId, request);
    }
}
