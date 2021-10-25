package com.pump.awt.geom;

import java.awt.*;

/**
 * This factory creates a QArea from a given Shape.
 */
public interface QAreaFactory<T extends QArea> {
    public T create(Shape shape);
}
