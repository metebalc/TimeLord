package com.timelord.mih;

import java.util.Arrays;

/**
 * Monotone piecewise cubic Hermite interpolation using PCHIP slopes.
 */
final class MonotoneCubicCurve {
    private final double[] x;
    private final double[] y;
    private final double[] slopes;

    MonotoneCubicCurve(double[] x, double[] y) {
        if (x.length != y.length || x.length < 2)
            throw new IllegalArgumentException("A curve requires matching arrays with at least two points");

        this.x = Arrays.copyOf(x, x.length);
        this.y = Arrays.copyOf(y, y.length);
        this.slopes = calculateSlopes(this.x, this.y);
    }

    double value(double input) {
        if (input <= x[0])
            return y[0];
        if (input >= x[x.length - 1])
            return y[y.length - 1];

        int upper = Arrays.binarySearch(x, input);
        if (upper >= 0)
            return y[upper];

        upper = -upper - 1;
        int lower = upper - 1;
        double width = x[upper] - x[lower];
        double t = (input - x[lower]) / width;
        double t2 = t * t;
        double t3 = t2 * t;

        double h00 = 2.0D * t3 - 3.0D * t2 + 1.0D;
        double h10 = t3 - 2.0D * t2 + t;
        double h01 = -2.0D * t3 + 3.0D * t2;
        double h11 = t3 - t2;

        return h00 * y[lower]
                + h10 * width * slopes[lower]
                + h01 * y[upper]
                + h11 * width * slopes[upper];
    }

    private static double[] calculateSlopes(double[] x, double[] y) {
        int pointCount = x.length;
        double[] widths = new double[pointCount - 1];
        double[] secants = new double[pointCount - 1];

        for (int i = 0; i < pointCount - 1; i++) {
            widths[i] = x[i + 1] - x[i];
            if (widths[i] <= 0.0D)
                throw new IllegalArgumentException("Curve inputs must be strictly increasing");
            secants[i] = (y[i + 1] - y[i]) / widths[i];
        }

        double[] result = new double[pointCount];
        if (pointCount == 2) {
            result[0] = secants[0];
            result[1] = secants[0];
            return result;
        }

        result[0] = endpointSlope(widths[0], widths[1], secants[0], secants[1]);
        result[pointCount - 1] = endpointSlope(
                widths[pointCount - 2],
                widths[pointCount - 3],
                secants[pointCount - 2],
                secants[pointCount - 3]
        );

        for (int i = 1; i < pointCount - 1; i++) {
            double before = secants[i - 1];
            double after = secants[i];
            if (before == 0.0D || after == 0.0D || Math.signum(before) != Math.signum(after)) {
                result[i] = 0.0D;
                continue;
            }

            double beforeWidth = widths[i - 1];
            double afterWidth = widths[i];
            double firstWeight = 2.0D * afterWidth + beforeWidth;
            double secondWeight = afterWidth + 2.0D * beforeWidth;
            result[i] = (firstWeight + secondWeight)
                    / (firstWeight / before + secondWeight / after);
        }

        return result;
    }

    private static double endpointSlope(
            double adjacentWidth,
            double nextWidth,
            double adjacentSecant,
            double nextSecant
    ) {
        double slope = ((2.0D * adjacentWidth + nextWidth) * adjacentSecant
                - adjacentWidth * nextSecant) / (adjacentWidth + nextWidth);

        if (Math.signum(slope) != Math.signum(adjacentSecant))
            return 0.0D;

        if (Math.signum(adjacentSecant) != Math.signum(nextSecant)
                && Math.abs(slope) > Math.abs(3.0D * adjacentSecant))
            return 3.0D * adjacentSecant;

        return slope;
    }
}
