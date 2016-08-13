package ch.muriz.gaface;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Created by Muriz on 13.08.16.
 */

public class Fitness {

    Phenotype phenotypeObject = new Phenotype();

    /*
     * Determines the fitness of the individual by creating and matching
     * the individual's phenotype with the source phenotype (a predefined image)
     */
    public double calculateFitness(List<Integer> DNA) throws IOException{
        Phenotype phenotypeObject = new Phenotype();
        ImageUtils imageUtils = new ImageUtils();
        BufferedImage phenotype = phenotypeObject.createPhenotype(DNA);
        double similarity = calculateImageSimilarity(phenotype, imageUtils.init("source"));
        return ((similarity - similarityMatch("min")) / (similarityMatch("max") - similarityMatch("min")))*100;
    }

    /*
     * Calculates the difference between two images
     * Uses root mean squared analysis
     * If specified, mask is used for the histogram
     * TODO Take the parameter mask and calculate the histogram with only the mask
     */
    private double simpleImageSimilarity(BufferedImage image, BufferedImage match, BufferedImage mask) {
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage difference = Utils.imageAbsoluteDifference(image, match);
        // Create histogram with a mask if image is RGBA
        //BufferedImage maskWithAlpha = ImageIO.read(new File("mask.png"));
        int[] histogram = Utils.imageHistogram(difference);
        long sumSquaredValues = 0;
        long square = 0;
        for(long n : histogram) {
            square = n*n;
            sumSquaredValues += square;
        }
        return 1/ (Math.sqrt(sumSquaredValues / (w*h)));
    }
    /*
     * If no mask is available
     */
    private double simpleImageSimilarity(BufferedImage image, BufferedImage match) {
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage difference = Utils.imageAbsoluteDifference(image, match);
        // Create histogram with a mask if image is RGBA
        int[] histogram = Utils.imageHistogram(difference);
        long sumSquaredValues = 0;
        long square = 0;
        for(long n : histogram) {
            square = n*n;
            sumSquaredValues += square;
        }
        return 1/ (Math.sqrt(sumSquaredValues / (w*h)));
    }

    /*
     * Uses simpleImageSimilarity() and any masks
     * importantMask = Single Layer Mask, only the alpha channel.
     * importantMaskRGB = mask.png drew over top of a white, blank image
     */
    private double calculateImageSimilarity(BufferedImage image, BufferedImage match) throws IOException {
        double similarity = simpleImageSimilarity(image, match);
        ImageUtils imageUtils = new ImageUtils();
        BufferedImage importantMask = imageUtils.createImportantMask();
        BufferedImage mask = ImageIO.read(new File("mask.png"));
        BufferedImage maskedImage = new BufferedImage(importantMask.getWidth(),
                importantMask.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D maskedGraphics = maskedImage.createGraphics();
        maskedGraphics.setBackground(Color.WHITE);
        maskedGraphics.clearRect(0, 0, maskedImage.getWidth(), maskedImage.getHeight());
        maskedGraphics.dispose();
        BufferedImage importantMaskRGB = maskedImage;
        maskedImage = Utils.applyGrayscaleMaskToAlpha(image, importantMask);
        Graphics2D importantMaskRGBGraphics = importantMaskRGB.createGraphics();
        importantMaskRGBGraphics.drawImage(mask, 0, 0, null);
        importantMaskRGBGraphics.dispose();
        similarity += simpleImageSimilarity(maskedImage, importantMaskRGB, importantMask) * Settings.IMPORTANT_AREAS_SCALE;

        return similarity;
    }

    private double similarityMatch(String caseString) throws IOException{
        ImageUtils imageUtils = new ImageUtils();
        BoxBlurFilter blurFilter = new BoxBlurFilter();
        blurFilter.setHRadius(4); blurFilter.setRadius(4); blurFilter.setIterations(1);
        BufferedImage source = imageUtils.init("source");
        if(caseString.equals("min")) {
            return calculateImageSimilarity(imageUtils.createNegativeImage(), source);
        }
        if(caseString.equals("max")) {
            return calculateImageSimilarity(blurFilter.filter(source, null), source);
        }
        System.out.println("Failed to calculate similarity. Please type min or max");
        return 0.0;
    }

}
