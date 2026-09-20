package com.descriptioncreator.backend.shopify;

import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping("/api/shopify")
public class ShopifyAuthController {

    private final ShopifyOAuthService oauthService;

    public ShopifyAuthController(ShopifyOAuthService oauthService) {
        this.oauthService = oauthService;
    }

    @GetMapping("/auth")
    public RedirectView authorize() {
        return new RedirectView(oauthService.createAuthorizationUrl());
    }

    @GetMapping("/callback")
    public RedirectView callback(@RequestParam MultiValueMap<String, String> parameters) {
        oauthService.completeAuthorization(parameters);
        return new RedirectView("/api/shopify/status");
    }

    @GetMapping("/status")
    @ResponseBody
    public ShopifyTokenStore.Status status() {
        return oauthService.status();
    }
}
