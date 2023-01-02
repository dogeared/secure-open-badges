package com.afitnerd.secureopenbadges.model;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.AttributedString;
import java.util.Date;

import static com.afitnerd.secureopenbadges.config.Constants.IMAGES_PATH;

public class ImageBuilder {

    public enum VerticalPosition {
        TOP, BOTTOM
    }

    private BufferedImage bi;
    private Font font;
    private Color fontColor;
    private VerticalPosition datePosition;
    private VerticalPosition userPosition;
    private Integer width;
    private String user;
    private boolean showDate = true;

    private ImageBuilder(){}

    public static ImageBuilder start(String badgeName) throws IOException {
        InputStream is = ImageBuilder.class.getResourceAsStream(IMAGES_PATH + "/" + badgeName + ".png");
        if (is == null) { throw new IOException("Image Not Found!"); }
        ImageBuilder im = new ImageBuilder();
        im.bi = ImageIO.read(is);
        return im;
    }

    public ImageBuilder githubUser(String user) {
        this.user = user;
        return this;
    }

    public ImageBuilder font(String fontFamily, Integer fontAttribute, Integer fontSize) {
        if (fontFamily == null || Font.SANS_SERIF.toLowerCase().equals(fontFamily.toLowerCase())) {
            fontFamily = Font.SANS_SERIF;
        } else if (Font.SERIF.toLowerCase().equals(fontFamily.toLowerCase())) {
            fontFamily = Font.SERIF;
        } else if (Font.MONOSPACED.toLowerCase().equals(fontFamily.toLowerCase())) {
            fontFamily = Font.MONOSPACED;
        } else if (Font.DIALOG.toLowerCase().equals(fontFamily.toLowerCase())) {
            fontFamily = Font.DIALOG;
        } else if (Font.DIALOG_INPUT.toLowerCase().equals(fontFamily.toLowerCase())) {
            fontFamily = Font.DIALOG_INPUT;
        } else {
            fontFamily = Font.SANS_SERIF;
        }

        if (fontAttribute == null) {
            fontAttribute = Font.PLAIN;
        } else if (
            fontAttribute != Font.PLAIN && fontAttribute != Font.BOLD && fontAttribute != Font.ITALIC &&
            fontAttribute != (Font.BOLD|Font.ITALIC)
        ) {
            fontAttribute = Font.PLAIN;
        }

        if (fontSize == null || fontSize < 1) {
            fontSize = 80;
        }
        font = new Font(fontFamily, fontAttribute, fontSize);
        return this;
    }

    public ImageBuilder fontColor(Color fontColor) {
        if (fontColor == null) {
            fontColor = Color.BLACK;
        }
        this.fontColor = fontColor;
        return this;
    }

    public ImageBuilder datePosition(VerticalPosition datePosition) {
        if (datePosition == null) {
            datePosition = VerticalPosition.BOTTOM;
        }
        this.datePosition = datePosition;
        return this;
    }

    public ImageBuilder userPosition(VerticalPosition userPosition) {
        if (userPosition == null) {
            userPosition = VerticalPosition.TOP;
        }
        this.userPosition = userPosition;
        return this;
    }

    public ImageBuilder width(Integer width) {
        if (width != null) {
            this.width = width;
        }
        return this;
    }

    public ImageBuilder shouldShowDate(boolean shouldShowDate) {
        this.showDate = shouldShowDate;
        return this;
    }

    private BufferedImage resizeImage(BufferedImage image, Integer width) {
        if (width == null || width < 1 || width >= image.getWidth()) {
            return image;
        }
        double aspectRatio = (double) width / image.getWidth();
        int height = (int) Math.ceil(image.getHeight() * aspectRatio);

        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Image scaled = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        resized.getGraphics().drawImage(scaled, 0, 0, null);
        return resized;
    }

    private void fitText(String text, VerticalPosition vPos) {
        Graphics g = this.bi.getGraphics();
        FontMetrics metrics = g.getFontMetrics(this.font);

        AttributedString attributedText = new AttributedString(text);
        attributedText.addAttribute(TextAttribute.FONT, this.font);
        attributedText.addAttribute(TextAttribute.FOREGROUND, this.fontColor);

        GlyphVector vector = this.font.createGlyphVector(metrics.getFontRenderContext(), text);
        Shape outline = vector.getOutline(0, 0);
        double expectedWidth = outline.getBounds().getWidth();
        double expectedHeight = outline.getBounds().getHeight();
        boolean textFits = this.bi.getWidth() >= expectedWidth && this.bi.getHeight() >= expectedHeight;
        if (!textFits) {
            double widthBasedFontSize = ((this.font.getSize2D()*this.bi.getWidth())/expectedWidth)-10;
            double heightBasedFontSize = ((this.font.getSize2D()*this.bi.getHeight())/expectedHeight)-10;

            double newFontSize = Math.min(widthBasedFontSize, heightBasedFontSize);
            Font newFont = this.font.deriveFont(this.font.getStyle(), (float)newFontSize);
            metrics = g.getFontMetrics(newFont);
            attributedText = new AttributedString(text);
            attributedText.addAttribute(TextAttribute.FONT, newFont);
            attributedText.addAttribute(TextAttribute.FOREGROUND, this.fontColor);
        }
        int positionX = (this.bi.getWidth() - metrics.stringWidth(text)) / 2;
        int positionY = (this.bi.getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
        if (vPos == VerticalPosition.TOP) {
            positionY = metrics.getAscent();
        } else if (vPos == VerticalPosition.BOTTOM) {
            positionY =  bi.getHeight() - metrics.getHeight() + metrics.getAscent();
        }

        g.drawString(attributedText.getIterator(), positionX, positionY);
    }

    public byte[] build() throws IOException {
        if (datePosition == userPosition) {
            userPosition = VerticalPosition.TOP;
            datePosition = VerticalPosition.BOTTOM;
        }

        if (showDate) {
            String date = new Date().toString();
            fitText(date, datePosition);
        }

        if (user != null) {
            fitText(user, userPosition);
        }

        this.bi = resizeImage(this.bi, this.width);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(this.bi , "png", baos);

        return baos.toByteArray();
    }

}
