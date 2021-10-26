/*
 * This is a derivative of sun.awt.geom.Edge. Its original license reads:
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


final class QEdge implements Comparable<QEdge> {
    static final int INIT_PARTS = 4;
    static final int GROW_PARTS = 10;

    public final QCurve curve;
    public final int curveTag;
    public int edgeTag;
    double activey;
    int equivalence;

    public QEdge(QCurve c, int curveTag) {
        this(c, curveTag, QAreaOp.ETAG_IGNORE);
    }

    public QEdge(QCurve c, int curveTag, int edgeTag) {
        this.curve = c;
        this.curveTag = curveTag;
        this.edgeTag = edgeTag;
    }

    public int getEquivalence() {
        return equivalence;
    }

    public void setEquivalence(int eq) {
        equivalence = eq;
    }

    private QEdge lastEdge;
    private int lastResult;
    private double lastLimit;

    public int compareTo(QEdge other, double[] yrange) {
        if (other == lastEdge && yrange[0] < lastLimit) {
            if (yrange[1] > lastLimit) {
                yrange[1] = lastLimit;
            }
            return lastResult;
        }
        if (this == other.lastEdge && yrange[0] < other.lastLimit) {
            if (yrange[1] > other.lastLimit) {
                yrange[1] = other.lastLimit;
            }
            return 0-other.lastResult;
        }
        //long start = System.currentTimeMillis();
        int ret = curve.compareTo(other.curve, yrange);
        //long end = System.currentTimeMillis();
        /*
        System.out.println("compare: "+
                           ((System.identityHashCode(this) <
                             System.identityHashCode(other))
                            ? this+" to "+other
                            : other+" to "+this)+
                           " == "+ret+" at "+yrange[1]+
                           " in "+(end-start)+"ms");
         */
        lastEdge = other;
        lastLimit = yrange[1];
        lastResult = ret;
        return ret;
    }

    public void record(double yend, int edgeTag) {
        this.activey = yend;
        this.edgeTag = edgeTag;
    }

    public boolean isActiveFor(double y, int edgeTag) {
        return (this.edgeTag == edgeTag && this.activey >= y);
    }

    public String toString() {
        return ("QEdge["+curve+
                ", "+
                (curveTag == QAreaOp.CTAG_LEFT ? "L" : "R")+
                ", "+
                (edgeTag == QAreaOp.ETAG_ENTER ? "I" :
                        (edgeTag == QAreaOp.ETAG_EXIT ? "O" : "N"))+
                "]");
    }

    @Override
    public int compareTo(QEdge o2) {
        QCurve c2 = o2.curve;
        double v1, v2;
        if ((v1 = curve.y0) == (v2 = c2.y0)) {
            if ((v1 = curve.x0) == (v2 = c2.x0)) {
                return 0;
            }
        }
        if (v1 < v2) {
            return -1;
        }
        return 1;
    }
}
