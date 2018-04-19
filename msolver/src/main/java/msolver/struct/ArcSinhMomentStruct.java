package msolver.struct;

import org.apache.commons.math3.util.FastMath;

import java.util.Arrays;

public class ArcSinhMomentStruct {
    public double min, max;
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

    public double invert(double x) {
        double xS = x * xr + xc;
        double xVal = FastMath.sinh(xS);
        return xVal;
    }

    @Override
    public String toString() {
        return String.format(
                "%g,%g,%s", min, max, Arrays.toString(powerSums)
        );
    }
}
