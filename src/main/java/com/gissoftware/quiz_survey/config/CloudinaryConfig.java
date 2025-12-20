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
            "cloud_name", "xxxx",
            "api_key", "xxxxxx",
            "api_secret", "xxxx"));
  }
}
