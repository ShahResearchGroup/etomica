package etomica.util.numerical;

import Jama.Matrix;

/**
 * 
 * Class to approximate a power series with rational function (Pade Approximation)
 * 
 * 
 *  Pade[K/L] = A_K/ C_L = B_M
 *   with Kth, Lth and Mth order, where K + L = M  
 *  
 *  a0 + a1*x + a2*x^2 + ... + aK*x^K
 *  ----------------------------------  = b0 + b1*x + b2*x^2 + b3*x^3 + ... + b(K+L)*x^(K+L) [or bM*x^M]      
 *  c0 + c1*x + c2*x^2 + ... + cL*x^L
 * 
 *  - c0 is set to be 1
 * 
 * 
 * @author Tai Boon Tan
 *
 */
public class PadeApproximation {
	public PadeApproximation(double[] b, int K, int L){
		if((K+L) > b.length){
			throw new RuntimeException("K plus L should not exceed the M-order");
		}
		
		this.b = b;
		a = new double[K+1];
		c = new double[L+1];
		
		double[][] y = new double[L][L];
		double[] z = new double [L];
		
		for (int icol=0; icol<L; icol++){
			z[icol] = -b[K+1+icol];
			System.out.println("z["+icol+"]: " + z[icol]);
		}
		
		for (int irow=0; irow<L; irow++){
			for (int icol=0; icol<L; icol++ ){
				y[irow][icol] = b[K+irow-icol]; 
				
			}
		}
		
		Y = new Matrix(y);
		System.out.println("yMAtrix Col: " + Y.getColumnDimension());
		System.out.println("yMAtrix Row: " + Y.getRowDimension());
		Z = new Matrix(z, z.length);
		System.out.println("zMAtrix Col: " + Z.getColumnDimension());
		System.out.println("zMAtrix Row: " + Z.getRowDimension());
	}
	
	public void solveCoefficients(){
		
		Matrix cMatrix = Y.solve(Z);
		System.out.println("cMAtrix Col: " + cMatrix.getColumnDimension());
		System.out.println("cMAtrix Row: " + cMatrix.getRowDimension());
		
		/*
		 * determine "c" coefficients
		 */
		c[0] = 1.0;
		for (int i=0; i<c.length-1; i++){
			c[i+1] = cMatrix.get(i, 0);
		}
		
		/*
		 * determine "a" coefficients
		 */
		for (int i=0; i<a.length; i++){
			for (int j=i; j>=0; j--){
				a[i] += b[j]*c[i-j];
			}
		}
		
	}
	
	public double[] getA(){
		return a;
	}
	
	public double[] getC(){
		return c;
	}
	
	public static void main(String[] args){
		
		double[] b = new double []{1.0, 4.0, 4.0, 5.0, 6.0};
		int K = 2;
		int L = 2;
		PadeApproximation pade = new PadeApproximation(b, K, L);
		pade.solveCoefficients();
		
		double[] aValues = pade.getA();
		double[] cValues = pade.getC();
		
		for (int i=0; i<aValues.length; i++){
			System.out.println("a["+i+"]: " + aValues[i]);
		}
		for (int i=0; i<cValues.length; i++){
			System.out.println("c["+i+"]: " + cValues[i]);
		}
		
	}
	
	protected double[] a, b, c;
	protected Matrix Y, Z;
}