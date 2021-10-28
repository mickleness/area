package com.pump.awt.geom;


import junit.framework.TestCase;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * This tests the performance of different QArea implementations for certain tasks.
 * <p>
 * The results are saved as .log files. The ambition of this project is that as
 * these tables will start off comparably equal at first, and as we commit more
 * to this project the QAreaImpl will start to show a noticeable improvement.
 * </p>
 * <p>
 * Also most tests confirm that the visual results of both QArea implementations stay
 * constant. (That is: the new changes this project introduces shouldn't break anything.)
 * </p>
 */
public class QAreaPerformanceTests extends TestCase {

    @Test
    public void testAddLetters() throws FileNotFoundException {
        testCombiningShapes("Letters", getLetterGlyphs());
    }

    private List<Shape> getLetterGlyphs() {
        String letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        List<Shape> glyphs = new ArrayList<>(letters.length());
        Font font = new Font("serif", 0, 120);
        FontRenderContext frc = new FontRenderContext(new AffineTransform(), true, true);
        for(int a = 0; a<letters.length(); a++) {
            char[] chars = new char[] { letters.charAt(a) };
            GlyphVector gv = font.createGlyphVector(frc, chars);
            glyphs.add(gv.getOutline());
        }
        return glyphs;
    }

    @Test
    public void testRandomTriangles() throws FileNotFoundException {
        List<Shape> shapes = new ArrayList<>();
        Random random = new Random(0);
        for(int a = 0; a < 100; a++) {
            Path2D p = new Path2D.Double();
            double x = random.nextDouble() * 1000;
            double y = random.nextDouble() * 1000;
            p.moveTo(x + 200 * random.nextDouble(), y + 200 * random.nextDouble());
            p.lineTo(x + 200 * random.nextDouble(), y + 200 * random.nextDouble());
            p.lineTo(x + 200 * random.nextDouble(), y + 200 * random.nextDouble());
            shapes.add(p);
        }

        testCombiningShapes("Random Triangles", shapes);
    }

    @Test
    public void testRandomQuads() throws FileNotFoundException {
        List<Shape> shapes = new ArrayList<>();
        Random random = new Random(0);
        for(int a = 0; a < 100; a++) {
            Path2D p = new Path2D.Double();
            double x = random.nextDouble() * 1000;
            double y = random.nextDouble() * 1000;
            p.moveTo(x + 200 * random.nextDouble(), y + 200 * random.nextDouble());
            p.quadTo(x + 200 * random.nextDouble(), y + 200 * random.nextDouble(),
                    x + 200 * random.nextDouble(), y + 200 * random.nextDouble());
            shapes.add(p);
        }

        testCombiningShapes("Random Quads", shapes);
    }

    @Test
    public void testRandomCubics() throws FileNotFoundException {
        List<Shape> shapes = new ArrayList<>();
        Random random = new Random(0);
        for(int a = 0; a < 100; a++) {
            Path2D p = new Path2D.Double();
            double x = random.nextDouble() * 1000;
            double y = random.nextDouble() * 1000;
            p.moveTo(x + 200 * random.nextDouble(), y + 200 * random.nextDouble());
            p.curveTo(x + 200 * random.nextDouble(), y + 200 * random.nextDouble(),
                    x + 200 * random.nextDouble(), y + 200 * random.nextDouble(),
                    x + 200 * random.nextDouble(), y + 200 * random.nextDouble());
            shapes.add(p);
        }

        testCombiningShapes("Random Cubics", shapes);
    }

    abstract class TestActivity {
        String name;

        public TestActivity(String name) {
            Objects.requireNonNull(name);
            this.name = name;
        }

        /**
         * This is executed just before the timer starts.
         */
        public void setup(int trial, QAreaFactory factory) {}

        /**
         * This is the activity we time.
         */
        public abstract QArea run(int trial, QAreaFactory factory);

        public void runAll() throws FileNotFoundException {
            QAreaFactory[] factories = getFactories();
            long[][] resultsTable = new long[10][factories.length];
            Logger log = createFileLogger(name);

            log.info("This table catalogs the time in ms two different QArea implementations took to do the same work.");

            StringBuilder tableBuilder = new StringBuilder();
            String tableHeader = QAreaPerformanceTests.toString(factories);
            tableBuilder.append(tableHeader+"\n");
            System.out.println(tableHeader);

            for(int trial = 0; trial < 10; trial++) {
                BufferedImage expectedImage = null;
                for(int factoryIndex = 0; factoryIndex < factories.length; factoryIndex++) {
                    QAreaFactory factory = factories[factoryIndex];
                    long[] sampleTimes = new long[5];
                    for (int sample = 0; sample < sampleTimes.length; sample++) {

                        setup(trial, factory);

                        System.gc();
                        System.runFinalization();
                        System.gc();
                        System.runFinalization();

                        sampleTimes[sample] = System.currentTimeMillis();

                        QArea result = run(trial, factory);

                        sampleTimes[sample] = System.currentTimeMillis() - sampleTimes[sample];

                        if (sample == 0) {
                            if (factoryIndex == 0) {
                                expectedImage = createImage(result);
                            } else {
                                BufferedImage bi = createImage(result);
                                assertImageEquals(name+"-"+trial+"-"+factory, expectedImage, bi);
                            }
                        }
                    }
                    Arrays.sort(sampleTimes);
                    resultsTable[trial][factoryIndex] = sampleTimes[sampleTimes.length/2];
                }
                String tableRow = QAreaPerformanceTests.toString(resultsTable[trial]);
                tableBuilder.append(tableRow+"\n");
                System.out.println(tableRow);
            }

            log.info("Data:\n"+tableBuilder.toString());
            log.info("finished");
            for(Handler h : log.getHandlers()) {
                h.close();
            }
        }
    }

    class TransformActivity extends TestActivity {
        QArea baseShape;
        AffineTransform tx;

        public TransformActivity(String name, AffineTransform tx) {
            super(name);
            this.tx = tx;
        }

        @Override
        public void setup(int trial, QAreaFactory factory) {
            baseShape = createLargeShape(trial, factory, getLetterGlyphs());
        }

        @Override
        public QArea run(int trial, QAreaFactory factory) {
            QArea copy = (QArea) baseShape.cloneArea();
            copy.transform(tx);
            return copy;
        }
    }

    @Test
    public void testTransform_translate() throws FileNotFoundException {
        TestActivity activity = new TransformActivity("translate", AffineTransform.getTranslateInstance(10, 10));
        activity.runAll();
    }

    @Test
    public void testTransform_scale() throws FileNotFoundException {
        TestActivity activity = new TransformActivity("scale", AffineTransform.getScaleInstance(1.234123423, 2.351235123));
        activity.runAll();
    }

    @Test
    public void testTransform_flipHorizontal() throws FileNotFoundException {
        TestActivity activity = new TransformActivity("flip horizontal", AffineTransform.getScaleInstance(-1, 1));
        activity.runAll();
    }

    @Test
    public void testTransform_flipVertical() throws FileNotFoundException {
        TestActivity activity = new TransformActivity("flip vertical", AffineTransform.getScaleInstance(1, -1));
        activity.runAll();
    }

    @Test
    public void testTransform_flipBoth() throws FileNotFoundException {
        TestActivity activity = new TransformActivity("flip both", AffineTransform.getScaleInstance(-1.9182841212312, -.1435183451349));
        activity.runAll();
    }

    private void testCombiningShapes(String name, List<Shape> shapes) throws FileNotFoundException {
        TestActivity activity = new TestActivity(name) {
            @Override
            public QArea run(int trial, QAreaFactory factory) {
                return createLargeShape(trial, factory, shapes);
            }
        };
        activity.runAll();
    }

    private QArea createLargeShape(int randomSeed, QAreaFactory factory, List<Shape> shapes) {
        Random random = new Random(randomSeed);
        List<Shape> shapesCopy = new LinkedList<>();
        shapesCopy.addAll(shapes);
        shapesCopy.addAll(shapes);

        QArea sum = null;

        while (!shapesCopy.isEmpty()) {
            int i = random.nextInt(shapesCopy.size());
            Shape shape = shapesCopy.remove(i);
            QArea a = factory.create(shape);
            double dx = 400 * random.nextDouble() - 200;
            double dy = 400 * random.nextDouble() - 200;
            a.transform(AffineTransform.getTranslateInstance(dx, dy));
            if (sum == null) {
                sum = a;
            } else {
                int op = shapesCopy.size() % 3;
                switch (op) {
                    case 0:
                    case 1:
                        sum.add(a);
                        break;
                    case 2:
                        sum.subtract(a);
                        break;
                }
            }
        }
        return sum;
    }

    private void assertImageEquals(String name, BufferedImage expected, BufferedImage actual) {
        try {
            assertEquals(expected.getHeight(), actual.getHeight());
            assertEquals(expected.getWidth(), actual.getWidth());
            assertEquals(expected.getType(), actual.getType());

            int w = expected.getWidth();
            int h = expected.getHeight();
            int[] row1 = new int[w];
            int[] row2 = new int[w];
            for (int y = 0; y < h; y++) {
                expected.getRaster().getDataElements(0, y, w, 1, row1);
                actual.getRaster().getDataElements(0, y, w, 1, row2);
                for (int x = 0; x < w; x++) {
                    int argb1 = row1[x];
                    int argb2 = row2[x];
                    assertEquals(x + ", " + y, argb1, argb2);
                }
            }
        } catch(Throwable t) {
            writeImage(name+"-expected", expected);
            writeImage(name+"-actual", actual);
            throw t;
        }
    }

    private Logger createFileLogger(String name) throws FileNotFoundException {
        File file = new File(name+" Output.log");
        FileOutputStream fileOut = new FileOutputStream(file);
        OutputStreamWriter writer = new OutputStreamWriter(fileOut);
        Logger logger = Logger.getLogger(name);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                String str = record.getMessage();
                System.out.println(str);
                try {
                    writer.write(str);
                    writer.write("\n");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void flush() {
                try {
                    writer.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void close() throws SecurityException {
                flush();

                try {
                    writer.close();
                    fileOut.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return logger;
    }

    public static String toString(Object[] array) {
        StringBuilder sb = new StringBuilder();
        for (int a = 0; a<array.length; a++) {
            if (a != 0) {
                sb.append("\t");
            }
            sb.append(array[a].toString());
        }
        return sb.toString();
    }

    public static String toString(long[] array) {
        StringBuilder sb = new StringBuilder();
        for (int a = 0; a<array.length; a++) {
            if (a != 0) {
                sb.append("\t");
            }
            sb.append(Long.toString(array[a]));
        }
        return sb.toString();
    }

    private void writeImage(String name, BufferedImage bi) {
        File file = new File(name+".png");
        try {
            ImageIO.write(bi, "png", file);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private BufferedImage createImage(Shape shape) {
        Rectangle r = shape.getBounds();
        BufferedImage bi = new BufferedImage(r.width, r.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        g.setColor(Color.black);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.translate(-r.x, -r.y);
        g.fill(shape);
        g.dispose();
        return bi;
    }

    /**
     * Return the factories to test, where the first factory also models the expected behavior.
     */
    private QAreaFactory[] getFactories() {
        return new QAreaFactory[] { LegacyArea.FACTORY, QAreaImpl.FACTORY };
    }
}
