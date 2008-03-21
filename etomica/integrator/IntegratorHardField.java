package etomica.integrator;

import etomica.EtomicaInfo;
import etomica.api.IAtom;
import etomica.api.IAtomSet;
import etomica.api.IAtomTypeLeaf;
import etomica.api.IBox;
import etomica.api.IPotential;
import etomica.api.IPotentialMaster;
import etomica.api.IRandom;
import etomica.api.ISimulation;
import etomica.api.IVector;
import etomica.atom.IAtomKinetic;
import etomica.atom.iterator.IteratorDirective;
import etomica.exception.ConfigurationOverlapException;
import etomica.potential.Potential1;
import etomica.space.Space;

/**
 * Extension of IntegratorHard for case where a constant external force field is applied.
 *
 * @see IntegratorHard
 * @author David Kofke
 *
 */
public final class IntegratorHardField extends IntegratorHard {

    private static final long serialVersionUID = 1L;
	public final PotentialCalculationForceSum forceSum;
    private final IteratorDirective fieldsOnly = new IteratorDirective();
//    private final Space space;
	
    //XXX not serializable
    private final IteratorDirective.PotentialCriterion noFieldsCriterion = new IteratorDirective.PotentialCriterion() {
	    public boolean excludes(IPotential candidatePotential) {
	        return (candidatePotential instanceof Potential1);
	    }
    };

    public IntegratorHardField(ISimulation sim, IPotentialMaster potentialMaster, Space _space) {
        this(sim, potentialMaster, sim.getRandom(),0.05, 1.0, _space);
    }
    
    public IntegratorHardField(ISimulation sim, IPotentialMaster potentialMaster, IRandom random,
            double timeStep, double temperature, Space _space) {
        super(sim, potentialMaster,random,timeStep,temperature, _space, false);
        forceSum = new PotentialCalculationForceSum();
        //XXX not serializable
        fieldsOnly.addCriterion(new IteratorDirective.PotentialCriterion() {
            public boolean excludes(IPotential candidatePotential) {
                return !(candidatePotential instanceof Potential1);
            }
        });
        upList.addCriterion(noFieldsCriterion);
        downList.addCriterion(noFieldsCriterion);
    }
    
    public static EtomicaInfo getEtomicaInfo() {
        EtomicaInfo info = new EtomicaInfo("Collision-based MD simulation in the presence of a constant external field");
        return info;
    }

    /**
    * Advances all atom coordinates by tStep, without any intervening collisions.
    * Uses constant-force kinematics.
    */
    protected void advanceAcrossTimeStep(double tStep) {
        
        calculateForces();
        
        double t2 = 0.5*tStep*tStep;
        IAtomSet leafList = box.getLeafList();
        int nLeaf = leafList.getAtomCount();
        for (int iLeaf=0; iLeaf<nLeaf; iLeaf++) {
            IAtomKinetic a = (IAtomKinetic)leafList.getAtom(iLeaf);
            HardFieldAgent agent = (HardFieldAgent)agentManager.getAgent(a);
            agent.decrementCollisionTime(tStep);
            a.getPosition().PEa1Tv1(tStep,a.getVelocity());
            if(!agent.forceFree) {
//                System.out.println("IntegratorHardField "+agent.force.toString()+" "+a.toString());
                a.getPosition().PEa1Tv1(t2*((IAtomTypeLeaf)a.getType()).rm(),agent.force);
                a.getVelocity().PEa1Tv1(tStep*((IAtomTypeLeaf)a.getType()).rm(),agent.force);
            }
        }
    }
    
    public void setBox(IBox newBox) {
        super.setBox(newBox);
        forceSum.setAgentManager(agentManager);
    }
    
    public void reset() throws ConfigurationOverlapException {
        calculateForces();
        super.reset();
    }

    private void calculateForces() {
        
        //Compute all forces
        IAtomSet leafList = box.getLeafList();
        int nLeaf = leafList.getAtomCount();
        for (int iLeaf=0; iLeaf<nLeaf; iLeaf++) {
            IAtom atom = leafList.getAtom(iLeaf);
            ((HardFieldAgent)agentManager.getAgent(atom)).forceFree = true;
        }
        //zero forces on all atoms
        forceSum.reset();
        //Compute forces on each atom
        potential.calculate(box, fieldsOnly, forceSum);
        
    }//end of calculateForces
    
    /**
    *
    */
    public void scaleMomenta(double s) {
        double rs = 1.0/s;
        IAtomSet leafList = box.getLeafList();
        int nLeaf = leafList.getAtomCount();
        for (int iLeaf=0; iLeaf<nLeaf; iLeaf++) {
            IAtomKinetic a = (IAtomKinetic)leafList.getAtom(iLeaf);
            a.getVelocity().TE(s); //scale momentum
            ((Agent)agentManager.getAgent(a)).eventLinker.sortKey *= rs;
        }
        for (int iLeaf=0; iLeaf<nLeaf; iLeaf++) {
            IAtomKinetic a = (IAtomKinetic)leafList.getAtom(iLeaf);
 //           System.out.println(a.coord.position().toString()+a.coord.momentum().toString()+"  "+
 //                               a.coord.momentum().squared());
            HardFieldAgent iagent = (HardFieldAgent)agentManager.getAgent(a);
            if(!iagent.forceFree) updateAtom(a);//update because not force free
            IAtom partner = iagent.collisionPartner();
            if(partner == null) continue;
            HardFieldAgent jagent = (HardFieldAgent)agentManager.getAgent(partner);
            if(!iagent.forceFree) {
                updateAtom(partner);//update because partner not force free
                continue;
            }
            if(!jagent.forceFree) {
                updateAtom(partner);
                updateAtom(a);
            }
        }
        findNextCollider();
 //       System.out.println();
    }
    
    public Class getTypeAgentClass() {
        return HardFieldAgent.class;
    }
    
    /**
    * Produces the Agent defined by this integrator.
    * One instance of an Agent is placed in each atom controlled by this integrator.
    */
    public Object makeAgent(IAtom a) {
        return new HardFieldAgent(a,this);
    }
     
    /**
    * Extends IntegratorHard.Agent to hold a force vector.
    */
    public static class HardFieldAgent extends IntegratorHard.Agent implements IntegratorBox.Forcible { 
    
        public final IVector force;
        public boolean forceFree = true;
        public HardFieldAgent(IAtom a, IntegratorHardField integrator) {
            super(a, integrator);
            force = integrator.space.makeVector();
        }
        public final IVector force() {return force;}
    }//end of Agent
    
    /**
     * Sums the force on each iterated atom and adds it to the integrator agent
     * associated with the atom.
     * Differs from PotentialCalculation.ForceSum in that only 1-body potentials
     * are considered, and also sets forceFree flag of Agent appropriately.
     */
    public static final class PotentialCalculationForceSum extends etomica.potential.PotentialCalculationForceSum {

		public void doCalculation(IAtomSet atoms, IPotential potential) {
			super.doCalculation(atoms,potential);
			((HardFieldAgent)integratorAgentManager.getAgent(atoms.getAtom(0))).forceFree = false;
		}
    }
}
