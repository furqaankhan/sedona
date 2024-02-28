/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sedona.common.utils;


import org.apache.sis.coverage.grid.GridCoverage2D;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.feature.jts.JTS;
import org.locationtech.jts.geom.*;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * This class is to keep all the Apache SIS internal classes in one place so that it's easier to manage any changes
 * in Apache SIS internal classes.
 */
public class SISInternal {

    private static GeometryFactory geometryFactory = new GeometryFactory();

    public static Geometry transform(Geometry geom, MathTransform transform) throws FactoryException, TransformException {
        return JTS.transform(geom, transform);
    }

    public static Envelope2D getEnvelope2D(final Geometry geometry) throws FactoryException {
        final Envelope bounds = geometry.getEnvelopeInternal();
        final Envelope2D envelope = new Envelope2D();
        envelope.setCoordinateReferenceSystem(JTS.getCoordinateReferenceSystem(geometry));
        envelope.setFrameFromDiagonal(bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY());
        return envelope;
    }

    public static Envelope2D getEnvelope2D(GridCoverage2D raster) {
        org.opengis.geometry.Envelope bounds = raster.getEnvelope().get();
        Envelope2D envelope = new Envelope2D();
        envelope.setCoordinateReferenceSystem(bounds.getCoordinateReferenceSystem());
        DirectPosition lowerCorner = bounds.getLowerCorner();
        DirectPosition upperCorner = bounds.getUpperCorner();
        envelope.setFrameFromDiagonal(lowerCorner.getOrdinate(0), lowerCorner.getOrdinate(1), upperCorner.getOrdinate(0), upperCorner.getOrdinate(1));
        return envelope;
    }

    /**
     * Creates a JTS polygon from org.opengis.geometry.Envelope. This method was created to support JTS.toGeometry from GeoTools
     * @param envelope
     * @return
     */
    public static Polygon toGeometry(org.opengis.geometry.Envelope envelope) {
        DirectPosition lowerCorner = envelope.getLowerCorner();
        double minX = lowerCorner.getOrdinate(0);
        double minY = lowerCorner.getOrdinate(1);
        DirectPosition upperCorner = envelope.getUpperCorner();
        double maxX = lowerCorner.getOrdinate(0);
        double maxY = lowerCorner.getOrdinate(1);
        Polygon polygon = geometryFactory.createPolygon(
                geometryFactory.createLinearRing(
                        new Coordinate[] {
                                new Coordinate(minX, minY),
                                new Coordinate(maxX, minY),
                                new Coordinate(maxX, maxY),
                                new Coordinate(minX, maxY),
                                new Coordinate(minX, minY)
                        }
                )
        );
        polygon.setUserData(envelope.getCoordinateReferenceSystem());
        return polygon;
    }
}
