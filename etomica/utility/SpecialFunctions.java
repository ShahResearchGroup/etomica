package etomica.utility;

/**
 * Static-method library of various functions
 */
 
public final class SpecialFunctions {
    
    private SpecialFunctions() {}
    
    
    /**
     * Complementary error function, computed using the approximant 7.1.26 of Abramowitz & Stegun.
     * Defined for x >= 0
     */
    public static double erfc(double x) {
        double t = 1.0/(1.0 + 0.3275911*x);
        return (t * (
                  0.254829592 + t * (
                     -0.284496736 + t * (
                       1.421413741 +  t * (
                         - 1.453152027 + 1.061405429*t)))))*Math.exp(-x*x);
    }
    
    public static int factorial(int i){
        if(i < 0){
            throw new IllegalArgumentException("Argument less than zero: "+i);
        }
        else{
            return (i <= 1) ? 1 :(i*factorial(i-1));
        }
    }
    
    //non-recursive version
//	public static int factorial(int n) {
//		if(n < 0) throw new IllegalArgumentException("Illegal to pass negative value to factorial");
//		int factorial = 1;
//		for (int i=2; i<=n; i++) {
//		   factorial *= i;
//		}
//		return factorial;	
//	}
	
   
}