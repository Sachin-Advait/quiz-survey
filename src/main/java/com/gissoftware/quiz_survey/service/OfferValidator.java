package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.model.OfferModel;

public class OfferValidator {

    public static void validate(OfferModel offer) {

        if (offer.getType() == null) {
            throw new IllegalArgumentException("Offer type is required");
        }

        switch (offer.getType()) {

            case IMAGE_ONLY -> {
                if (offer.getImageUrl() == null || offer.getImageUrl().isBlank()) {
                    throw new IllegalArgumentException(
                            "IMAGE_ONLY offer requires imageUrl");
                }
            }

            case TEXT_ONLY -> {
                if (isBlank(offer.getTitle()) || isBlank(offer.getDescription())) {
                    throw new IllegalArgumentException(
                            "TEXT_ONLY offer requires title and description");
                }
            }

            case IMAGE_WITH_TEXT -> {
                if (isBlank(offer.getTitle())
                        || isBlank(offer.getDescription())
                        || isBlank(offer.getImageUrl())) {
                    throw new IllegalArgumentException(
                            "IMAGE_WITH_TEXT offer requires title, description and imageUrl");
                }
            }
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
