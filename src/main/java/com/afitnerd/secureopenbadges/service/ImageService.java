package com.afitnerd.secureopenbadges.service;

import java.io.IOException;

public interface ImageService {

    byte[] getImage(String name) throws IOException;
    byte[] getImage(
        String name, String width, String vPos,
        String fontColor, String fontSize, String fontFamily, String fontAttr
    ) throws IOException;
}
