package com.umss.estudiomercado.estadistica;

/** Distribuciones chi-cuadrado, t de Student y F implementadas sin dependencias externas. */
public final class Distribuciones {
    private Distribuciones() {}

    private static final double EPS = 1e-14;
    private static final double FPMIN = 1e-300;

    public static double lnGamma(double x) {
        double[] c = {76.18009172947146, -86.50532032941677, 24.01409824083091,
                -1.231739572450155, 0.1208650973866179e-2, -0.5395239384953e-5};
        double y = x;
        double tmp = x + 5.5;
        tmp -= (x + 0.5) * Math.log(tmp);
        double ser = 1.000000000190015;
        for (double cj : c) ser += cj / ++y;
        return -tmp + Math.log(2.5066282746310005 * ser / x);
    }

    /** {P, Q} de la gamma incompleta regularizada. */
    private static double[] gammaPQ(double a, double x) {
        if (x <= 0) return new double[]{0, 1};
        double lnPre = -x + a * Math.log(x) - lnGamma(a);
        if (x < a + 1) {
            double ap = a, sum = 1.0 / a, del = sum;
            for (int n = 0; n < 2000; n++) {
                ap++;
                del *= x / ap;
                sum += del;
                if (Math.abs(del) < Math.abs(sum) * EPS) break;
            }
            double p = sum * Math.exp(lnPre);
            return new double[]{p, 1 - p};
        }
        double b = x + 1 - a, c = 1.0 / FPMIN, d = 1.0 / b, h = d;
        for (int i = 1; i < 2000; i++) {
            double an = -i * (i - a);
            b += 2;
            d = an * d + b;
            if (Math.abs(d) < FPMIN) d = FPMIN;
            c = b + an / c;
            if (Math.abs(c) < FPMIN) c = FPMIN;
            d = 1.0 / d;
            double del = d * c;
            h *= del;
            if (Math.abs(del - 1) < EPS) break;
        }
        double q = Math.exp(lnPre) * h;
        return new double[]{1 - q, q};
    }

    public static double chi2Cdf(double x, int gl) { return gammaPQ(gl / 2.0, x / 2.0)[0]; }

    /** P(X >= x): valor p de la prueba chi-cuadrado. */
    public static double chi2Sf(double x, int gl) { return gammaPQ(gl / 2.0, x / 2.0)[1]; }

    /** Valor crítico: x tal que P(X <= x) = p. */
    public static double chi2Inv(double p, int gl) {
        double lo = 0, hi = Math.max(10, gl * 10.0);
        while (chi2Cdf(hi, gl) < p && hi < 1e7) hi *= 2;
        for (int i = 0; i < 300; i++) {
            double mid = (lo + hi) / 2;
            if (chi2Cdf(mid, gl) < p) lo = mid; else hi = mid;
        }
        return (lo + hi) / 2;
    }

    private static double betaCf(double a, double b, double x) {
        double qab = a + b, qap = a + 1, qam = a - 1, c = 1, d = 1 - qab * x / qap;
        if (Math.abs(d) < FPMIN) d = FPMIN;
        d = 1 / d;
        double h = d;
        for (int m = 1; m <= 500; m++) {
            int m2 = 2 * m;
            double aa = m * (b - m) * x / ((qam + m2) * (a + m2));
            d = 1 + aa * d;
            if (Math.abs(d) < FPMIN) d = FPMIN;
            c = 1 + aa / c;
            if (Math.abs(c) < FPMIN) c = FPMIN;
            d = 1 / d;
            h *= d * c;
            aa = -(a + m) * (qab + m) * x / ((a + m2) * (qap + m2));
            d = 1 + aa * d;
            if (Math.abs(d) < FPMIN) d = FPMIN;
            c = 1 + aa / c;
            if (Math.abs(c) < FPMIN) c = FPMIN;
            d = 1 / d;
            double del = d * c;
            h *= del;
            if (Math.abs(del - 1) < EPS) break;
        }
        return h;
    }

    /** Beta incompleta regularizada I_x(a, b). */
    public static double betaI(double a, double b, double x) {
        if (x <= 0) return 0;
        if (x >= 1) return 1;
        double bt = Math.exp(lnGamma(a + b) - lnGamma(a) - lnGamma(b) + a * Math.log(x) + b * Math.log(1 - x));
        if (x < (a + 1) / (a + b + 2)) return bt * betaCf(a, b, x) / a;
        return 1 - bt * betaCf(b, a, 1 - x) / b;
    }

    public static double tCdf(double t, double gl) {
        double p = 0.5 * betaI(gl / 2.0, 0.5, gl / (gl + t * t));
        return t > 0 ? 1 - p : p;
    }

    /** Valor p bilateral de un estadístico t. */
    public static double tDosColas(double t, double gl) { return betaI(gl / 2.0, 0.5, gl / (gl + t * t)); }

    /** Cuantil t: x tal que P(T <= x) = p. */
    public static double tInv(double p, double gl) {
        double lo = -1e5, hi = 1e5;
        for (int i = 0; i < 300; i++) {
            double mid = (lo + hi) / 2;
            if (tCdf(mid, gl) < p) lo = mid; else hi = mid;
        }
        return (lo + hi) / 2;
    }

    /** P(F >= f) para una F de Fisher con (d1, d2) grados de libertad. */
    public static double fSf(double f, double d1, double d2) {
        if (f <= 0) return 1;
        return betaI(d2 / 2.0, d1 / 2.0, d2 / (d2 + d1 * f));
    }
}
