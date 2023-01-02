package com.afitnerd.secureopenbadges.service;

import java.io.IOException;

public interface ImageService {

    byte[] getImage(String name) throws IOException;
    byte[] getImage(
        String name, String githubUser, String width, String datePosition, String userPosition,
        String fontColor, String fontSize, String fontFamily, String fontAttr, boolean shouldShowDate
    ) throws IOException;
}
