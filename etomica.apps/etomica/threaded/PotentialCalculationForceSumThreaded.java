package etomica.threaded;

import etomica.api.IAtom;
import etomica.api.IAtomSet;
import etomica.api.IBox;
import etomica.api.IPotential;
import etomica.api.IVector;
import etomica.atom.AtomLeafAgentManager;
import etomica.atom.AtomLeafAgentManager.AgentSource;
import etomica.integrator.IntegratorBox;
import etomica.integrator.IntegratorVelocityVerlet.MyAgent;
import etomica.potential.PotentialCalculation;
import etomica.potential.PotentialCalculationForceSum;
import etomica.space.ISpace;

public class PotentialCalculationForceSumThreaded extends PotentialCalculationForceSum implements IPotentialCalculationThreaded, AgentSource{

	final protected PotentialCalculationForceSum[] pc;
	protected AtomLeafAgentManager[] atomAgentManager;
	private final ISpace space;
    
	public PotentialCalculationForceSumThreaded(PotentialCalculationForceSum[] pc, ISpace _space) {
		this.pc = pc;
		this.space = _space;
	}

    public void reset(){
        super.reset();
        for (int i=0; i<pc.length; i++){
            pc[i].reset();
        }
    }
    
	public void setAgentManager(AtomLeafAgentManager agentManager) {
        super.setAgentManager(agentManager);
        atomAgentManager = new AtomLeafAgentManager[pc.length];
        
        for (int i=0; i<pc.length; i++){
            atomAgentManager[i] = new AtomLeafAgentManager(this, agentManager.getBox());
            pc[i].setAgentManager(atomAgentManager[i]);
            agentManager.getBox();
		}
		
	}
	
	public void doCalculation(IAtomSet atoms, IPotential potential) {
		throw new RuntimeException("This is not the correct 'doCalculation' to call.");
	}
	
	/* (non-Javadoc)
	 * @see etomica.threads.PotentialCalculationThreaded#getPotentialCalculations()
	 */
	public PotentialCalculation[] getPotentialCalculations(){
		return pc;
	}
	
	public void writeData(){
       
		IBox box = integratorAgentManager.getBox();
        IAtomSet atomArrayList = box.getLeafList();
      
        for(int j=0; j<atomArrayList.getAtomCount(); j++){
            IVector force = ((IntegratorBox.Forcible)integratorAgentManager.getAgent(atomArrayList.getAtom(j))).force();
      
            for(int i=0; i<pc.length; i++){
                force.PE(((IntegratorBox.Forcible)atomAgentManager[i].getAgent(atomArrayList.getAtom(j))).force());
               
                
            }
        }
            
	}
    
    public Class getAgentClass() {
        return MyAgent.class;
    }

    public final Object makeAgent(IAtom a) {
        return new MyAgent(space);
    }
    
    public void releaseAgent(Object object, IAtom atom){
        
    }
    
}
