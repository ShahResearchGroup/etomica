package etomica.modules.chainequilibrium;

import etomica.action.BoxImposePbc;
import etomica.action.activity.ActivityIntegrate;
import etomica.api.IAtom;
import etomica.api.IAtomTypeLeaf;
import etomica.api.IAtomTypeSphere;
import etomica.api.IBox;
import etomica.api.IController;
import etomica.atom.AtomAgentManager;
import etomica.atom.AtomLeafAgentManager;
import etomica.atom.AtomAgentManager.AgentSource;
import etomica.box.Box;
import etomica.config.ConfigurationLattice;
import etomica.data.meter.MeterTemperature;
import etomica.integrator.IntegratorHard;
import etomica.integrator.IntegratorMD.ThermostatType;
import etomica.lattice.LatticeOrthorhombicHexagonal;
import etomica.potential.P1HardPeriodic;
import etomica.potential.PotentialMaster;
import etomica.simulation.Simulation;
import etomica.space.BoundaryRectangularPeriodic;
import etomica.space2d.Space2D;
import etomica.species.SpeciesSpheresMono;

public class ChainEquilibriumSim extends Simulation implements AgentSource {

	public MeterChainLength molecularCount;
	public IController controller1;
	public IntegratorHard integratorHard1;
	public java.awt.Component display;
	public IBox box;
	public etomica.action.SimulationRestart restartAction;
	public boolean initializing = true;
	public MeterTemperature thermometer;
	public SpeciesSpheresMono speciesA;
	public SpeciesSpheresMono speciesB;
	public P2SquareWellBonded AAbonded;
	public P2SquareWellBonded ABbonded;
	public P2SquareWellBonded BBbonded;
    public ActivityIntegrate activityIntegrate;
    private AtomLeafAgentManager agentManager = null;
    public IAtom[] agents;

    public ChainEquilibriumSim() {
        super(Space2D.getInstance());
        PotentialMaster potentialMaster = new PotentialMaster(space);
        controller1 = getController();

        double diameter = 1.0;

        integratorHard1 = new IntegratorHard(this, potentialMaster, space);
        integratorHard1.setIsothermal(true);
        integratorHard1.setThermostat(ThermostatType.ANDERSEN_SINGLE);
        integratorHard1.setThermostatInterval(10);

        box = new Box(this, space);
        addBox(box);
        box.setBoundary(new BoundaryRectangularPeriodic(space, random, 30));
        integratorHard1.setBox(box);	
        speciesA = new SpeciesSpheresMono(this, space);
        speciesB = new SpeciesSpheresMono(this, space);
        getSpeciesManager().addSpecies(speciesA);
        getSpeciesManager().addSpecies(speciesB);
        ((IAtomTypeSphere)speciesA.getLeafType()).setDiameter(diameter);
        ((IAtomTypeSphere)speciesB.getLeafType()).setDiameter(diameter);
        box.setNMolecules(speciesA, 10);
        box.setNMolecules(speciesB, 40);
        new ConfigurationLattice(new LatticeOrthorhombicHexagonal(), space).initializeCoordinates(box);

        agentManager = new AtomLeafAgentManager(this,box);

        molecularCount = new MeterChainLength(agentManager);
        molecularCount.setBox(box);

		//potentials
        AAbonded = new P2SquareWellBonded(space, agentManager, 0.5 * diameter, 2.0, 1.0);
		ABbonded = new P2SquareWellBonded(space, agentManager, 0.5 * diameter, 2.0, 1.0);
		BBbonded = new P2SquareWellBonded(space, agentManager, 0.5 * diameter, 2.0, 1.0);

		potentialMaster.addPotential(AAbonded,
		        new IAtomTypeLeaf[] { speciesA.getLeafType(), speciesA.getLeafType() });
		potentialMaster.addPotential(ABbonded,
		        new IAtomTypeLeaf[] { speciesA.getLeafType(), speciesB.getLeafType() });
		
		potentialMaster.addPotential(BBbonded,
		        new IAtomTypeLeaf[] { speciesB.getLeafType(), speciesB.getLeafType() });


		// **** Setting Up the thermometer Meter *****
		
		thermometer = new MeterTemperature(box, space.D());
		
		integratorHard1.setNullPotential(new P1HardPeriodic(space, 3), speciesA.getLeafType());
        integratorHard1.setNullPotential(new P1HardPeriodic(space, 3), speciesB.getLeafType());
        
		activityIntegrate = new ActivityIntegrate(integratorHard1, 1, true);
		getController().addAction(activityIntegrate);
		integratorHard1.addIntervalAction(new BoxImposePbc(box, space));

	}
    
    public Class getAgentClass() {
        return IAtom[].class;
    }
    
	/**
	 * Implementation of AtomAgentManager.AgentSource interface. Agent
     * is used to hold bonding partners.
	 */
	public Object makeAgent(IAtom a) {
		
		return new IAtom[2];
	}
    
    public void releaseAgent(Object agent, IAtom atom) {}
    
    public AtomAgentManager getAgentManager() {
    	return agentManager;
    }
}
