package org.example;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class DicomImage {
    public static String endian;
    public static int rows;
    public static int cols;
    public static int numOfImages;
    public static double[] pixelSpacing;
    public static double sliceThickness;
    public static double[] imagePosition;
    public static int bitsStored;
    public static int bitsAllocated;
    public static int rescaleSlope;
    public static int rescaleIntercept;
    public static int windowCenter;
    public static int windowWidth;
    public static int[][][] pixelData;
    public int index = 132;
    public static int x = 0;
    public static int y = 0;
    public static int z = 0;
    public static double stretchXZ;
    public static double stretchYZ;

    ArrayList<String> twoBytesLongVRs = new ArrayList<>(Arrays.asList("AE", "AS", "AT", "CS", "DA", "DS", "DT", "FL", "FD", "IS", "LO", "LT", "PN", "SH", "SL", "SS",
            "ST", "TM", "UI", "UL", "US"));

    ArrayList<String> importantTags = new ArrayList<>(Arrays.asList(
            "280010", //rows
            "280011", //cols
            "280030", //pixelSpacing
            "180050", //sliceThickness
            "200032", //imagePosition
            "280101", //bitsStored
            "280100",  //bitsAllocated
            "281053", //rescaleSlope
            "281052", //rescaleIntercept
            "281050", //windowCenter
            "281051"  //windowWidth
    ));

    Map<String, String> tagsNames = new HashMap<String, String>() {{
        put("280010", "rows"); //int
        put("280011", "cols"); //int
        put("280030", "pixelSpacing"); //double[]
        put("180050", "sliceThickness"); //double
        put("200032", "imagePosition"); //double[]
        put("280101", "bitsStored"); //int
        put("280100", "bitsAllocated"); //int
        put("281053", "rescaleSlope");
        put("281052", "rescaleIntercept");
        put("281050", "windowCenter");
        put("281051", "windowWidth");
    }};

    public DicomImage(String folderPath) throws IOException {
        String path = System.getProperty("user.dir") + folderPath;
        File dicomImagesFolder = new File(path);
        int dicomImages = dicomImagesFolder.listFiles().length;
        numOfImages = dicomImages;

        FileInputStream file = new FileInputStream(path + "\\IM1");
        byte[] byteAr = file.readAllBytes();

        Map<String, double[]> importantData = new HashMap<>();

        if (byteAr[132] == 2 & byteAr[133] == 0) {
            endian = "little";
        } else if (byteAr[132] == 0 & byteAr[133] == 2) {
            endian = "big";
        }

        while (index < byteAr.length) {
            long currentTag = readTag(byteAr, index);
            index += 4;

            // end tag
            if (Long.toHexString(currentTag).equals("ffffffffffe00010")) {
                index += 6;
                break;
            }

            String VR = new String(new char[]{(char) byteAr[index], (char) byteAr[index + 1]});
            index += 2;

            if (twoBytesLongVRs.contains(VR)) {
                int valueLength = byteAr[index + 1] << 8 | byteAr[index];
                index += 2;

                if (importantTags.contains(Long.toHexString(currentTag))) {
                    importantData.put(tagsNames.get(Long.toHexString(currentTag)), readValue(byteAr, index, valueLength, tagsNames.get(Long.toHexString(currentTag))));
                }
                index += valueLength;

            } else {
                index += 2;
                int valueLength = byteAr[index + 3] << 24 | byteAr[index + 2] << 16 | byteAr[index + 1] << 8 | byteAr[index];
                index += 4;
                index += valueLength;

                if (importantTags.contains(currentTag)) {
                    importantData.put(tagsNames.get(Long.toHexString(currentTag)), readValue(byteAr, index, valueLength, tagsNames.get(Long.toHexString(currentTag))));
                }
            }
        }

        rows = (int) importantData.get("rows")[0];
        cols = (int) importantData.get("cols")[0];
        pixelSpacing = importantData.get("pixelSpacing");
        sliceThickness = importantData.get("sliceThickness")[0];
        imagePosition = importantData.get("imagePosition");
        bitsStored = (int) importantData.get("bitsStored")[0];
        bitsAllocated = (int) importantData.get("bitsAllocated")[0];
        rescaleSlope = (int) importantData.get("rescaleSlope")[0];
        rescaleIntercept = (int) importantData.get("rescaleIntercept")[0];
        windowCenter = (int) importantData.get("windowCenter")[0];
        windowWidth = (int) importantData.get("windowWidth")[0];
        stretchYZ = (double) rows / numOfImages;
        stretchXZ = (double) cols / numOfImages;

        pixelData = new int[dicomImages][rows][cols];

        for (int i = 1; i < dicomImages - 1; i++) {
            pixelData[i - 1] = readPixelData(new File(path + "\\IM" + i));
        }
    }

    private int[][] readPixelData(File dicomImage) throws IOException {
        FileInputStream fileStream = new FileInputStream(dicomImage);
        byte[] imageBytes = fileStream.readAllBytes();
        int[] grayPixelData = new int[rows * cols];

        for (int i = 0; i < rows * cols - 1; i++) {
            grayPixelData[i] = rescaleSlope * readPixelValue(imageBytes, index + i * bitsAllocated / 8) + rescaleIntercept;
        }

        int[][] imageArray = new int[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                imageArray[i][j] = grayPixelData[i * cols + j];
            }
        }
        return imageArray;
    }

    private int readPixelValue(byte[] imageBytes, int i) {
        if (Objects.equals(endian, "little")) {
            return imageBytes[i + 1] << 8 | imageBytes[i] & 0xFF;
        } else {
            return imageBytes[i] << 8 | imageBytes[i + 1] & 0xFF;
        }
    }

    private int readTag(byte[] byteAr, int index) {
        if (Objects.equals(endian, "little")) {
            return byteAr[index + 1] << 24 | byteAr[index] << 16 | byteAr[index + 3] << 8 | byteAr[index + 2];
        } else {
            return byteAr[index] << 24 | byteAr[index + 1] << 16 | byteAr[index + 2] << 8 | byteAr[index + 3];
        }
    }

    private static double[] readValue(byte[] byteAr, int index, int valueLength, String tag) {
        if (Objects.equals(tag, "pixelSpacing") || Objects.equals(tag, "sliceThickness")
                || Objects.equals(tag, "imagePosition") || Objects.equals(tag,"windowCenter") ||
                Objects.equals(tag,"windowWidth") || Objects.equals(tag, "rescaleSlope") ||
                Objects.equals(tag, "rescaleIntercept")) {
            char[] chars = new char[valueLength];

            if (endian == "little") {
                for (int i = index + valueLength - 1; i >= index; i--) {
                    chars[i - index] = (char) byteAr[i];
                }
            } else {
                for (int i = index; i <= index + valueLength; i++) {
                    chars[i] = (char) byteAr[i];
                }
            }

            String[] k = new String(chars).split("\\\\");
            double[] output = new double[k.length];

            for (int i = 0; i < k.length; i++) {
                output[i] = Double.parseDouble(k[i]);
            }

            return output;
        } else {
            if (Objects.equals(endian, "little")) {
                int output = byteAr[index + valueLength - 1];
                for (int i = index + valueLength - 2; i >= index; i--) {
                    output = output << 8 | byteAr[i];
                }
                return new double[]{output};
            } else {
                int output = byteAr[index];
                for (int i = index + 1; i <= index + valueLength; i++) {
                    output = output << 8 | byteAr[i];
                }
                return new double[]{output};
            }
        }
    }

    public BufferedImage getSliceXY() {
        BufferedImage BImage = new BufferedImage(cols, rows, BufferedImage.TYPE_INT_ARGB);
        int[] greyValues = new int[cols * rows];

        int min = windowCenter - windowWidth;
        int max = windowCenter + windowWidth;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (pixelData[z][i][j] <= min) {
                    greyValues[i * cols + j] = min;
                } else if (pixelData[z][i][j] >= max) {
                    greyValues[i * cols + j] = max;
                } else {
                    greyValues[i * cols + j] = pixelData[z][i][j];
                }
            }
        }

        int[] output = new int[cols * rows];
        for (int i = 0; i < cols * rows; i++) {
            int pixValue = (int) (((double) (greyValues[i] - min) / (2 * windowWidth)) * 255.0);
            output[i] = pixValue << 16 | pixValue << 8 | pixValue | 0xFF000000;
        }
        BImage.setRGB(0, 0, cols, rows, output, 0, cols);
        return BImage;
    }

    public BufferedImage getSliceYZ() {
        BufferedImage BImage = new BufferedImage(rows, rows, BufferedImage.TYPE_INT_ARGB);

        int min = windowCenter - windowWidth;
        int max = windowCenter + windowWidth;

        int[][] imageResized = new int[rows][rows];
        int remainder = rows % numOfImages;
        int p = rows / numOfImages;

        for (int i = 0; i < numOfImages; i++) {
            int row = i >= remainder ? remainder * (p + 1) + (i - remainder) * p : i * (p + 1);
            for (int k = 0; k < rows / numOfImages + (rows % numOfImages > i ? 1 : 0); k++) {
                for (int j = 0; j < rows; j++) {
                    int val = pixelData[i][j][x];
                    if (val > max) {
                        imageResized[row + k][j] = max;
                    } else if (val < min) {
                        imageResized[row + k][j] = min;
                    } else {
                        imageResized[row + k][j] = val;
                    }
                }
            }
        }

        int[] output = new int[rows * rows];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < rows; j++) {
                int piksValue = (int) (((double) (imageResized[i][j] - min) / (2 * windowWidth)) * 255.0);
                output[rows * rows - (i * rows + j) - 1] = piksValue << 16 | piksValue << 8 | piksValue | 0xFF000000;
            }
        }
        BImage.setRGB(0, 0, rows, rows, output, 0, rows);
        return BImage;
    }

    public BufferedImage getSliceXZ() {
        BufferedImage BImage = new BufferedImage(cols, cols, BufferedImage.TYPE_INT_ARGB);

        int min = windowCenter - windowWidth;
        int max = windowCenter + windowWidth;

        int[][] imageArray = new int[numOfImages][rows];

        for (int i = 0; i < numOfImages; i++) {
            for (int j = 0; j < cols; j++) {
                imageArray[i][j] = pixelData[i][y][j];
            }
        }

        int[][] imageResized = new int[cols][cols];
        int remainder = cols % numOfImages;
        int p = cols / numOfImages;

        for (int i = 0; i < numOfImages; i++) {
            int row = i >= remainder ? remainder * (p + 1) + (i - remainder) * p : i * (p + 1);
            for (int k = 0; k < cols / numOfImages + (cols % numOfImages > i ? 1 : 0); k++) {
                for (int j = 0; j < cols; j++) {
                    int val = pixelData[i][y][j];
                    if (val > max) {
                        imageResized[row + k][j] = max;
                    } else if (val < min) {
                        imageResized[row + k][j] = min;
                    } else {
                        imageResized[row + k][j] = val;
                    }
                }
            }
        }


        int[] output = new int[cols * cols];
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < cols; j++) {
                int pixValue = (int) (((double) (imageResized[i][j] - min) / (2 * windowWidth)) * 255.0);
                output[cols * cols - (i * cols + j) - 1] = pixValue << 16 | pixValue << 8 | pixValue | 0xFF000000;
            }
        }
        BImage.setRGB(0, 0, cols, cols, output, 0, cols);
        return BImage;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public void setWindowCenter(int windowCenter) {
        this.windowCenter = windowCenter;
    }

    public void setWindowWidth(int windowWidth) {
        this.windowWidth = windowWidth;
    }

    public int getCols() {
        return cols;
    }

    public int getRows() {
        return rows;
    }

    public double[] getPixelSpacing() {return pixelSpacing;}

    public void printParams() {
        System.out.println("---------------");
        System.out.println("Images num: " + numOfImages);
        System.out.println("Rows: " + rows);
        System.out.println("Cols: " + cols);
        System.out.println("Pixel spacing: " + Arrays.toString(pixelSpacing));
        System.out.println("Slice thickness: " + sliceThickness);
        System.out.println("Image position: " + Arrays.toString(imagePosition));
        System.out.println("Bits stored: " + bitsStored);
        System.out.println("Bits allocated: " + bitsAllocated);
        System.out.println("Rescale slope: " + rescaleSlope);
        System.out.println("Rescale intercept: " + rescaleIntercept);
        System.out.println("Window width: " + windowWidth);
        System.out.println("Window center: " + windowCenter);
        System.out.println("---------------");
    }
}
