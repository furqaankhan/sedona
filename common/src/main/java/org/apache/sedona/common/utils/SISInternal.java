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


import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.internal.feature.Geometries;
import org.apache.sis.internal.feature.jts.JTS;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * This class is to keep all the Apache SIS internal classes in one place so that it's easier to manage any changes
 * in Apache SIS internal classes.
 */
public class SISInternal {
    public static Geometry transform(Geometry geom, MathTransform crs) throws FactoryException, TransformException {
        return JTS.transform(geom, crs);
    }

    public static Envelope2D getEnvelope2D(final Geometry geometry) throws FactoryException {
        final Envelope bounds = geometry.getEnvelopeInternal();
        final Envelope2D env = new Envelope2D();
        env.setCoordinateReferenceSystem(JTS.getCoordinateReferenceSystem(geometry));
        env.setFrameFromDiagonal(bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY());
        return env;
    }
}
