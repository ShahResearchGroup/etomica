package etomica;

/**
 * Elementary Monte Carlo move in which a molecule of a specified species is
 * inserted into or removed from a phase.
 *
 * @author David Kofke
 */
public class MCMoveInsertDelete extends MCMove {
    
    //chemical potential
    private double mu;
    
    //directive must specify "BOTH" to get energy with all atom pairs
    private final IteratorDirective iteratorDirective = new IteratorDirective(IteratorDirective.BOTH);
    private Species species;
    private SpeciesAgent speciesAgent;

    public MCMoveInsertDelete(IntegratorMC parent) {
        super(parent);
        setStepSizeMax(1.0);
        setStepSizeMin(0.0);
        setStepSize(0.10);
        setMu(0.0);
        setTunable(false);
    }
    
//perhaps should have a way to ensure that two instances of this class aren't assigned the same species
    public void setSpecies(Species s) {
        species = s;
        if(phase != null) speciesAgent = (SpeciesAgent)species.getAgent(phase); 
    }
    public Species getSpecies() {return species;}
    
    public void setPhase(Phase p) {
        super.setPhase(p);
        if(species != null) speciesAgent = (SpeciesAgent)species.getAgent(phase); 
    }
    
    public final boolean thisTrial() {
        if(Simulation.random.nextDouble() < 0.5) {
            return trialInsert();
        }
        else {
            return trialDelete();
        }
    }
                                                                                                                                                                                                                                                                                                                                                                       
    private final boolean trialInsert() {
        Atom testMolecule = species.moleculeFactory().makeAtom();
        speciesAgent.addAtom(testMolecule);
        testMolecule.coord.translateTo(phase.randomPosition());
        double uNew = potential.set(phase).calculate(iteratorDirective.set(testMolecule), energy.reset()).sum();
        if(uNew == Double.MAX_VALUE) {  //overlap
            testMolecule.sendToReservoir();
            return false;
        }      
        double bNew = Math.exp((mu-uNew)/parentIntegrator.temperature)*phase.volume()/(speciesAgent.moleculeCount()+1);
        if(bNew < 1.0 && bNew < Simulation.random.nextDouble()) {  //reject
            testMolecule.sendToReservoir();
            return false;
        }
        else return true;
    }
    
    private final boolean trialDelete() {
        if(speciesAgent.moleculeCount() == 0) {return false;}
        Atom testMolecule = speciesAgent.randomMolecule();
        double uOld = potential.set(phase).calculate(iteratorDirective.set(testMolecule), energy.reset()).sum();
        double bOld = Math.exp((mu-uOld)/parentIntegrator.temperature);
        double bNew = speciesAgent.moleculeCount()/phase.volume();
        if(bNew > bOld || bNew > Simulation.random.nextDouble()*bOld) {  //accept
            testMolecule.sendToReservoir();
            return true;
        }
        else return false;
    }

    /**
     * Mutator method for the chemical potential of the insertion/deletion species.
     */
    public final void setMu(double mu) {this.mu = mu;}
    /**
     * Accessor method for the chemical potential of th insertion/deletion species.
     */
    public final double getMu() {return mu;}
    /**
     * Indicates that chemical potential has dimensions of energy.
     */
    public final etomica.units.Dimension getMuDimension() {return etomica.units.Dimension.ENERGY;}
    
    public static void main(String[] args) {
        etomica.simulations.HsMc2d sim = new etomica.simulations.HsMc2d();
        Simulation.instance = sim;

        MeterNMolecules meterN = new MeterNMolecules();
        DisplayBox box = new DisplayBox(meterN);
        box.setUpdateInterval(10);

		Simulation.instance.elementCoordinator.go();

        MCMoveInsertDelete mcMoveInsDel = new MCMoveInsertDelete(sim.integrator);
        mcMoveInsDel.setSpecies(sim.species);
        mcMoveInsDel.setMu(-2000.);
		                                    
        Simulation.makeAndDisplayFrame(sim);
    }//end of main
}//end of MCMoveInsertDelete