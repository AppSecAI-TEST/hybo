/*
 * Copyright 1997-2017 Optimatika (www.optimatika.se)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.algo.matrix.store;

import org.algo.ProgrammingError;
import org.algo.scalar.Scalar;

final class LowerHermitianStore<N extends Number> extends ShadingStore<N> {

    @SuppressWarnings("unused")
    private LowerHermitianStore(final int aRowDim, final int aColDim, final MatrixStore<N> base) {

        this(base);

        ProgrammingError.throwForIllegalInvocation();
    }

    LowerHermitianStore(final MatrixStore<N> base) {
        super(base, (int) base.countRows(), (int) Math.min(base.countRows(), base.countColumns()));
    }

    public double doubleValue(final long row, final long col) {
        if (row < col) {
            return this.getBase().doubleValue(col, row);
        } else {
            return this.getBase().doubleValue(row, col);
        }
    }

    public N get(final long row, final long col) {
        return this.toScalar(row, col).getNumber();
    }

    public Scalar<N> toScalar(final long row, final long col) {
        if (row < col) {
            return this.getBase().toScalar(col, row).conjugate();
        } else {
            return this.getBase().toScalar(row, col);
        }
    }

}
