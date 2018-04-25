package msolver.struct;

import msolver.util.MathUtil;
import org.apache.commons.math3.util.FastMath;

import java.util.Arrays;

public class ArcSinhMomentStruct {
    // arcsinh min, max
    public double min, max;
    // arcsinh power sums
    public double[] powerSums;

    private boolean integral;
    private double xc, xr;

    public ArcSinhMomentStruct(int k) {
        this.min = Double.MAX_VALUE;
        this.max = -Double.MAX_VALUE;
        this.xc = 0;
        this.xr = 1;
        this.powerSums = new double[k];
    }

    public ArcSinhMomentStruct(
            double min, double max, double[] powerSums
    ) {
        this.min = min;
        this.max = max;
        this.xc = (this.min + this.max) / 2;
        this.xr = (this.max - this.min) / 2;
        this.powerSums = powerSums;
    }

    public void add(double[] xs) {
        for (double x : xs) {
            double arcX = FastMath.asinh(x);
            this.min = Math.min(this.min, arcX);
            this.max = Math.max(this.max, arcX);
            for (int i = 0; i < powerSums.length; i++) {
                powerSums[i] += Math.pow(arcX, i);
            }
        }
        this.xc = (this.min + this.max) / 2;
        this.xr = (this.max - this.min) / 2;
    }

    public double convert(double x) {
        double xS = FastMath.asinh(x);
        return (xS - xc) / xr;
    }

    public double invert(double x) {
        double xS = x * xr + xc;
        double xVal = FastMath.sinh(xS);
        return xVal;
    }

    public double[] getChebyMoments() {
        return MathUtil.powerSumsToChebyMoments(min, max, powerSums);
    }

    public double[] getPowerMoments() {
        double[] shiftedSums = MathUtil.shiftPowerSum(powerSums, xr, xc);
        double count = shiftedSums[0];
        for (int i = 0; i < shiftedSums.length; i++) {
            shiftedSums[i] /= count;
        }
        return shiftedSums;
    }

    @Override
    public String toString() {
        return String.format(
                "%g,%g,%s", min, max, Arrays.toString(powerSums)
        );
    }
}
