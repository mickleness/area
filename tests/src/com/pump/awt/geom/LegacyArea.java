package com.pump.awt.geom;

import java.awt.*;
import java.awt.geom.*;

public class LegacyArea implements QArea<LegacyArea> {

    public static QAreaFactory<LegacyArea> FACTORY = new QAreaFactory<LegacyArea>() {
        @Override
        public LegacyArea create(Shape shape) {
            return new LegacyArea(shape);
        }

        @Override
        public String toString() {
            return "LegacyArea Factory";
        }
    };

    Area delegate;

    public LegacyArea(Shape shape) {
        if (shape == null) {
            delegate = new Area();
        } else {
            delegate = new Area(shape);
        }
    }

    @Override
    public void add(LegacyArea shape) {
        delegate.add(shape.delegate);
    }

    @Override
    public void subtract(LegacyArea shape) {
        delegate.subtract(shape.delegate);
    }

    @Override
    public void exclusiveOr(LegacyArea shape) {
        delegate.exclusiveOr(shape.delegate);
    }

    @Override
    public boolean isEqual(LegacyArea shape) {
        return delegate.equals(shape.delegate);
    }

    @Override
    public void reset() {
        delegate.reset();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean isPolygonal() {
        return delegate.isPolygonal();
    }

    @Override
    public boolean isRectangular() {
        return delegate.isRectangular();
    }

    @Override
    public boolean isSingular() {
        return delegate.isSingular();
    }

    @Override
    public void transform(AffineTransform tx) {
        delegate.transform(tx);
    }

    @Override
    public Rectangle getBounds() {
        return delegate.getBounds();
    }

    @Override
    public Rectangle2D getBounds2D() {
        return delegate.getBounds2D();
    }

    @Override
    public boolean contains(double x, double y) {
        return delegate.contains(x, y);
    }

    @Override
    public boolean contains(Point2D p) {
        return delegate.contains(p);
    }

    @Override
    public boolean intersects(double x, double y, double w, double h) {
        return delegate.intersects(x, y, w, h);
    }

    @Override
    public boolean intersects(Rectangle2D r) {
        return delegate.intersects(r);
    }

    @Override
    public boolean contains(double x, double y, double w, double h) {
        return delegate.contains(x, y, w, h);
    }

    @Override
    public boolean contains(Rectangle2D r) {
        return delegate.contains(r);
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at) {
        return delegate.getPathIterator(at);
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return delegate.getPathIterator(at, flatness);
    }
}
