package net.darkwyvbat.dwbcore.util;

public class MathU {

    public static final float PI = 3.1415927F;
    public static final float HALF_PI = PI / 2;
    public static final float TWO_PI = PI * 2;
    public static final float FOUR_PI = PI * 4;
    public static final float SQR_PI = PI * PI;

    private static final float FS_L = 4.0F / PI;
    private static final float FS_Q = -4.0F / (PI * PI);


    public static float wrapDeg(float deg) {
        deg %= 360.0F;
        return deg < 0 ? deg + 360.0F : deg;
    }

    public static float wrapRad(float rad) {
        rad %= TWO_PI;
        return rad < 0 ? rad + TWO_PI : rad;
    }

    public static float norm180(float deg) {
        deg %= 360.0F;
        if (deg > 180.0F) deg -= 360.0F;
        else if (deg < -180.0F) deg += 360.0F;
        return deg;
    }

    public static float normPI(float rad) {
        rad %= TWO_PI;
        if (rad > PI) rad -= TWO_PI;
        else if (rad < -PI) rad += TWO_PI;
        return rad;
    }

    // b * x + c * x * absX
    public static float fSin(float x) {
        x = normPI(x);
        return FS_L * x + x * FS_Q * (x < 0 ? -x : x);
    }

    public static float fCos(float x) {
        return fSin(x + HALF_PI);
    }

    public static float poorSin(float x) {
        x = norm180(x);
        return x * (180.0F - (x < 0 ? -x : x)) * 0.00012345679F;
    }

    public static float poorCos(float x) {
        return poorSin(x + 90);
    }

    public static boolean eqWithin(double a, double b, double e) {
        return Math.abs(a - b) < e;
    }

    public static boolean isBtwn(double v, double l, double h) {
        return v > l && v < h;
    }

    public static boolean isBtwnIncl(double v, double l, double h) {
        return v >= l && v <= h;
    }
}