package com.gissoftware.quiz_survey.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

  @Bean
  public Cloudinary cloudinary() {
    return new Cloudinary(
        ObjectUtils.asMap(
            "cloud_name", "dxv3dts8k",
            "api_key", "422744849866437",
            "api_secret", "G3im-z914VaIXMdpuIIIw8BK3vM"));
  }
}
