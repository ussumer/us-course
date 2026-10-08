package com.us.message.thirdparty.ali;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "us.sms.ali")
public class AliProperties {
    private String accessId;
    private String accessSecret;
}
