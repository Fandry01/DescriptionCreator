package com.descriptioncreator.backend.usage;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usage")
public class UsageController {
    private final UsageService usage;
    public UsageController(UsageService usage) { this.usage = usage; }
    @GetMapping public UsageService.UsageStatus status(Authentication authentication) {
        return usage.status(authentication.getName());
    }
}
