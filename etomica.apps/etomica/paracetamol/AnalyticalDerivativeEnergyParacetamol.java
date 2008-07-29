package etomica.paracetamol;

import java.io.Serializable;

import etomica.api.IAtomLeaf;
import etomica.api.IAtomSet;
import etomica.api.IAtomPositioned;
import etomica.api.IBox;
import etomica.api.IMolecule;
import etomica.api.IVector;
import etomica.api.IVector3D;

import etomica.conjugategradient.DerivativeEnergyFunction;
import etomica.integrator.IntegratorVelocityVerlet;
import etomica.potential.PotentialMaster;
import etomica.space.ISpace;

public class AnalyticalDerivativeEnergyParacetamol extends DerivativeEnergyFunction implements Serializable{
	
	public AnalyticalDerivativeEnergyParacetamol(IBox box, PotentialMaster potentialMaster, ISpace space){
		super(box, potentialMaster, space);
		rotationAxis = (IVector3D)space.makeVector();
		a      = (IVector3D)space.makeVector();
		aProj  = (IVector3D)space.makeVector();
		v      = (IVector3D)space.makeVector();
		deltaV = (IVector3D)space.makeVector();
		distance = new IVector3D[20];
		torque   = new IVector3D[20];
		torqueF  = new IVector3D[20];
		for (int i=0; i<20; i++){
			distance[i] = (IVector3D)space.makeVector();
			torque  [i] = (IVector3D)space.makeVector();
			torqueF [i] = (IVector3D)space.makeVector();
		}
		torqueSum = space.makeVector();
		
	}
	
