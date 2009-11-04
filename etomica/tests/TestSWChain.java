package etomica.tests;

import etomica.action.ActionIntegrate;
import etomica.api.IAtomType;
import etomica.api.IBox;
import etomica.api.ISpecies;
import etomica.atom.AtomTypeSphere;
import etomica.atom.iterator.ApiBuilder;
import etomica.box.Box;
import etomica.config.ConfigurationFile;
import etomica.config.ConformationLinear;
import etomica.data.AccumulatorAverage;
import etomica.data.AccumulatorAverageFixed;
import etomica.data.DataPump;
import etomica.data.AccumulatorAverage.StatType;
import etomica.data.meter.MeterPotentialEnergyFromIntegrator;
import etomica.data.meter.MeterPressureHard;
import etomica.data.types.DataDouble;
import etomica.data.types.DataGroup;
import etomica.integrator.IntegratorHard;
import etomica.listener.IntegratorListenerAction;
import etomica.nbr.CriterionAll;
import etomica.nbr.CriterionBondedSimple;
import etomica.nbr.CriterionInterMolecular;
import etomica.nbr.list.PotentialMasterList;
import etomica.potential.P2HardBond;
import etomica.potential.P2SquareWell;
import etomica.potential.PotentialGroup;
import etomica.simulation.Simulation;
import etomica.space.Space;
import etomica.space3d.Space3D;
import etomica.species.SpeciesSpheres;

/**
 * Simple square-well chain simulation.
 * Initial configurations at http://rheneas.eng.buffalo.edu/etomica/tests/
 */
 
public class TestSWChain extends Simulation {
    
    private static final long serialVersionUID = 1L;
    public IntegratorHard integrator;
    public IBox box;

    public TestSWChain(Space _space) {
        this(_space, 500);
    }
    
    public TestSWChain(Space _space, int numMolecules) {
        super(_space);
        PotentialMasterList potentialMaster = new PotentialMasterList(this, space);
        int chainLength = 10;
        int numAtoms = numMolecules * chainLength;
        double sigma = 1.0;
        double sqwLambda = 1.5;
        double neighborRangeFac = 1.2;
        double bondFactor = 0.15;
        double timeStep = 0.005;
        double simTime = 100000.0/numAtoms;
        int nSteps = (int)(simTime / timeStep);

        // makes eta = 0.35
        double l = 14.4094*Math.pow((numAtoms/2000.0),1.0/3.0);
        integrator = new IntegratorHard(this, potentialMaster, space);
        integrator.setTimeStep(timeStep);
        integrator.setIsothermal(true);
        ActionIntegrate actionIntegrate = new ActionIntegrate(integrator,false);
        getController().addAction(actionIntegrate);
        actionIntegrate.setMaxSteps(nSteps);
        potentialMaster.setCellRange(2);
        potentialMaster.setRange(neighborRangeFac*sqwLambda*sigma);

        SpeciesSpheres species = new SpeciesSpheres(this,_space,chainLength);
        species.setIsDynamic(true);
        addSpecies(species);
        P2HardBond bonded = new P2HardBond(space, sigma, bondFactor, false);
        PotentialGroup potentialChainIntra = potentialMaster.makePotentialGroup(1);
        potentialChainIntra.addPotential(bonded, ApiBuilder.makeAdjacentPairIterator());

        potentialMaster.addPotential(potentialChainIntra, new ISpecies[] {species});
        ((ConformationLinear)species.getConformation()).setBondLength(sigma);

        P2SquareWell potential = new P2SquareWell(space,sigma,sqwLambda,0.5,false);

        AtomTypeSphere sphereType = (AtomTypeSphere)species.getLeafType();
        potentialMaster.addPotential(potential,new IAtomType[]{sphereType,sphereType});
        CriterionInterMolecular sqwCriterion = (CriterionInterMolecular)potentialMaster.getCriterion(potential);
        CriterionBondedSimple nonBondedCriterion = new CriterionBondedSimple(new CriterionAll());
        nonBondedCriterion.setBonded(false);
        sqwCriterion.setIntraMolecularCriterion(nonBondedCriterion);

        box = new Box(space);
        addBox(box);
        box.getBoundary().setBoxSize(space.makeVector(new double[]{l,l,l}));
        box.setNMolecules(species, numMolecules);
        integrator.getEventManager().addListener(potentialMaster.getNeighborManager(box));

        integrator.setBox(box);
        ConfigurationFile config = new ConfigurationFile("SWChain"+Integer.toString(numMolecules));
        config.initializeCoordinates(box);
    }
    
    public static void main(String[] args) {
        int numMolecules = 500;
        if (args.length > 0) {
            numMolecules = Integer.valueOf(args[0]).intValue();
        }
        Space sp = Space3D.getInstance();
        TestSWChain sim = new TestSWChain(sp, numMolecules);

        MeterPressureHard pMeter = new MeterPressureHard(sim.space);
        pMeter.setIntegrator(sim.integrator);
        MeterPotentialEnergyFromIntegrator energyMeter = new MeterPotentialEnergyFromIntegrator(sim.integrator);
        AccumulatorAverage energyAccumulator = new AccumulatorAverageFixed();
        DataPump energyManager = new DataPump(energyMeter, energyAccumulator);
        energyAccumulator.setBlockSize(50);
        sim.integrator.getEventManager().addListener(new IntegratorListenerAction(energyManager));
        
        sim.getController().actionPerformed();
        
        double Z = pMeter.getDataAsScalar()*sim.box.getBoundary().volume()/(sim.box.getMoleculeList().getMoleculeCount()*sim.integrator.getTemperature());
        double avgPE = ((DataDouble)((DataGroup)energyAccumulator.getData()).getData(StatType.AVERAGE.index)).x;
        avgPE /= numMolecules;
        System.out.println("Z="+Z);
        System.out.println("PE/epsilon="+avgPE);
        double temp = sim.integrator.getTemperature();
        double Cv = ((DataDouble)((DataGroup)energyAccumulator.getData()).getData(StatType.STANDARD_DEVIATION.index)).x;
        Cv /= temp;
        Cv *= Cv/numMolecules;
        System.out.println("Cv/k="+Cv);
        
        if (Double.isNaN(Z) || Math.abs(Z-4.5) > 1.5) {
            System.exit(1);
        }
        if (Double.isNaN(avgPE) || Math.abs(avgPE+19.32) > 0.12) {
            System.exit(1);
        }
        // actual value ~2
        if (Double.isNaN(Cv) || Cv < 0.5 || Cv > 4.5) {
            System.exit(1);
        }
    }
}