package etomica.potential;

import etomica.api.IAtomKinetic;
import etomica.api.IAtomLeaf;
import etomica.api.IAtomSet;
import etomica.api.IBox;
import etomica.api.IPotential;
import etomica.integrator.IntegratorBox;
import etomica.space.ISpace;
import etomica.space.Tensor;

/**
 * Calculates the pressure tensor by calculating the force on each atom, along
 * with including the kinetic portion (from the velocities or an Integrator).
 * If the simulation is non-dynamic (MC), the Integrator must be provided.
 */
public class PotentialCalculationPressureTensor implements PotentialCalculation {

    private static final long serialVersionUID = 1L;
    protected final Tensor pressureTensor;
    protected final Tensor workTensor;
    protected final ISpace space;
    protected IAtomSet leafList;
    protected IntegratorBox integrator;
    protected boolean warningPrinted;
    
    public PotentialCalculationPressureTensor(ISpace space) {
        this.space = space;
        pressureTensor = space.makeTensor();
        workTensor = space.makeTensor();
    }
    
    /**
	 * Adds the pressure tensor contribution based on the forces acting on each
     * pair of atoms produced by the iterator.
	 */
	public void doCalculation(IAtomSet atoms, IPotential potential) {
		((PotentialSoft)potential).gradient(atoms, pressureTensor);
	}
    
    public void setBox(IBox newBox) {
        leafList = newBox.getLeafList();
    }
    
    public void zeroSum() {
        pressureTensor.E(0);
    }

    /**
     * Sets an integrator to use a source for the temperature to compute the
     * kinetic portion of the pressure.  If running a dynamic simulation
     * (where the Atoms have velocities), this method should not be called.
     */
    public void setIntegrator(IntegratorBox newIntegrator) {
        integrator = newIntegrator;
    }
    
    /**
     * Returns the pressure tensor based on a previous call to 
     * PotentialMaster.calculate
     */
    public Tensor getPressureTensor() {
        if (leafList.getAtomCount() == 0) {
            return pressureTensor;
        }
        
        // now handle the kinetic part
        workTensor.E(0);

        if (leafList.getAtom(0) instanceof IAtomKinetic) {
            if (integrator != null) {
                warningPrinted = true;
                System.out.println("Ignoring Integrator's temperature and using actual Atom velocities.  You shouldn't have given me an Integrator.");
            }
        }
        else if (integrator == null) {
            throw new RuntimeException("Need an IntegratorBox to provide temperature since this is a non-dynamic simulation");
        }
        else {
            for (int i = 0; i < space.D(); i++) {
                pressureTensor.PE(leafList.getAtomCount()*integrator.getTemperature());
            }
            return pressureTensor;
        }

        // simulation is dynamic, use the velocities
        int nLeaf = leafList.getAtomCount();
        for (int iLeaf=0; iLeaf<nLeaf; iLeaf++) {
            IAtomKinetic atom = (IAtomKinetic)leafList.getAtom(iLeaf);
            workTensor.Ev1v2(atom.getVelocity(), atom.getVelocity());
            workTensor.TE(((IAtomLeaf)atom).getType().getMass());
            pressureTensor.PE(workTensor);
        }
        
        return pressureTensor;
    }
}