	public double df(int[] d, double[] u){ 
		
		/*
		 * u is the generalized coordinate
		 */
		
		/*
		 * d only takes in array that compute first-order derivative w.r.t. to corresponding n-th dimension
		 *  for example, d=new double{1, 0, 0} or {0, 0, 1}, which means first-order differentiation to 
		 *  first- and third- dimension respectively. 
		 */
		
		int index =0;
		double check =0;
		fPrimeRotation = new double[u.length];
		
		for (int i =0; i <d.length; i++){
			check += d[i];
			
			if (d[i]==1){
				index = i;
			}
		} 
		
		if (check != 1){
			throw new IllegalArgumentException("The function MUST and CAN only compute first-order derivative!!");
		}
		
		int group = index;
		
		/*
		 * return the first-derivative of translation mode
		 */
		if(group%6 < 3){
			
			fPrimeRotation[index] = super.df(d, u);
			return fPrimeRotation[index];
			
		} else {
			
			forceSum.reset();

			for (int cell=0; cell<coordinateDefinition.getBasisCells().length; cell++){
				IAtomSet molecules = coordinateDefinition.getBasisCells()[cell].molecules;
				coordinateDefinition.setToU(molecules, u);
			}
			
			/*
			 *  fPrime[coordinateDim] 
			 * 	where we have 6 generalized coordinates: 3 modes on translation and 3 on rotation for each molecule
			 *  
			 */
			IAtomSet molecules = coordinateDefinition.getBasisCells()[0].molecules;
			
			int j=3;
			
			for (int p=0; p<molecules.getAtomCount(); p++){ //loop over the 8 molecules in the basis cell
				
				IAtomSet molecule = ((IMolecule)molecules.getAtom(p)).getChildList();
			
				 //leafPos0 is atom C1 in Paracetamol
				 //leafPos5 is atom C4 in Paracetamol
				IVector leafPos0 = ((IAtomPositioned)molecule.getAtom(0)).getPosition();
				IVector leafPos5 = ((IAtomPositioned)molecule.getAtom(5)).getPosition();
				
				v.Ev1Mv2(leafPos5, leafPos0);
				v.normalize();
				 
				potentialMaster.calculate(box, allAtoms, forceSum);
				 
				/*
				 * To find the rotation axis by taking the cross-product
				 * of v and delta v
				 * 
				 * there are 3 cases: u[3], u[4], and u[5]
				 * setting the rotation axis correspondingly
				 * 
				 *  having jay to loop over the 3 cases
				 */
				 
				for (int jay=0; jay<3; jay++){
					 
					 if(jay==0){
					 	deltaV.E(new double[]{-u[j]/Math.sqrt(1-u[j]*u[j]-u[j+1]*u[j+1]) ,1 ,0});
					 	rotationAxis.E(v);
					 	rotationAxis.XE(deltaV);
					 	rotationAxis.normalize();
					 } else
					
					 if(jay==1){
						 deltaV.E(new double[]{-u[j+1]/Math.sqrt(1-u[j]*u[j]-u[j+1]*u[j+1]) ,0 ,1});
						 rotationAxis.E(v);
						 rotationAxis.XE(deltaV);
						 rotationAxis.normalize();
					 } else
					 
					 if(jay==2){
						 rotationAxis.E(v);
					 }
					 
					 
					 /*
					  * To find the distance vector, d[] of each atoms within p-th molecule
					  * that is perpendicular to the rotation axis
					  */
					 for (int q=0; q<molecule.getAtomCount(); q++){
						 
			    	    	/*
			    	    	 * Determine the distance, d, by using Vector Projection
			    	    	 */
						 
						 	// vector a when q=0
						 	if (q==0){
						 		a.E(new double[] {0, 0, 0});
						 	} else {
						 	
				    	    	a.Ev1Mv2(((IAtomPositioned)molecule.getAtom(q)).getPosition(), leafPos0);
				    	    	a.normalize();
						 	}
						 	
			    	    	double dotProd = a.dot(rotationAxis);
			    	    	aProj.Ea1Tv1(dotProd,rotationAxis);
			    	    	aProj.normalize();
			    	    	
			    	    	if (q==0){
						 		distance[q].E(new double[] {0, 0, 0});
						 	} else {
						 		
						 		distance[q].Ev1Mv2(a, aProj);             
						 		distance[q].normalize();                 
						 	}
					 }
					 
					 /*
					  * The forces acting on each individual atoms within a given p-th molecule
					  *   x-component, y-componet and z-component
					  *   
					  *   And summing the torque of all atoms to torqueSum[]
					  */
	
					 moleculeForce.E(0); //initialize moleculeForce to zero
					 torqueSum.E(0);
					 
					 for (int q=0; q<molecule.getAtomCount(); q++){ 
						
						if (q==0){
							deltaV.E(new double[] {0, 0, 0});
						} else {
							
						deltaV.E(distance[q]);
						deltaV.XE(rotationAxis);
						deltaV.normalize();
						}
						
						moleculeForce.E(((IntegratorVelocityVerlet.MyAgent)agentManager.getAgent((IAtomLeaf)molecule.getAtom(q)))
									   .force);
						
						double scalarF = 0;
						scalarF = moleculeForce.dot(deltaV);
						torqueF[q].Ea1Tv1(scalarF, deltaV);
						
						if (q==0){
					 		torque[q].E(new double[] {0, 0, 0});
					 	} else {
							torque[q].E(d[q]);                         // torque = d X F
							torque[q].XE(torqueF[q]);
					 	}
						
						torqueSum.PE(torque[q]);    
						// torqueSum all equal to NaN!!!!!!!!!!!!!
					 }
					 
					fPrimeRotation[j+jay] = Math.sqrt(torqueSum.squared());  //taking the magnitude
					
					if (index == j+jay){
						return fPrimeRotation[j+jay];
					}
					
				 }
				
				 j += coordinateDefinition.getCoordinateDim()/molecules.getAtomCount();
			}
		
		return fPrimeRotation[index];
		}
	}
	
	
	
	protected final IVector3D rotationAxis;
	protected final IVector3D a, aProj;
	protected final IVector3D v, deltaV;
	protected final IVector3D [] distance, torque, torqueF;
	protected final IVector torqueSum;
	protected double[] fPrimeRotation;
	private static final long serialVersionUID = 1L;
}
