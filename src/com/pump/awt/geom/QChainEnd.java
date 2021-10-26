/*
 * This is a derivative of sun.awt.geom.ChainEnd. Its original license reads:
 *
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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

final class QChainEnd {
    QCurveLink head;
    QCurveLink tail;
    QChainEnd partner;
    int etag;

    public QChainEnd(QCurveLink first, QChainEnd partner) {
        this.head = first;
        this.tail = first;
        this.partner = partner;
        this.etag = first.etag;
    }

    public QCurveLink getChain() {
        return head;
    }

    public void setOtherEnd(QChainEnd partner) {
        this.partner = partner;
    }

    public QChainEnd getPartner() {
        return partner;
    }

    /*
     * Returns head of a complete chain to be added to subcurves
     * or null if the links did not complete such a chain.
     */
    public QCurveLink linkTo(QChainEnd that) {
        if (etag == QAreaOp.ETAG_IGNORE ||
                that.etag == QAreaOp.ETAG_IGNORE)
        {
            throw new InternalError("ChainEnd linked more than once!");
        }
        if (etag == that.etag) {
            throw new InternalError("Linking chains of the same type!");
        }
        QChainEnd enter, exit;
        // assert(partner.etag != that.partner.etag);
        if (etag == QAreaOp.ETAG_ENTER) {
            enter = this;
            exit = that;
        } else {
            enter = that;
            exit = this;
        }
        // Now make sure these ChainEnds are not linked to any others...
        etag = QAreaOp.ETAG_IGNORE;
        that.etag = QAreaOp.ETAG_IGNORE;
        // Now link everything up...
        enter.tail.next = exit.head;
        enter.tail = exit.tail;
        if (partner == that) {
            // Curve has closed on itself...
            return enter.head;
        }
        // Link this chain into one end of the chain formed by the partners
        QChainEnd otherenter = exit.partner;
        QChainEnd otherexit = enter.partner;
        otherenter.partner = otherexit;
        otherexit.partner = otherenter;
        if (enter.head.getYTop() < otherenter.head.getYTop()) {
            enter.tail.next = otherenter.head;
            otherenter.head = enter.head;
        } else {
            otherexit.tail.next = enter.head;
            otherexit.tail = enter.tail;
        }
        return null;
    }

    public void addLink(QCurveLink newlink) {
        if (etag == QAreaOp.ETAG_ENTER) {
            tail.next = newlink;
            tail = newlink;
        } else {
            newlink.next = head;
            head = newlink;
        }
    }

    public double getX() {
        if (etag == QAreaOp.ETAG_ENTER) {
            return tail.getXBot();
        } else {
            return head.getXBot();
        }
    }
}
