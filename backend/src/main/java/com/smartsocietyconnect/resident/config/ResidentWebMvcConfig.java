package com.smartsocietyconnect.resident.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.smartsocietyconnect.resident.entity.FlatType;

/**
 * Spring MVC configuration for the resident module.
 *
 * <p>Registers custom type converters that Spring MVC uses when binding
 * {@code @PathVariable} and {@code @RequestParam} values to method parameters.
 *
 * <p><b>Why this is needed for {@link FlatType}:</b>
 * Spring MVC's default enum binding uses {@link Enum#valueOf(Class, String)},
 * which looks for an exact match of the Java constant name. {@link FlatType}
 * constants use underscore prefixes ({@code _2BHK}) to work around Java's
 * digit-start restriction, so {@code valueOf("2BHK")} throws
 * {@code MethodArgumentTypeMismatchException} at runtime.
 *
 * <p>The registered {@link FlatTypeStringConverter} uses
 * {@link FlatType#fromDisplayValue(String)} instead, correctly mapping
 * clean URL values ({@code "2BHK"}) to their enum constants ({@code _2BHK}).
 *
 * <p><b>Note:</b> The JPA {@link FlatType.FlatTypeConverter} is a
 * {@code jakarta.persistence.AttributeConverter} — it is Hibernate-specific
 * and has no effect on Spring MVC binding. These are two separate concerns
 * requiring two separate converters.
 *
 * @author Smart Society Connect Team
 * @version 1.0
 */
@Configuration
public class ResidentWebMvcConfig implements WebMvcConfigurer {

    /**
     * Registers custom converters for the resident module's enum types.
     *
     * @param registry the {@link FormatterRegistry} to add converters to
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new FlatTypeStringConverter());
    }

    // ==========================================================================
    // Inner Converter
    // ==========================================================================

    /**
     * Spring MVC {@link org.springframework.core.convert.converter.Converter}
     * that maps a clean URL string (e.g. {@code "2BHK"}) to the corresponding
     * {@link FlatType} enum constant (e.g. {@code _2BHK}).
     *
     * <p>Used by Spring MVC when binding {@code @PathVariable FlatType flatType}
     * and {@code @RequestParam FlatType flatType} parameters.
     */
    static class FlatTypeStringConverter
            implements org.springframework.core.convert.converter.Converter<String, FlatType> {

        /**
         * Converts a display-value string from a URL path or query parameter
         * to the corresponding {@link FlatType} constant.
         *
         * @param source the raw string from the URL (e.g. {@code "2BHK"})
         * @return the matching {@link FlatType} constant
         * @throws IllegalArgumentException if the value does not match any {@link FlatType}
         */
        @Override
        public FlatType convert(String source) {
            return FlatType.fromDisplayValue(source.trim().toUpperCase());
        }

    }

}