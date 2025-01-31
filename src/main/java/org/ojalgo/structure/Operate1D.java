package org.ojalgo.structure;

import org.ojalgo.function.BinaryFunction;
import org.ojalgo.function.ParameterFunction;
import org.ojalgo.function.UnaryFunction;

public interface Operate1D<N extends Comparable<N>, P extends Operate1D<N, P>> {

    default P onAll(final BinaryFunction<N> operator, final double right) {
        return this.onAll(operator.second(right));
    }

    default P onAll(final BinaryFunction<N> operator, final N right) {
        return this.onAll(operator.second(right));
    }

    default P onAll(final double left, final BinaryFunction<N> operator) {
        return this.onAll(operator.first(left));
    }

    default P onAll(final N left, final BinaryFunction<N> operator) {
        return this.onAll(operator.first(left));
    }

    default P onAll(final ParameterFunction<N> operator, final int parameter) {
        return this.onAll(operator.parameter(parameter));
    }

    P onAll(UnaryFunction<N> operator);

    P onAny(Transformation1D<N> operator);

    P onMatching(Access1D<N> left, BinaryFunction<N> operator);

    P onMatching(BinaryFunction<N> operator, Access1D<N> right);

    /**
     * @deprecated v49
     */
    @Deprecated
    default P operateOnAll(final BinaryFunction<N> operator, final double right) {
        return this.onAll(operator, right);
    }

    /**
     * @deprecated v49
     */
    @Deprecated
    default P operateOnAll(final BinaryFunction<N> operator, final N right) {
        return this.onAll(operator, right);
    }

    /**
     * @deprecated v49
     */
    @Deprecated
    default P operateOnAll(final double left, final BinaryFunction<N> operator) {
        return this.onAll(left, operator);
    }

    /**
     * @deprecated v49
     */
    @Deprecated
    default P operateOnAll(final N left, final BinaryFunction<N> operator) {
        return this.onAll(left, operator);
    }

    /**
     * @deprecated v49
     */
    @Deprecated
    default P operateOnAll(final ParameterFunction<N> operator, final int parameter) {
        return this.onAll(operator, parameter);
    }

    /**
     * @deprecated v49
     */
    @Deprecated
    default P operateOnAll(final UnaryFunction<N> operator) {
        return this.onAll(operator);
    }

    /**
     * @deprecated v49
     */
    @Deprecated
    default P operateOnAny(final Transformation1D<N> operator) {
        return this.onAny(operator);
    }

    /**
     * @deprecated v49
     */
    @Deprecated
    default P operateOnMatching(final Access1D<N> left, final BinaryFunction<N> operator) {
        return this.onMatching(left, operator);
    }

    /**
     * @deprecated v49
     */
    @Deprecated
    default P operateOnMatching(final BinaryFunction<N> operator, final Access1D<N> right) {
        return this.onMatching(operator, right);
    }

}
