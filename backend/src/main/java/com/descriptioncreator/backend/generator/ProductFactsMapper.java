package com.descriptioncreator.backend.generator;

import com.descriptioncreator.backend.shopify.ShopifyProductMetafieldsDto;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ProductFactsMapper {

    private static final String CUSTOM_NAMESPACE = "custom";

    private final GradeMapper gradeMapper;

    public ProductFactsMapper(GradeMapper gradeMapper) {
        this.gradeMapper = gradeMapper;
    }

    public ProductFacts map(ShopifyProductMetafieldsDto product) {
        Map<String, String> customMetafields = customMetafields(product);
        String grade = value(customMetafields, "grade");

        return new ProductFacts(
                trim(product.vendor()),
                value(customMetafields, "model"),
                value(customMetafields, "color"),
                grade,
                gradeMapper.map(grade),
                value(customMetafields, "material"),
                value(customMetafields, "hardware"),
                value(customMetafields, "includes"),
                value(customMetafields, "size"),
                value(customMetafields, "dimensions"),
                value(customMetafields, "chain_length"),
                value(customMetafields, "strap_length"),
                value(customMetafields, "year"),
                value(customMetafields, "signs_of_wear")
        );
    }

    private Map<String, String> customMetafields(ShopifyProductMetafieldsDto product) {
        Map<String, String> values = new HashMap<>();
        if (product.metafields() == null) {
            return values;
        }

        for (ShopifyProductMetafieldsDto.Metafield metafield : product.metafields()) {
            if (metafield != null && CUSTOM_NAMESPACE.equals(metafield.namespace())) {
                values.put(metafield.key(), trim(metafield.value()));
            }
        }
        return values;
    }

    private String value(Map<String, String> metafields, String key) {
        return metafields.getOrDefault(key, "");
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
