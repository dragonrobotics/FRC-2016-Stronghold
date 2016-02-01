
class KalmanFilter {
    /* Kalman Filter parameters: */
    private double R;
    private double Q;
    
    private double A;
    private double B;
    private double H;
    
    /* Previous outputs: */
    private double prevOut;
    private double prevErrCov;
    
    public double doFilter(double controlInput, double measurement) {
        /* Time Update step: */
        prevOut = (A*prevOut) + (B*controlInput);
        prevErrCov = (A*A*prevErrCov) + Q;
        
        double gain = 1 / (prevErrCov*H*(H*H*prevErrCov + R));
        
        /* Measurement Update step: */
        prevOut = prevOut + (gain*(measurement - (H*prevOut)));
        prevErrCov = (1 - (gain*H))*prevErrCov;
        
        return prevOut;
    }
    
    public KalmanFilter(double a, double b, double h, double r, double q) {
        this.R = r;
        this.Q = q;
        this.A = a;
        this.B = b;
        this.H = h;
        prevOut = 0;
        prevErrCov = 0;
    }
}
