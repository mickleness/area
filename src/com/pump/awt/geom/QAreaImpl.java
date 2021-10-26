/*
 * This is a derivative of java.awt.geom.Area. Its original license reads:
 *
 * Copyright (c) 1998, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.pump.awt.geom;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;

public class QAreaImpl implements QArea<QAreaImpl> {

    public static QAreaFactory<QAreaImpl> FACTORY = new QAreaFactory<QAreaImpl>() {
        @Override
        public QAreaImpl create(Shape shape) {
            return new QAreaImpl(shape);
        }

        @Override
        public String toString() {
            return "QAreaImpl Factory";
        }
    };

    private ExposedArrayWrapper<QCurve> curves;

    /**
     * Default constructor which creates an empty area.
     * @since 1.2
     */
    public QAreaImpl() {
        curves = new ExposedArrayWrapper<>(QCurve.class);
    }

    /**
     * The {@code Area} class creates an area geometry from the
     * specified {@link Shape} object.  The geometry is explicitly
     * closed, if the {@code Shape} is not already closed.  The
     * fill rule (even-odd or winding) specified by the geometry of the
     * {@code Shape} is used to determine the resulting enclosed area.
     * @param s  the {@code Shape} from which the area is constructed
     * @throws NullPointerException if {@code s} is null
     * @since 1.2
     */
    public QAreaImpl(Shape s) {
        if (s instanceof QAreaImpl) {
            curves = ((QAreaImpl) s).curves;
        } else {
            curves = pathToCurves(s.getPathIterator(null));
        }
    }

    private static ExposedArrayWrapper<QCurve> pathToCurves(PathIterator pi) {
        ExposedArrayWrapper<QCurve> curves = new ExposedArrayWrapper<>(QCurve.class);
        int windingRule = pi.getWindingRule();
        // coords array is big enough for holding:
        //     coordinates returned from currentSegment (6)
        //     OR
        //         two subdivided quadratic curves (2+4+4=10)
        //         AND
        //             0-1 horizontal splitting parameters
        //             OR
        //             2 parametric equation derivative coefficients
        //     OR
        //         three subdivided cubic curves (2+6+6+6=20)
        //         AND
        //             0-2 horizontal splitting parameters
        //             OR
        //             3 parametric equation derivative coefficients
        double[] coords = new double[23];
        double movx = 0, movy = 0;
        double curx = 0, cury = 0;
        double newx, newy;
        while (!pi.isDone()) {
            switch (pi.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO:
                    QCurve.insertLine(curves, curx, cury, movx, movy);
                    curx = movx = coords[0];
                    cury = movy = coords[1];
                    QCurve.insertMove(curves, movx, movy);
                    break;
                case PathIterator.SEG_LINETO:
                    newx = coords[0];
                    newy = coords[1];
                    QCurve.insertLine(curves, curx, cury, newx, newy);
                    curx = newx;
                    cury = newy;
                    break;
                case PathIterator.SEG_QUADTO:
                    newx = coords[2];
                    newy = coords[3];
                    QCurve.insertQuad(curves, curx, cury, coords);
                    curx = newx;
                    cury = newy;
                    break;
                case PathIterator.SEG_CUBICTO:
                    newx = coords[4];
                    newy = coords[5];
                    QCurve.insertCubic(curves, curx, cury, coords);
                    curx = newx;
                    cury = newy;
                    break;
                case PathIterator.SEG_CLOSE:
                    QCurve.insertLine(curves, curx, cury, movx, movy);
                    curx = movx;
                    cury = movy;
                    break;
            }
            pi.next();
        }
        QCurve.insertLine(curves, curx, cury, movx, movy);
        QAreaOp operator;
        if (windingRule == PathIterator.WIND_EVEN_ODD) {
            operator = new QAreaOp.EOWindOp();
        } else {
            operator = new QAreaOp.NZWindOp();
        }
        return operator.calculate(curves, new ExposedArrayWrapper<>(QCurve.class));
    }

    /**
     * Adds the shape of the specified {@code Area} to the
     * shape of this {@code Area}.
     * The resulting shape of this {@code Area} will include
     * the union of both shapes, or all areas that were contained
     * in either this or the specified {@code Area}.
     * <pre>
     *     // Example:
     *     Area a1 = new Area([triangle 0,0 =&gt; 8,0 =&gt; 0,8]);
     *     Area a2 = new Area([triangle 0,0 =&gt; 8,0 =&gt; 8,8]);
     *     a1.add(a2);
     *
     *        a1(before)     +         a2         =     a1(after)
     *
     *     ################     ################     ################
     *     ##############         ##############     ################
     *     ############             ############     ################
     *     ##########                 ##########     ################
     *     ########                     ########     ################
     *     ######                         ######     ######    ######
     *     ####                             ####     ####        ####
     *     ##                                 ##     ##            ##
     * </pre>
     * @param   rhs  the {@code Area} to be added to the
     *          current shape
     * @throws NullPointerException if {@code rhs} is null
     * @since 1.2
     */
    @Override
    public void add(QAreaImpl rhs) {
        curves = new QAreaOp.AddOp().calculate(this.curves, rhs.curves);
        invalidateBounds();
    }

    /**
     * Subtracts the shape of the specified {@code Area} from the
     * shape of this {@code Area}.
     * The resulting shape of this {@code Area} will include
     * areas that were contained only in this {@code Area}
     * and not in the specified {@code Area}.
     * <pre>
     *     // Example:
     *     Area a1 = new Area([triangle 0,0 =&gt; 8,0 =&gt; 0,8]);
     *     Area a2 = new Area([triangle 0,0 =&gt; 8,0 =&gt; 8,8]);
     *     a1.subtract(a2);
     *
     *        a1(before)     -         a2         =     a1(after)
     *
     *     ################     ################
     *     ##############         ##############     ##
     *     ############             ############     ####
     *     ##########                 ##########     ######
     *     ########                     ########     ########
     *     ######                         ######     ######
     *     ####                             ####     ####
     *     ##                                 ##     ##
     * </pre>
     * @param   rhs  the {@code Area} to be subtracted from the
     *          current shape
     * @throws NullPointerException if {@code rhs} is null
     * @since 1.2
     */
    @Override
    public void subtract(QAreaImpl rhs) {
        curves = new QAreaOp.SubOp().calculate(this.curves, rhs.curves);
        invalidateBounds();
    }

    /**
     * Sets the shape of this {@code Area} to the intersection of
     * its current shape and the shape of the specified {@code Area}.
     * The resulting shape of this {@code Area} will include
     * only areas that were contained in both this {@code Area}
     * and also in the specified {@code Area}.
     * <pre>
     *     // Example:
     *     Area a1 = new Area([triangle 0,0 =&gt; 8,0 =&gt; 0,8]);
     *     Area a2 = new Area([triangle 0,0 =&gt; 8,0 =&gt; 8,8]);
     *     a1.intersect(a2);
     *
     *      a1(before)   intersect     a2         =     a1(after)
     *
     *     ################     ################     ################
     *     ##############         ##############       ############
     *     ############             ############         ########
     *     ##########                 ##########           ####
     *     ########                     ########
     *     ######                         ######
     *     ####                             ####
     *     ##                                 ##
     * </pre>
     * @param   rhs  the {@code Area} to be intersected with this
     *          {@code Area}
     * @throws NullPointerException if {@code rhs} is null
     * @since 1.2
     */
    @Override
    public void intersect(QAreaImpl rhs) {
        curves = new QAreaOp.IntOp().calculate(this.curves, rhs.curves);
        invalidateBounds();
    }

    /**
     * Sets the shape of this {@code Area} to be the combined area
     * of its current shape and the shape of the specified {@code Area},
     * minus their intersection.
     * The resulting shape of this {@code Area} will include
     * only areas that were contained in either this {@code Area}
     * or in the specified {@code Area}, but not in both.
     * <pre>
     *     // Example:
     *     Area a1 = new Area([triangle 0,0 =&gt; 8,0 =&gt; 0,8]);
     *     Area a2 = new Area([triangle 0,0 =&gt; 8,0 =&gt; 8,8]);
     *     a1.exclusiveOr(a2);
     *
     *        a1(before)    xor        a2         =     a1(after)
     *
     *     ################     ################
     *     ##############         ##############     ##            ##
     *     ############             ############     ####        ####
     *     ##########                 ##########     ######    ######
     *     ########                     ########     ################
     *     ######                         ######     ######    ######
     *     ####                             ####     ####        ####
     *     ##                                 ##     ##            ##
     * </pre>
     * @param   rhs  the {@code Area} to be exclusive ORed with this
     *          {@code Area}.
     * @throws NullPointerException if {@code rhs} is null
     * @since 1.2
     */
    @Override
    public void exclusiveOr(QAreaImpl rhs) {
        curves = new QAreaOp.XorOp().calculate(this.curves, rhs.curves);
        invalidateBounds();
    }

    /**
     * Removes all of the geometry from this {@code Area} and
     * restores it to an empty area.
     * @since 1.2
     */
    @Override
    public void reset() {
        curves = new ExposedArrayWrapper<>(QCurve.class);
        invalidateBounds();
    }

    /**
     * Tests whether this {@code Area} object encloses any area.
     * @return    {@code true} if this {@code Area} object
     * represents an empty area; {@code false} otherwise.
     * @since 1.2
     */
    @Override
    public boolean isEmpty() {
        return (curves.elementCount == 0);
    }

    /**
     * Tests whether this {@code Area} consists entirely of
     * straight edged polygonal geometry.
     * @return    {@code true} if the geometry of this
     * {@code Area} consists entirely of line segments;
     * {@code false} otherwise.
     * @since 1.2
     */
    @Override
    public boolean isPolygonal() {
        for (int a = 0; a<curves.elementCount; a++) {
            if (curves.elementData[a].order > 1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tests whether this {@code Area} is rectangular in shape.
     * @return    {@code true} if the geometry of this
     * {@code Area} is rectangular in shape; {@code false}
     * otherwise.
     * @since 1.2
     */
    @Override
    public boolean isRectangular() {
        int size = curves.elementCount;
        if (size == 0) {
            return true;
        }
        if (size > 3) {
            return false;
        }
        QCurve c1 = curves.elementData[1];
        QCurve c2 = curves.elementData[2];
        if (c1.order != 1 || c2.order != 1) {
            return false;
        }
        if (c1.x0 != c1.x1 || c2.x0 != c2.x1) {
            return false;
        }
        if (c1.y0 != c2.y0 || c1.y1 != c2.y1) {
            // One might be able to prove that this is impossible...
            return false;
        }
        return true;
    }

    /**
     * Tests whether this {@code Area} is comprised of a single
     * closed subpath.  This method returns {@code true} if the
     * path contains 0 or 1 subpaths, or {@code false} if the path
     * contains more than 1 subpath.  The subpaths are counted by the
     * number of {@link PathIterator#SEG_MOVETO SEG_MOVETO}  segments
     * that appear in the path.
     * @return    {@code true} if the {@code Area} is comprised
     * of a single basic geometry; {@code false} otherwise.
     * @since 1.2
     */
    @Override
    public boolean isSingular() {
        if (curves.elementCount < 3) {
            return true;
        }
        for(int a = 1; a < curves.elementCount; a++) {
            if (curves.elementData[a].order == 0) {
                return false;
            }
        }
        return true;
    }

    private Rectangle2D cachedBounds;
    private void invalidateBounds() {
        cachedBounds = null;
    }
    private Rectangle2D getCachedBounds() {
        if (cachedBounds != null) {
            return cachedBounds;
        }
        Rectangle2D r = new Rectangle2D.Double();
        if (curves.elementCount > 0) {
            QCurve c = curves.elementData[0];
            // First point is always an order 0 curve (moveto)
            r.setRect(c.getX0(), c.getY0(), 0, 0);
            for (int i = 1; i < curves.elementCount; i++) {
                curves.elementData[i].enlarge(r);
            }
        }
        return (cachedBounds = r);
    }

    /**
     * Returns a high precision bounding {@link Rectangle2D} that
     * completely encloses this {@code Area}.
     * <p>
     * The Area class will attempt to return the tightest bounding
     * box possible for the Shape.  The bounding box will not be
     * padded to include the control points of curves in the outline
     * of the Shape, but should tightly fit the actual geometry of
     * the outline itself.
     * @return    the bounding {@code Rectangle2D} for the
     * {@code Area}.
     * @since 1.2
     */
    @Override
    public Rectangle2D getBounds2D() {
        return getCachedBounds().getBounds2D();
    }

    /**
     * Returns a bounding {@link Rectangle} that completely encloses
     * this {@code Area}.
     * <p>
     * The Area class will attempt to return the tightest bounding
     * box possible for the Shape.  The bounding box will not be
     * padded to include the control points of curves in the outline
     * of the Shape, but should tightly fit the actual geometry of
     * the outline itself.  Since the returned object represents
     * the bounding box with integers, the bounding box can only be
     * as tight as the nearest integer coordinates that encompass
     * the geometry of the Shape.
     * @return    the bounding {@code Rectangle} for the
     * {@code Area}.
     * @since 1.2
     */
    @Override
    public Rectangle getBounds() {
        return getCachedBounds().getBounds();
    }

    /**
     * Returns an exact copy of this {@code Area} object.
     * @return    Created clone object
     * @since 1.2
     */
    @Override
    public Object clone() {
        return new QAreaImpl(this);
    }

    /**
     * Tests whether the geometries of the two {@code Area} objects
     * are equal.
     * This method will return false if the argument is null.
     * @param   other  the {@code Area} to be compared to this
     *          {@code Area}
     * @return  {@code true} if the two geometries are equal;
     *          {@code false} otherwise.
     * @since 1.2
     */
    @Override
    public boolean isEqual(QAreaImpl other) {
        // REMIND: A *much* simpler operation should be possible...
        // Should be able to do a curve-wise comparison since all Areas
        // should evaluate their curves in the same top-down order.
        if (other == this) {
            return true;
        }
        if (other == null) {
            return false;
        }
        ExposedArrayWrapper<QCurve> c = new QAreaOp.XorOp().calculate(this.curves, other.curves);
        return c.elementCount == 0;
    }

    /**
     * Transforms the geometry of this {@code Area} using the specified
     * {@link AffineTransform}.  The geometry is transformed in place, which
     * permanently changes the enclosed area defined by this object.
     * @param t  the transformation used to transform the area
     * @throws NullPointerException if {@code t} is null
     * @since 1.2
     */
    @Override
    public void transform(AffineTransform t) {
        if (t == null) {
            throw new NullPointerException("transform must not be null");
        }
        // REMIND: A simpler operation can be performed for some types
        // of transform.
        curves = pathToCurves(getPathIterator(t));
        invalidateBounds();
    }

    /**
     * Creates a new {@code Area} object that contains the same
     * geometry as this {@code Area} transformed by the specified
     * {@code AffineTransform}.  This {@code Area} object
     * is unchanged.
     * @param t  the specified {@code AffineTransform} used to transform
     *           the new {@code Area}
     * @throws NullPointerException if {@code t} is null
     * @return   a new {@code Area} object representing the transformed
     *           geometry.
     * @since 1.2
     */
    public QAreaImpl createTransformedArea(AffineTransform t) {
        QAreaImpl a = new QAreaImpl(this);
        a.transform(t);
        return a;
    }

    /**
     * {@inheritDoc}
     * @since 1.2
     */
    @Override
    public boolean contains(double x, double y) {
        if (!getCachedBounds().contains(x, y)) {
            return false;
        }
        int crossings = 0;
        for(int a = 0; a<curves.elementCount; a++) {
            QCurve c = curves.elementData[a];
            crossings += c.crossingsFor(x, y);
        }
        return ((crossings & 1) == 1);
    }

    /**
     * {@inheritDoc}
     * @since 1.2
     */
    @Override
    public boolean contains(Point2D p) {
        return contains(p.getX(), p.getY());
    }

    /**
     * {@inheritDoc}
     * @since 1.2
     */
    @Override
    public boolean contains(double x, double y, double w, double h) {
        if (w < 0 || h < 0) {
            return false;
        }
        if (!getCachedBounds().contains(x, y, w, h)) {
            return false;
        }
        QCrossings c = QCrossings.findCrossings(curves, x, y, x+w, y+h);
        return (c != null && c.covers(y, y+h));
    }

    /**
     * {@inheritDoc}
     * @since 1.2
     */
    @Override
    public boolean contains(Rectangle2D r) {
        return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    /**
     * {@inheritDoc}
     * @since 1.2
     */
    @Override
    public boolean intersects(double x, double y, double w, double h) {
        if (w < 0 || h < 0) {
            return false;
        }
        if (!getCachedBounds().intersects(x, y, w, h)) {
            return false;
        }
        QCrossings c = QCrossings.findCrossings(curves, x, y, x+w, y+h);
        return (c == null || !c.isEmpty());
    }

    /**
     * {@inheritDoc}
     * @since 1.2
     */
    @Override
    public boolean intersects(Rectangle2D r) {
        return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    /**
     * Creates a {@link PathIterator} for the outline of this
     * {@code Area} object.  This {@code Area} object is unchanged.
     * @param at an optional {@code AffineTransform} to be applied to
     * the coordinates as they are returned in the iteration, or
     * {@code null} if untransformed coordinates are desired
     * @return    the {@code PathIterator} object that returns the
     *          geometry of the outline of this {@code Area}, one
     *          segment at a time.
     * @since 1.2
     */
    @Override
    public PathIterator getPathIterator(AffineTransform at) {
        return new QAreaIterator(curves, at);
    }

    /**
     * Creates a {@code PathIterator} for the flattened outline of
     * this {@code Area} object.  Only uncurved path segments
     * represented by the SEG_MOVETO, SEG_LINETO, and SEG_CLOSE point
     * types are returned by the iterator.  This {@code Area}
     * object is unchanged.
     * @param at an optional {@code AffineTransform} to be
     * applied to the coordinates as they are returned in the
     * iteration, or {@code null} if untransformed coordinates
     * are desired
     * @param flatness the maximum amount that the control points
     * for a given curve can vary from colinear before a subdivided
     * curve is replaced by a straight line connecting the end points
     * @return    the {@code PathIterator} object that returns the
     * geometry of the outline of this {@code Area}, one segment
     * at a time.
     * @since 1.2
     */
    @Override
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return new FlatteningPathIterator(getPathIterator(at), flatness);
    }
}

class QAreaIterator implements PathIterator {
    private AffineTransform transform;
    private ExposedArrayWrapper<QCurve> curves;
    private int index;
    private QCurve prevcurve;
    private QCurve thiscurve;

    public QAreaIterator(ExposedArrayWrapper<QCurve> curves, AffineTransform at) {
        this.curves = curves;
        this.transform = at;
        if (curves.elementCount >= 1) {
            thiscurve = curves.elementData[0];
        }
    }

    @Override
    public int getWindingRule() {
        // REMIND: Which is better, EVEN_ODD or NON_ZERO?
        //         The paths calculated could be classified either way.
        //return WIND_EVEN_ODD;
        return WIND_NON_ZERO;
    }

    @Override
    public boolean isDone() {
        return (prevcurve == null && thiscurve == null);
    }

    @Override
    public void next() {
        if (prevcurve != null) {
            prevcurve = null;
        } else {
            prevcurve = thiscurve;
            index++;
            if (index < curves.elementCount) {
                thiscurve = curves.elementData[index];
                if (thiscurve.order != 0 &&
                        prevcurve.getX1() == thiscurve.getX0() &&
                        prevcurve.getY1() == thiscurve.getY0())
                {
                    prevcurve = null;
                }
            } else {
                thiscurve = null;
            }
        }
    }

    @Override
    public int currentSegment(float[] coords) {
        double[] dcoords = new double[6];
        int segtype = currentSegment(dcoords);
        int numpoints = (segtype == SEG_CLOSE ? 0
                : (segtype == SEG_QUADTO ? 2
                : (segtype == SEG_CUBICTO ? 3
                : 1)));
        for (int i = 0; i < numpoints * 2; i++) {
            coords[i] = (float) dcoords[i];
        }
        return segtype;
    }

    @Override
    public int currentSegment(double[] coords) {
        int segtype;
        int numpoints;
        if (prevcurve != null) {
            // Need to finish off junction between curves
            if (thiscurve == null || thiscurve.order == 0) {
                return SEG_CLOSE;
            }
            coords[0] = thiscurve.getX0();
            coords[1] = thiscurve.getY0();
            segtype = SEG_LINETO;
            numpoints = 1;
        } else if (thiscurve == null) {
            throw new NoSuchElementException("area iterator out of bounds");
        } else {
            segtype = thiscurve.getSegment(coords);
            numpoints = thiscurve.order;
            if (numpoints == 0) {
                numpoints = 1;
            }
        }
        if (transform != null) {
            transform.transform(coords, 0, coords, 0, numpoints);
        }
        return segtype;
    }
}
