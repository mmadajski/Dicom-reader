package org.example;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;

public class App {
    private static JFrame frame;
    private static JLabel imageXY;
    private static JLabel imageYZ;
    private static JLabel imageXZ;
    private static int cols;
    private static int rows;
    private static int x;
    private static int y;
    private static int z;

    private static int windowCenter;
    private static int windowWidth;

    private static ArrayList<int[]> pointsXY = new ArrayList<>();
    private static ArrayList<int[]> pointsYZ= new ArrayList<>();
    private static ArrayList<int[]> pointsXZ= new ArrayList<>();

    private static double[] pixelSpacing;

    static DicomImage image;

    public static void main( String[] args ) throws IOException {
        try {
            image = new DicomImage("\\src\\main\\java\\org\\example\\head-dicom");
        } catch (IOException e) {
            e.printStackTrace();
        }

        image.printParams();
        cols = image.getCols();
        rows = image.getRows();
        pixelSpacing = image.getPixelSpacing();

        frame = new JFrame("Dicom reader");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        createPane(frame.getContentPane());

        frame.pack();
        windowCenter = 60;
        windowWidth = 120;
        updateWindowCenter();
        updateWindowWidth();
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setVisible(true);
    }

    private static void createPane(Container pane) {
        JPanel sliderPanel = new JPanel();
        sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.PAGE_AXIS));

        JPanel imgPanel = new JPanel();
        imgPanel.setLayout(new BoxLayout(imgPanel, BoxLayout.X_AXIS));

        //image1
        imageXY = new JLabel(new ImageIcon(image.getSliceXY()));
        imageXY.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                pointsXY.add(new int[]{e.getX(), e.getY()});

                if (pointsXY.size() == 2) {
                    repaint_with_lineXY();
                } else if (pointsXY.size() > 2) {
                    pointsXY.remove(0);
                    repaint_with_lineXY();
                }
            }
            @Override
            public void mousePressed(MouseEvent e) {}
            @Override
            public void mouseReleased(MouseEvent e) {}
            @Override
            public void mouseEntered(MouseEvent e) {}
            @Override
            public void mouseExited(MouseEvent e) {}
        });
        imgPanel.add(imageXY);

        //image2
        imageYZ = new JLabel(new ImageIcon(image.getSliceYZ()));
        imageYZ.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                pointsYZ.add(new int[]{e.getX(), e.getY()});

                if (pointsYZ.size() == 2) {
                    repaint_with_lineYZ();
                } else if (pointsYZ.size() > 2) {
                    pointsYZ.remove(0);
                    repaint_with_lineYZ();
                }
            }
            @Override
            public void mousePressed(MouseEvent e) {}
            @Override
            public void mouseReleased(MouseEvent e) {}
            @Override
            public void mouseEntered(MouseEvent e) {}
            @Override
            public void mouseExited(MouseEvent e) {}
        });
        imgPanel.add(imageYZ);

        //image3
        imageXZ = new JLabel(new ImageIcon(image.getSliceXZ()));
        imageXZ.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                pointsXZ.add(new int[]{e.getX(), e.getY()});

                if (pointsXZ.size() == 2) {
                    repaint_with_lineXZ();
                } else if (pointsXZ.size() > 2) {
                    pointsXZ.remove(0);
                    repaint_with_lineXZ();
                }
            }
            @Override
            public void mousePressed(MouseEvent e) {}
            @Override
            public void mouseReleased(MouseEvent e) {}
            @Override
            public void mouseEntered(MouseEvent e) {}
            @Override
            public void mouseExited(MouseEvent e) {}
        });
        imgPanel.add(imageXZ);
        pane.add(imgPanel, BorderLayout.PAGE_START);

        //Slider1
        sliderPanel.add(new JLabel("Slider x: "));
        JSlider sliderX = new JSlider(0, image.cols - 1,  x);
        sliderX.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider) e.getSource();
                x = source.getValue();
                updateX();
            }
        });
        sliderPanel.add(sliderX);

        //Slider2
        sliderPanel.add(new JLabel("Slider y: "));
        JSlider sliderY = new JSlider(0, image.rows - 1, y);
        sliderY.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider) e.getSource();
                y = source.getValue();
                updateY();
            }
        });
        sliderPanel.add(sliderY);

        //Slider3
        sliderPanel.add(new JLabel("Slice: "));
        JSlider sliderZ = new JSlider(0, image.numOfImages,  z);
        sliderZ.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider) e.getSource();
                z = source.getValue();
                try {
                    updateZ();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        sliderPanel.add(sliderZ);

        //Slider4
        sliderPanel.add(new JLabel("Window Center: "));
        JSlider sliderWindowCenter = new JSlider(-4000, 4000, windowCenter);
        sliderWindowCenter.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider) e.getSource();
                windowCenter = source.getValue();
                updateWindowCenter();
            }
        });
        sliderPanel.add(sliderWindowCenter);
        pane.add(sliderPanel, BorderLayout.PAGE_END);

        //Slider5
        sliderPanel.add(new JLabel("Window width: "));
        JSlider sliderWindowWidth= new JSlider(0, 4000, windowWidth);
        sliderWindowWidth.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider) e.getSource();
                windowWidth = source.getValue();
                updateWindowWidth();
            }
        });
        sliderPanel.add(sliderWindowWidth);
        pane.add(sliderPanel, BorderLayout.PAGE_END);

    }

    public static double calculateDist(int[] p1, int[] p2) {
        return Math.sqrt(
                        ((p1[0] - p2[0]) * pixelSpacing[1]) * ((p1[0] - p2[0]) * pixelSpacing[1]) +
                        ((p1[1] - p2[1]) * pixelSpacing[0]) * ((p1[1] - p2[1]) * pixelSpacing[0])
        );
    }

    private static void updateY() {
        image.setY(y);
        imageXZ.setIcon(new ImageIcon(image.getSliceXZ()));
        frame.repaint();
    }

    private static void updateX() {
        image.setX(x);

        imageYZ.setIcon(new ImageIcon(image.getSliceYZ()));
        frame.repaint();
    }

    private static void updateZ() throws IOException {
        image.setZ(z);
        imageXY.setIcon(new ImageIcon(image.getSliceXY()));
        frame.repaint();
    }

    private static void updateWindowCenter() {
        image.setWindowCenter(windowCenter);

        imageXY.setIcon(new ImageIcon(image.getSliceXY()));
        imageYZ.setIcon(new ImageIcon(image.getSliceYZ()));
        imageXZ.setIcon(new ImageIcon(image.getSliceXZ()));

        frame.repaint();
    }

    private static void updateWindowWidth() {
        image.setWindowWidth(windowWidth);

        imageXY.setIcon(new ImageIcon(image.getSliceXY()));
        imageYZ.setIcon(new ImageIcon(image.getSliceYZ()));
        imageXZ.setIcon(new ImageIcon(image.getSliceXZ()));

        frame.repaint();
    }

    private static void repaint_with_lineXY() {
        BufferedImage newImage = image.getSliceXY();
        Graphics2D graphics = newImage.createGraphics();

        graphics.setStroke(new BasicStroke(2f));
        graphics.setColor(Color.GREEN);
        graphics.drawLine(pointsXY.get(0)[0], pointsXY.get(0)[1], pointsXY.get(1)[0], pointsXY.get(1)[1]);

        double dist = calculateDist(pointsXY.get(0), pointsXY.get(1));
        graphics.drawString(Double.toString(dist), Math.min(pointsXY.get(0)[0], pointsXY.get(1)[0]) + (pointsXY.get(0)[0] - pointsXY.get(1)[0]) / 2, Math.min(pointsXY.get(0)[1], pointsXY.get(1)[1]));

        graphics.drawImage(newImage, 0, 0, cols, rows, null);
        graphics.dispose();
        imageXY.setIcon(new ImageIcon(newImage));
        frame.repaint();
    }

    private static void repaint_with_lineYZ() {
        BufferedImage newImage = image.getSliceYZ();
        Graphics2D graphics = newImage.createGraphics();

        graphics.setStroke(new BasicStroke(2f));
        graphics.setColor(Color.GREEN);
        graphics.drawLine(pointsYZ.get(0)[0], pointsYZ.get(0)[1], pointsYZ.get(1)[0], pointsYZ.get(1)[1]);

        double dist = calculateDist(pointsYZ.get(0), pointsYZ.get(1));
        graphics.drawString(Double.toString(dist), Math.min(pointsYZ.get(0)[0], pointsYZ.get(1)[0]) + (pointsYZ.get(0)[0] - pointsYZ.get(1)[0]) / 2, Math.min(pointsYZ.get(0)[1], pointsYZ.get(1)[1]));

        graphics.drawImage(newImage, 0, 0, cols, rows, null);
        graphics.dispose();
        imageYZ.setIcon(new ImageIcon(newImage));
        frame.repaint();
    }

    private static void repaint_with_lineXZ() {
        BufferedImage newImage = image.getSliceXZ();
        Graphics2D graphics = newImage.createGraphics();

        graphics.setStroke(new BasicStroke(2f));
        graphics.setColor(Color.GREEN);
        graphics.drawLine(pointsXZ.get(0)[0], pointsXZ.get(0)[1], pointsXZ.get(1)[0], pointsXZ.get(1)[1]);

        double dist = calculateDist(pointsXZ.get(0), pointsXZ.get(1));
        graphics.drawString(Double.toString(dist), Math.min(pointsXZ.get(0)[0], pointsXZ.get(1)[0]) + (pointsXZ.get(0)[0] - pointsXZ.get(1)[0]) / 2, Math.min(pointsXZ.get(0)[1], pointsXZ.get(1)[1]));

        graphics.drawImage(newImage, 0, 0, cols, rows, null);
        graphics.dispose();
        imageXZ.setIcon(new ImageIcon(newImage));
        frame.repaint();
    }
}
