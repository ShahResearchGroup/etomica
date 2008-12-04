package etomica.potential;

import etomica.api.IAtomLeaf;
import etomica.api.IAtomList;
import etomica.api.IPotential;
import etomica.api.IVector;
import etomica.atom.AtomLeafAgentManager;
import etomica.integrator.IntegratorBox;

/**
 * Sums the force on each iterated atom and adds it to the integrator agent
 * associated with the atom.
 */
public class PotentialCalculationForceSum implements PotentialCalculation {
        
    private static final long serialVersionUID = 1L;
    protected AtomLeafAgentManager integratorAgentManager;
    protected AtomLeafAgentManager.AgentIterator agentIterator;
    
    public void setAgentManager(AtomLeafAgentManager agentManager) {
        integratorAgentManager = agentManager;
        agentIterator = integratorAgentManager.makeIterator();
    }

    /**
     * Re-zeros the force vectors.
     *
     */
    public void reset(){
        
        agentIterator.reset();
        while(agentIterator.hasNext()){
            Object agent = agentIterator.next();
            if (agent instanceof IntegratorBox.Forcible) {
                ((IntegratorBox.Forcible)agent).force().E(0);
            }
        }
    }

    /**
     * Adds forces due to given potential acting on the atoms produced by the iterator.
     * Implemented for only 1- and 2-body potentials.
     */
    public void doCalculation(IAtomList atoms, IPotential potential) {
        PotentialSoft potentialSoft = (PotentialSoft)potential;
        int nBody = potential.nBody();
        IVector[] f = potentialSoft.gradient(atoms);
        switch(nBody) {
            case 1:
                ((IntegratorBox.Forcible)integratorAgentManager.getAgent((IAtomLeaf)atoms.getAtom(0))).force().ME(f[0]);
                break;
            case 2:
                ((IntegratorBox.Forcible)integratorAgentManager.getAgent((IAtomLeaf)atoms.getAtom(0))).force().ME(f[0]);
                ((IntegratorBox.Forcible)integratorAgentManager.getAgent((IAtomLeaf)atoms.getAtom(1))).force().ME(f[1]);
                break;
            default:
                //XXX atoms.count might not equal f.length.  The potential might size its 
                //array of vectors to be large enough for one IAtomSet and then not resize it
                //back down for another IAtomSet with fewer atoms.
                for (int i=0; i<atoms.getAtomCount(); i++) {
                    ((IntegratorBox.Forcible)integratorAgentManager.getAgent((IAtomLeaf)atoms.getAtom(i))).force().ME(f[i]);
                }
		}
	}
}
