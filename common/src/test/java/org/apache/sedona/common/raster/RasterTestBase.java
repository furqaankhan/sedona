/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sedona.common.raster;

 import org.apache.sedona.common.utils.GeomUtils;
 import org.apache.sis.coverage.grid.GridCoverage2D;
 import org.apache.sis.geometry.DirectPosition2D;
 import org.apache.sis.referencing.CRS;
 import org.apache.sis.util.Utilities;
 import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.junit.Assert;
import org.junit.Before;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
 import org.opengis.util.FactoryException;

 import javax.media.jai.RasterFactory;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class RasterTestBase {
    String arc = "NCOLS 2\nNROWS 2\nXLLCORNER 378922\nYLLCORNER 4072345\nCELLSIZE 30\nNODATA_VALUE 0\n0 1 2 3\n";

    protected static final String resourceFolder = System.getProperty("user.dir") + "/../spark/common/src/test/resources/";

    protected static final double FP_TOLERANCE = 1E-4;

    GridCoverage2D oneBandRaster;
    GridCoverage2D multiBandRaster;
    byte[] geoTiff;
    byte[] testNc;
    String ncFile = resourceFolder + "raster/netcdf/test.nc";


    @Before
    public void setup() throws IOException {
        oneBandRaster = RasterConstructors.fromArcInfoAsciiGrid(arc.getBytes(StandardCharsets.UTF_8));
        multiBandRaster = createMultibandRaster();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        new GeoTiffWriter(bos).write(multiBandRaster, new GeneralParameterValue[]{});
        geoTiff = bos.toByteArray();
        File file = new File(ncFile);
        testNc = Files.readAllBytes(file.toPath());
    }

    GridCoverage2D createEmptyRaster(int numBands)
            throws FactoryException
    {
        int widthInPixel = 4;
        int heightInPixel = 5;
        double upperLeftX = 101;
        double upperLeftY = 102;
        double cellSize = 2;
        return RasterConstructors.makeEmptyRaster(numBands, widthInPixel, heightInPixel, upperLeftX, upperLeftY, cellSize);
    }

    GridCoverage2D createRandomRaster(int dataBufferType, int widthInPixel, int heightInPixel,
                                      double upperLeftX, double upperLeftY, double pixelSize,
                                      int numBand, String crsCode) {
        WritableRaster raster =
                RasterFactory.createBandedRaster(
                        dataBufferType, widthInPixel, heightInPixel, numBand, null);
        for (int b = 0; b < numBand; b++) {
            for (int y = 0; y < heightInPixel; y++) {
                for (int x = 0; x < widthInPixel; x++) {
                    double value = Math.random() * 255;
                    raster.setSample(x, y, b, value);
                }
            }
        }
        CoordinateReferenceSystem crs = null;
        if (crsCode != null && !crsCode.isEmpty()) {
            try {
                crs = GeomUtils.longitudeFirstCRS(CRS.forCode(crsCode));
            } catch (FactoryException e) {
                throw new RuntimeException(e);
            }
        }
        double x1 = upperLeftX;
        double x2 = upperLeftX + widthInPixel * pixelSize;
        double y1 = upperLeftY - heightInPixel * pixelSize;
        double y2 = upperLeftY;
        ReferencedEnvelope referencedEnvelope = new ReferencedEnvelope(x1, x2, y1, y2, crs);
        GridCoverageFactory factory = new GridCoverageFactory();
        return factory.create("random-raster", raster, referencedEnvelope);
    }

    GridCoverage2D createMultibandRaster() throws IOException {
        GridCoverageFactory factory = new GridCoverageFactory();
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < image.getHeight(); i++) {
            for (int j = 0; j < image.getWidth(); j++) {
                int color = i + j;
                image.setRGB(i, j, new Color(color, color, color).getRGB());
            }
        }
        return factory.create("test", image, new Envelope2D(DefaultGeographicCRS.WGS84, 0, 0, 10, 10));
    }

    protected void assertSameCoverage(GridCoverage2D expected, GridCoverage2D actual, int density) {
        Assert.assertEquals(expected.getSampleDimensions().size(), actual.getSampleDimensions().size());
        Envelope expectedEnvelope = expected.getEnvelope().get();
        Envelope actualEnvelope = actual.getEnvelope().get();
        assertSameEnvelope(expectedEnvelope, actualEnvelope, 1e-6);
        CoordinateReferenceSystem expectedCrs = expected.getCoordinateReferenceSystem();
        CoordinateReferenceSystem actualCrs = actual.getCoordinateReferenceSystem();
        Assert.assertTrue(Utilities.equalsIgnoreMetadata(expectedCrs, actualCrs));
        assertSameValues(expected, actual, density);
    }

    protected void assertSameEnvelope(Envelope expected, Envelope actual, double epsilon) {
        Assert.assertEquals(expected.getMinimum(0), actual.getMinimum(0), epsilon);
        Assert.assertEquals(expected.getMinimum(1), actual.getMinimum(1), epsilon);
        Assert.assertEquals(expected.getMaximum(0), actual.getMaximum(0), epsilon);
        Assert.assertEquals(expected.getMaximum(1), actual.getMaximum(1), epsilon);
    }

    protected void assertSameValues(GridCoverage2D expected, GridCoverage2D actual, int density) {
        Envelope expectedEnvelope = expected.getEnvelope().get();
        double x0 = expectedEnvelope.getMinimum(0);
        double y0 = expectedEnvelope.getMinimum(1);
        double xStep = (expectedEnvelope.getMaximum(0) - x0) / density;
        double yStep = (expectedEnvelope.getMaximum(1) - y0) / density;
        double[] expectedValues = new double[expected.getSampleDimensions().size()];
        double[] actualValues = new double[expected.getSampleDimensions().size()];
        int sampledPoints = 0;
        for (int i = 0; i < density; i++) {
            for (int j = 0; j < density; j++) {
                double x = x0 + j * xStep;
                double y = y0 + i * yStep;
                DirectPosition position = new DirectPosition2D(x, y);
                try {
                    GridCoordinates2D gridPosition = expected.getGridGeometry().worldToGrid(position);
                    if (Double.isNaN(gridPosition.getX()) || Double.isNaN(gridPosition.getY())) {
                        // This position is outside the coverage
                        continue;
                    }
                    expected.evaluate(position, expectedValues);
                    actual.evaluate(position, actualValues);
                    Assert.assertEquals(expectedValues.length, actualValues.length);
                    for (int k = 0; k < expectedValues.length; k++) {
                        Assert.assertEquals(expectedValues[k], actualValues[k], 1e-6);
                    }
                    sampledPoints += 1;
                } catch (TransformException e) {
                    throw new RuntimeException("Failed to convert world coordinate to grid coordinate", e);
                }
            }
        }
        Assert.assertTrue(sampledPoints > density * density / 2);
    }

    GridCoverage2D rasterFromGeoTiff(String filePath) throws IOException {
        File geoTiffFile = new File(filePath);
        GridCoverage2D raster = new GeoTiffReader(geoTiffFile).read(null);
        return raster;
    }
}
