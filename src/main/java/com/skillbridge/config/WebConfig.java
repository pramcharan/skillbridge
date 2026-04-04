package com.skillbridge.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // CSS — cache 1 year
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCacheControl(CacheControl
                        .maxAge(365, TimeUnit.DAYS)
                        .cachePublic()
                        .immutable());

        // JS — cache 1 year
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .setCacheControl(CacheControl
                        .maxAge(365, TimeUnit.DAYS)
                        .cachePublic()
                        .immutable());

        // Images / fonts / icons — cache 30 days
        registry.addResourceHandler("/images/**", "/fonts/**", "/*.svg", "/*.png", "/*.ico")
                .addResourceLocations(
                        "classpath:/static/images/",
                        "classpath:/static/fonts/",
                        "classpath:/static/"
                )
                .setCacheControl(CacheControl
                        .maxAge(30, TimeUnit.DAYS)
                        .cachePublic());

        // HTML pages — no cache (always fresh)
        registry.addResourceHandler("/*.html")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl
                        .noStore()
                        .mustRevalidate());
    }
}