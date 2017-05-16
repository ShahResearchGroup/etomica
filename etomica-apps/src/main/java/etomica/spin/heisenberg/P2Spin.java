/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.spin.heisenberg;

import etomica.api.IAtomList;
import etomica.api.IBox;
import etomica.api.IVector;
import etomica.api.IVectorMutable;
import etomica.atom.IAtomOriented;
import etomica.potential.IPotentialAtomicSecondDerivative;
import etomica.potential.IPotentialTorque;
import etomica.potential.Potential2;
import etomica.potential.PotentialSoft;
import etomica.space.ISpace;
import etomica.space.Tensor;
import etomica.space1d.Tensor1D;
import etomica.space1d.Vector1D;
//TODO
/**
 * Magnetic spin potential, with an energy defined by
 * 
 * U = -J r1 dot r2
 * 
 * where J is a coupling parameter, and r1 and r2 are the vectors given by
 * atom.coord.position. It is expected (but not verified here) that these
 * vectors are normalized to unity, and that the simulation integrator's
 * algorithm enforces this constraint.
 * 
 * @author weisong lin and David Kofke
 *  
 */

public class P2Spin extends Potential2 implements IPotentialTorque,IPotentialAtomicSecondDerivative {

    public P2Spin(ISpace space) {
        this(space, 1.0);
    }

    public P2Spin(ISpace space, double coupling) {
        super(space);
        setCoupling(coupling);
        gradient = new IVectorMutable[2];
		gradient[0] = space.makeVector();
		gradient[1] = space.makeVector();
		torque = new IVectorMutable[2];
		torque[0] = new Vector1D();
		torque[1] = new Vector1D();
		secondDerivative = new Tensor[3];
		this.secondDerivative[0] = new Tensor1D();
		this.secondDerivative[1] = new Tensor1D();
		this.secondDerivative[2] = new Tensor1D();
        gradientAndTorque = new IVectorMutable[][]{gradient,torque};
    }
    
    

    
    
    /**
     * Returns the energy for the given pair of atoms.
     * @param atoms
     * @throws ClassCastException if atoms is not an instance of AtomPair
     */

    public double energy(IAtomList atoms) {
    	IAtomOriented atom1 = (IAtomOriented)atoms.getAtom(0);
    	IAtomOriented atom2 = (IAtomOriented)atoms.getAtom(1);
    	return -coupling * atom1.getOrientation().getDirection().dot(atom2.getOrientation().getDirection());
    }

    /**
     * Returns 0, becuase potential operates on a lattice and range
     * should not be needed.  The PotentialMasterSite expects all Potentials
     * to have a range and uses the return value to determine whether or not
     * to use site iteration.
     */
    public double getRange() {
        return 0;
    }

	/**
	 * 	 * @return J the coupling parameter
	 */
	public double getCoupling() {
        return coupling;
    }

	/**
	 * set the coupling parameter J
	 * @param coupling
	 */
	public void setCoupling(double coupling) {
        this.coupling = coupling;
    }

	/**
	 * does nothing
	 * @param box
	 */
	public void setBox(IBox box) {

    }

    private static final long serialVersionUID = 1L;
    private double coupling;
    
    protected IVectorMutable dr;
    private  final IVectorMutable [][] gradientAndTorque;
	private final IVectorMutable[] gradient;
	protected final IVectorMutable[] torque;
	protected final Tensor[] secondDerivative;

	/**
	 * no virial is use here
	 *
	 * @throws Exception when virial is used
	 */
	public double virial(IAtomList atoms) {
		
		throw new RuntimeException("virial is not used in p2Spin");
		
	}

	/**
	 * @param atoms
	 * @return gradient and torque of given pair of atoms
	 */

	public IVector[][] gradientAndTorque(IAtomList atoms) {
		
		IAtomOriented atom1 = (IAtomOriented)atoms.getAtom(0);
    	IAtomOriented atom2 = (IAtomOriented)atoms.getAtom(1);
		
		double x1 = atom1.getOrientation().getDirection().getX(0);//cost1
		double y1 = atom1.getOrientation().getDirection().getX(1);//sint1
		double x2 = atom2.getOrientation().getDirection().getX(0);//cost2
		double y2 = atom2.getOrientation().getDirection().getX(1);//sint2
		
		//sin(t1-t2) = sint1*cost2- cost1*sint2 =y1*x2-x1*y2
		double JSin = coupling*(y1*x2-x1*y2);
		
		torque[0].E(-JSin);
		torque[1].E(JSin);
//		double t1 = Math.acos(x1);
//		System.out.println(" t1= "+t1);
//		System.out.println("x1 = " + x1 + " y1 = " + y1);
//		System.out.println("x2 = " + x2 + " y2 = " + y2);
//		System.out.println(JSin);
//		System.out.println("P2Spin Torque[0] = " + torque[0]);
//		System.out.println("P2Spin Torque[1] = " + torque[1]);
		return gradientAndTorque;
	}

    /**
     * do nothing
     */
	public IVector[][] gradientAndTorque(IAtomList atoms, Tensor pressureTensor) {
		return gradientAndTorque(atoms);
	}

    /**
     * compute the secondDerivative array of pair energy w.r.t theta1 or theta2
     * i.e d^2u/dtheta1_dtheta1 d^2u/dtheta1_dtheta2 and d^2u/dtheta2_dtheta2
     * theta1 is the angle between x axis and atom1's orientation etc.
     * @param atoms given pair of atoms
     * @return   secondDerivative array
     */
	public Tensor[] secondDerivative(IAtomList atoms){
		IAtomOriented atom1 = (IAtomOriented)atoms.getAtom(0);
    	IAtomOriented atom2 = (IAtomOriented)atoms.getAtom(1);
    	double JCos = atom1.getOrientation().getDirection().dot(atom2.getOrientation().getDirection());
    	
    	
    	secondDerivative[0].E(-JCos);
    	secondDerivative[1].E(JCos);
    	secondDerivative[2].E(-JCos);
    	
//    	System.out.println(secondDerivative[0].component(0, 0));
//    	System.out.println(secondDerivative[1].component(0, 0));
//    	System.out.println(secondDerivative[2].component(0, 0));
//    	System.out.println("test for secondDerivative in p2Spin");
		return secondDerivative;
	}

    /**
     * do nothing
     */
	public IVector[] gradient(IAtomList atoms) { 
		throw new RuntimeException("don't need to use gradient");
	}

    /**
     * do nothing
     */

	public IVector[] gradient(IAtomList atoms, Tensor pressureTensor) {
		throw new RuntimeException("don't need to use gradient");
	}
	
	
	
	
}
