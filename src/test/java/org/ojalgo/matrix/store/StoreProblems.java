/*
 * Copyright 1997-2021 Optimatika
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
package org.ojalgo.matrix.store;

import org.junit.jupiter.api.Test;
import org.ojalgo.OjAlgoUtils;
import org.ojalgo.TestUtils;
import org.ojalgo.function.aggregator.Aggregator;
import org.ojalgo.function.constant.PrimitiveMath;
import org.ojalgo.function.special.MissingMath;
import org.ojalgo.matrix.Primitive64Matrix;
import org.ojalgo.matrix.decomposition.QR;
import org.ojalgo.matrix.store.PhysicalStore.Factory;
import org.ojalgo.netio.BasicLogger;
import org.ojalgo.type.context.NumberContext;

public class StoreProblems extends MatrixStoreTests {

    static class EyeStore implements MatrixStore<Double> {

        private final long myCountRows, myCountColumns;

        EyeStore(final long countRows, final long countColumns) {
            super();
            myCountRows = countRows;
            myCountColumns = countColumns;
        }

        public long countColumns() {
            return myCountColumns;
        }

        public long countRows() {
            return myCountRows;
        }

        public double doubleValue(final long row, final long col) {
            return row == col ? 1D : 0D;
        }

        public int firstInColumn(final int col) {
            return col;
        }

        public int firstInRow(final int row) {
            return row;
        }

        public Double get(final long row, final long col) {
            return this.doubleValue(row, col);
        }

        public int limitOfColumn(final int col) {
            return col + 1;
        }

        public int limitOfRow(final int row) {
            return row + 1;
        }

        public org.ojalgo.matrix.store.PhysicalStore.Factory<Double, ?> physical() {
            return Primitive64Store.FACTORY;
        }

        public void supplyTo(final TransformableRegion<Double> receiver) {
            receiver.reset();
            receiver.fillDiagonal(1D);
        }

    }

    /**
     * https://github.com/optimatika/ojAlgo/issues/252 <br>
     * <br>
     * Test that the multiplication logic works when there are more elements that an int can count.
     */
    @Test
    public void testGitHubIssue252() {

        //int c = Integer.MAX_VALUE - 2;
        int c = 50_000;

        for (int m = 1; m < 10; m++) {
            for (int n = 1; n < 10; n++) {
                int min = MissingMath.min(m, c, n);

                MatrixStore<Double> left = new EyeStore(m, c);
                MatrixStore<Double> right = new EyeStore(c, n);

                MatrixStore<Double> denseProduct = left.multiply(right);

                TestUtils.assertEquals(m, denseProduct.countRows());
                TestUtils.assertEquals(n, denseProduct.countColumns());

                TestUtils.assertEquals(min, denseProduct.aggregateAll(Aggregator.SUM).intValue());

                SparseStore<Double> sparseProduct = SparseStore.PRIMITIVE64.make(m, n);
                sparseProduct.fillByMultiplying(left, right);

                TestUtils.assertEquals(m, sparseProduct.countRows());
                TestUtils.assertEquals(n, sparseProduct.countColumns());

                TestUtils.assertEquals(min, sparseProduct.aggregateAll(Aggregator.SUM).intValue());
            }
        }
    }

    /**
     * Problem with LogicalStore and multi-threading. The MinBatchSize#EXECUTOR was designed to have a fixed
     * number of threads which doesn't work if nested LogicalStores require more. The program would hang. This
     * test makes sure ojAlgo no longer hangs in such a case.
     */
    @Test
    public void testP20071210() {

        Primitive64Matrix A, Bu, K, sx, currentState;

        final double[][] a = { { 1, 2 }, { 3, 4 } };
        A = Primitive64Matrix.FACTORY.rows(a);
        final double[][] bu = { { 1, 0 }, { 0, 1 } };
        Bu = Primitive64Matrix.FACTORY.rows(bu);
        Primitive64Matrix.FACTORY.makeEye(2, 2);
        K = Primitive64Matrix.FACTORY.makeEye(2, 2);
        final int hp = 2 * OjAlgoUtils.ENVIRONMENT.threads;

        final Primitive64Matrix eye = Primitive64Matrix.FACTORY.makeEye(A);
        final Primitive64Matrix Aprime = A.subtract(Bu.multiply(K));
        Primitive64Matrix Apow = Primitive64Matrix.FACTORY.copy(Aprime);
        final Primitive64Matrix tmp = Aprime.subtract(eye);
        sx = Primitive64Matrix.FACTORY.copy(eye);
        sx = sx.logical().below(tmp).get();

        //loop runs hp-2 times, which means the first elements of the matrices must be "hardcoded"
        for (int i = 0; i < (hp - 2); i++) {
            sx = sx.logical().below(tmp.multiply(Apow)).get();
            Apow = Apow.multiply(Apow);
        }
        currentState = Primitive64Matrix.FACTORY.makeZero(A.countRows(), 1);
        currentState = currentState.add(1.0);
        sx.multiply(currentState);
    }

    /**
     * Peter Abeles reported a problem with ojAlgo his benchmark's C=A*BT test. The problem turned out be that
     * fillByMultiplying did not reset the destination matrix elements when doung "multiply right".
     */
    @Test
    public void testP20110223() {

        final int tmpDim = 9;

        final PhysicalStore<Double> tmpMtrxA = Primitive64Store.FACTORY.copy(TestUtils.makeRandomComplexStore(tmpDim, tmpDim));
        final PhysicalStore<Double> tmpMtrxB = Primitive64Store.FACTORY.copy(TestUtils.makeRandomComplexStore(tmpDim, tmpDim));
        final PhysicalStore<Double> tmpMtrxC = Primitive64Store.FACTORY.makeZero(tmpDim, tmpDim);

        PhysicalStore<Double> tmpExpected;
        PhysicalStore<Double> tmpActual;

        tmpMtrxC.fillByMultiplying(tmpMtrxA, tmpMtrxB);
        tmpExpected = tmpMtrxC.copy();
        tmpMtrxC.fillByMultiplying(tmpMtrxA, tmpMtrxB);
        tmpActual = tmpMtrxC.copy();
        TestUtils.assertEquals(tmpExpected, tmpActual, new NumberContext(7, 6));

        tmpMtrxC.fillByMultiplying(tmpMtrxA, tmpMtrxB.logical().transpose().get());
        tmpExpected = tmpMtrxC.copy();
        tmpMtrxC.fillByMultiplying(tmpMtrxA, tmpMtrxB.logical().transpose().get());
        tmpActual = tmpMtrxC.copy();
        TestUtils.assertEquals(tmpExpected, tmpActual, new NumberContext(7, 6));

        tmpMtrxC.fillByMultiplying(tmpMtrxA.logical().transpose().get(), tmpMtrxB);
        tmpExpected = tmpMtrxC.copy();
        tmpMtrxC.fillByMultiplying(tmpMtrxA.logical().transpose().get(), tmpMtrxB);
        tmpActual = tmpMtrxC.copy();
        TestUtils.assertEquals(tmpExpected, tmpActual, new NumberContext(7, 6));
    }

    /**
     * https://github.com/optimatika/ojAlgo/issues/330
     */
    @Test
    public void testGitHubIssue330() {

        double[][] data = { { 1.0, 2.0, 3.0, 10.0 }, { 4.0, 5.0, 6.0, 11.0 }, { 7.0, 8.0, 9.0, 11.0 } };

        PhysicalStore.Factory<Double, Primitive64Store> storeFactory = Primitive64Store.FACTORY;
        Primitive64Store m = storeFactory.rows(data);
        Primitive64Store r = storeFactory.make(m.countRows(), m.countColumns());

        QR<Double> qr = QR.PRIMITIVE.make(true);
        qr.decompose(m);

        if (DEBUG) {
            BasicLogger.debug("Original", m);
            BasicLogger.debug("Q", qr.getQ());
            BasicLogger.debug("R", qr.getR());
            BasicLogger.debug("Receiver (before)", r);
        }

        // java.lang.ArrayIndexOutOfBoundsException was thrown here
        qr.getR().supplyTo(r);

        if (DEBUG) {
            BasicLogger.debug("Receiver (after)", r);
        }

        TestUtils.assertEquals(qr.getR(), r);
    }

    /**
     * https://github.com/optimatika/ojAlgo/issues/55
     */
    @Test
    public void testP20180121() {

        final SparseStore<Double> m = SparseStore.PRIMITIVE64.make(3, 2);
        final Primitive64Store mAdd = Primitive64Store.FACTORY.rows(new double[][] { { 1.0, 0.0 }, { 0.0, 0.0 }, { 0.0, 0.0 } });
        final MatrixStore<Double> n = m.add(mAdd);

        final SparseStore<Double> eye = SparseStore.PRIMITIVE64.make(2, 2);
        eye.set(0, 0, 1.0);
        eye.set(1, 1, 1.0);

        final MatrixStore<Double> prod = n.multiply(eye);

        TestUtils.assertEquals(mAdd, prod);

        final SparseStore<Double> m2 = SparseStore.PRIMITIVE64.make(3, 2);
        m2.set(0, 0, 1.0);

        TestUtils.assertEquals(mAdd, m2.multiply(eye));
        TestUtils.assertEquals(mAdd, eye.premultiply(m2).collect(Primitive64Store.FACTORY));

        final SparseStore<Double> a = SparseStore.PRIMITIVE64.make(3, 3);
        a.set(1, 1, 1.0);

        final SparseStore<Double> b = SparseStore.PRIMITIVE64.make(3, 5);
        b.set(1, 1, 1.0);
        b.set(0, 3, 1.0);

        final SparseStore<Double> c = SparseStore.PRIMITIVE64.make(3, 5);
        c.set(1, 1, 1.0);

        if (DEBUG) {
            BasicLogger.debug("A", a);
            BasicLogger.debug("B", b);
            BasicLogger.debug("C", c);
        }

        TestUtils.assertEquals(c, a.multiply(b));
    }

    /**
     * https://github.com/optimatika/ojAlgo/issues/133
     */
    @Test
    public void testTransposeElementsSupplier() {

        double[][] _x = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
        double[][] _y = { { 0, 0, 0 }, { 1, 1, 1 }, { 2, 2, 2 } };
        double[][] exp = { { 1.0, 2.0, 3.0 }, { 3.0, 4.0, 5.0 }, { 5.0, 6.0, 7.0 } };

        Factory<Double, Primitive64Store> factory = Primitive64Store.FACTORY;

        Primitive64Store x = factory.rows(_x);
        Primitive64Store y = factory.rows(_y);

        ElementsSupplier<Double> diff = y.onMatching(x, PrimitiveMath.SUBTRACT);
        ElementsSupplier<Double> transp = diff.transpose();

        TestUtils.assertEquals(factory.rows(exp), diff.collect(factory));
        TestUtils.assertEquals(factory.columns(exp), transp.collect(factory));
    }

}
