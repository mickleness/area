package com.pump.awt.geom;

import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * This interface is functionally similar to the java.awt.geom.Area class.
 */
public interface QArea<T> extends Shape {
    void add(T shape);
    void subtract(T shape);
    void exclusiveOr(T shape);
    void intersect(T shape);
    boolean isEqual(T shape);
    T cloneArea();
    void reset();
    boolean isEmpty();
    boolean isPolygonal();
    boolean isRectangular();
    boolean isSingular();
    void transform(AffineTransform tx);
}