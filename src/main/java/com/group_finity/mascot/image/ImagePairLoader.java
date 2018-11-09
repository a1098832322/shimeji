package com.group_finity.mascot.image;

import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.FileInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */

public class ImagePairLoader {
    /**
     */
    public static void load(final String name, final String rightName, final Point center) throws IOException {
        if (ImagePairs.contains(name + (rightName == null ? "" : rightName)))
            return;

        final BufferedImage leftImage = premultiply(ImageIO.read(new FileInputStream(name)));
        final BufferedImage rightImage;
        if (rightName == null)
            rightImage = flip(leftImage);
        else
            rightImage = premultiply(ImageIO.read(new FileInputStream(rightName)));

        ImagePair ip = new ImagePair(new MascotImage(leftImage, center),
                new MascotImage(rightImage, new Point(rightImage.getWidth() - center.x, center.y)));
        ImagePairs.load(name + (rightName == null ? "" : rightName), ip);
    }

    /**
     */
    private static BufferedImage flip(final BufferedImage src) {

        final BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(),
                src.getType() == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_ARGB : src.getType());

        for (int y = 0; y < src.getHeight(); ++y) {
            for (int x = 0; x < src.getWidth(); ++x) {
                copy.setRGB(copy.getWidth() - x - 1, y, src.getRGB(x, y));
            }
        }
        return copy;
    }

    private static BufferedImage premultiply(final BufferedImage source) {
        final BufferedImage returnImage = new BufferedImage(source.getWidth(), source.getHeight(),
                source.getType() == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_ARGB_PRE : source.getType());
        Color colour;
        float[] components;

        for (int y = 0; y < source.getHeight(); ++y) {
            for (int x = 0; x < source.getWidth(); ++x) {
                colour = new Color(source.getRGB(x, y), true);
                components = colour.getComponents(null);
                components[0] = components[3] * components[0];
                components[1] = components[3] * components[1];
                components[2] = components[3] * components[2];
                colour = new Color(components[0], components[1], components[2], components[3]);
                returnImage.setRGB(x, y, colour.getRGB());
            }
        }

        return returnImage;
    }
}
