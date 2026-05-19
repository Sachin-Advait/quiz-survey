package com.gissoftware.quiz_survey.Utils;

public class DeviceDetectorUtil {

  private DeviceDetectorUtil() {}

  public static String detectPlatform(String userAgent) {

    if (userAgent == null) {
      return "UNKNOWN";
    }

    String ua = userAgent.toLowerCase();

    if (ua.contains("android")
        || ua.contains("iphone")
        || ua.contains("ipad")
        || ua.contains("mobile")) {
      return "MOBILE";
    }

    return "WEB";
  }

  public static String detectClient(String userAgent) {

    if (userAgent == null) {
      return "UNKNOWN";
    }

    String ua = userAgent.toLowerCase();

    // MOBILE
    if (ua.contains("android")) {
      return "ANDROID";
    }

    if (ua.contains("iphone") || ua.contains("ipad")) {
      return "IOS";
    }

    // WEB BROWSERS
    if (ua.contains("edg")) {
      return "EDGE";
    }

    if (ua.contains("chrome")) {
      return "CHROME";
    }

    if (ua.contains("firefox")) {
      return "FIREFOX";
    }

    if (ua.contains("safari")) {
      return "SAFARI";
    }

    return "UNKNOWN";
  }
}
