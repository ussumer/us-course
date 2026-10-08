package com.us.media.config;

import com.us.media.enums.Platform;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "us.platform")
public class PlatformProperties {
    private Platform file;
    private Platform media;
}
