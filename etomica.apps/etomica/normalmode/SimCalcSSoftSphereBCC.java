package etomica.normalmode;

import etomica.action.PDBWriter;
import etomica.action.activity.ActivityIntegrate;
import etomica.api.IAtomTypeLeaf;
import etomica.api.IBox;
import etomica.box.Box;
import etomica.integrator.IntegratorMC;
import etomica.integrator.mcmove.MCMoveStepTracker;
import etomica.lattice.crystal.Basis;
import etomica.lattice.crystal.BasisCubicBcc;
import etomica.lattice.crystal.BasisMonatomic;
import etomica.lattice.crystal.Primitive;
import etomica.lattice.crystal.PrimitiveCubic;
import etomica.potential.P2SoftSphere;
import etomica.potential.P2SoftSphericalTruncatedShifted;
import etomica.potential.Potential2SoftSpherical;
import etomica.potential.PotentialMaster;
import etomica.simulation.Simulation;
import etomica.space.Boundary;
import etomica.space.BoundaryRectangularPeriodic;
import etomica.space.Space;
import etomica.species.SpeciesSpheresMono;

/**
 * MC simulation of BCC soft-sphere model in 3D with tabulation of the
 * collective-coordinate S-matrix. No graphic display of simulation.
 * 
 * @author Tai Boon Tan
 */
public class SimCalcSSoftSphereBCC extends Simulation {

    public SimCalcSSoftSphereBCC(Space _space, int numAtoms, double density, double temperature, int exponent) {
        super(_space, true);

        PotentialMaster potentialMaster = new PotentialMaster(space);

        SpeciesSpheresMono species = new SpeciesSpheresMono(this, space);
        getSpeciesManager().addSpecies(species);

        box = new Box(this, space);
        addBox(box);
        box.setNMolecules(species, numAtoms);

        integrator = new IntegratorMC(potentialMaster, getRandom(), temperature);
        MCMoveAtomCoupled move = new MCMoveAtomCoupled(potentialMaster, getRandom());
        move.setStepSize(0.2);
        move.setStepSizeMax(0.5);
        integrator.getMoveManager().addMCMove(move);
        ((MCMoveStepTracker)move.getTracker()).setNoisyAdjustment(true);
        
        activityIntegrate = new ActivityIntegrate(integrator);
        getController().addAction(activityIntegrate);
        // activityIntegrate.setMaxSteps(nSteps);

        if (space.D() == 1) {
            primitive = new PrimitiveCubic(space, 1.0/density);
            boundary = new BoundaryRectangularPeriodic(space, getRandom(), numAtoms/density);
            nCells = new int[]{numAtoms};
            basis = new BasisMonatomic(space);
        } else {
            double L = Math.pow(2.0/density, 1.0/3.0);
            primitive = new PrimitiveCubic(space, L);
            int n = (int)Math.round(Math.pow(numAtoms/2, 1.0/3.0));
            nCells = new int[]{n,n,n};
            boundary = new BoundaryRectangularPeriodic(space, random, n * L);
            basis = new BasisCubicBcc();
        }

        Potential2SoftSpherical potential = new P2SoftSphere(space, 1.0, 1.0, exponent);
        double truncationRadius = boundary.getDimensions().x(0) * 0.5;
        P2SoftSphericalTruncatedShifted pTruncated = new P2SoftSphericalTruncatedShifted(potential, truncationRadius);
        IAtomTypeLeaf sphereType = species.getLeafType();
        potentialMaster.addPotential(pTruncated, new IAtomTypeLeaf[] {sphereType, sphereType});
        move.setPotential(pTruncated);

        box.setBoundary(boundary);

        coordinateDefinition = new CoordinateDefinitionLeaf(box, primitive, basis, space);
        coordinateDefinition.initializeCoordinates(nCells);
        
        integrator.setBox(box);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        // defaults
        int D = 3;
        int nA = 128;
        double density = 2.2;
        double temperature = 0.01;
        int exponent = 12;
        if (D == 1) {
            nA = 3;
            density = 0.5;
        }
        long simSteps = 1000000;

        // parse arguments
        if (args.length > 1) {
            density = Double.parseDouble(args[1]);
        }
        if (args.length > 2) {
            simSteps = Long.parseLong(args[2]);
        }
        if (args.length > 3) {
            nA = Integer.parseInt(args[3]);
        }
        if (args.length > 4) {
            temperature = Double.parseDouble(args[4]);
        }
        if (args.length > 5) {
        	exponent = Integer.parseInt(args[5]);
        }
        String filename = "nm_SoftSphereBCC_n" + exponent + "_D"+density;
        if (args.length > 0) {
            filename = args[0];
        }

        System.out.println("Running "
                + (D == 1 ? "1D" : (D == 3 ? "BCC" : "2D hexagonal"))
                + " soft sphere simulation");
        System.out.println(nA + " atoms with exponent " + exponent+" and temperature "+temperature);
        System.out.println(simSteps+ " steps");
        System.out.println("output data to " + filename);

        // construct simulation
        SimCalcSSoftSphereBCC sim = new SimCalcSSoftSphereBCC(Space.getInstance(D), nA, density, temperature, exponent);

        // set up initial configuration and save nominal positions
        Primitive primitive = sim.primitive;
        
        /*
        final String APP_NAME = "SimCalcSSoftSphere";
        final SimulationGraphic simGraphic = new SimulationGraphic(sim, APP_NAME);
  
        simGraphic.getController().getReinitButton().setPostAction(simGraphic.getPaintAction(sim.box));
        simGraphic.makeAndDisplayFrame(APP_NAME);
        */
        
        /* 
         * set up normal-mode meter
         */
        
        
        MeterNormalMode meterNormalMode = new MeterNormalMode();
        meterNormalMode.setCoordinateDefinition(sim.coordinateDefinition);
        WaveVectorFactory waveVectorFactory;
        if (D == 1) {
            waveVectorFactory = new WaveVectorFactory1D(D);
        } else if (D == 2) {
            waveVectorFactory = null;
        } else {
            waveVectorFactory = new WaveVectorFactorySimple(primitive, sim.space);
        }
        
        meterNormalMode.setWaveVectorFactory(waveVectorFactory);
        meterNormalMode.setBox(sim.box);

        sim.integrator.addIntervalAction(meterNormalMode);
        sim.integrator.setActionInterval(meterNormalMode, nA);
	
/*        
    	final String APP_NAME = "HSMD3D";
        final SimulationGraphic simGraphic = new SimulationGraphic(sim, APP_NAME);
        simGraphic.getController().getReinitButton().setPostAction(simGraphic.getPaintAction(sim.box));
        ((AtomTypeSphere)((SpeciesSpheresMono)sim.getSpeciesManager().getSpecies()[0]).getLeafType()).setDiameter(.5);
        simGraphic.makeAndDisplayFrame(APP_NAME);
  */      
        

        // MeterMomentumCOM meterCOM = new MeterMomentumCOM(sim.space);
        // MeterPositionCOM meterCOM = new MeterPositionCOM(sim.space);
        // DataSinkConsole console = new DataSinkConsole();
        // DataPump comPump = new DataPump(meterCOM,console);
        // IntervalActionAdapter comAdapter = new
        // IntervalActionAdapter(comPump);
        // sim.integrator.addListener(comAdapter);
        // meterCOM.setBox(sim.box);

        // start simulation
//        MeterEnergy m = new MeterEnergy(sim.getPotentialMaster());
//        m.setBox(sim.box);
//        DataLogger logger = new DataLogger();
//        logger.setAppending(true);
//        logger.setCloseFileEachTime(true);
//        DataTableWriter writer = new DataTableWriter();
//        writer.setIncludeHeader(false);
//        logger.setDataSink(writer);
//        logger.setFileName("LJ_energy.dat");
//        logger.setSameFileEachTime(true);
//        logger.setWriteInterval(1);
//        logger.setWriteOnInterval(true);
//        DataPump pump = new DataPump(m, logger);
//        sim.integrator.addListener(new IntervalActionAdapter(pump));
        
        
        
        
      
        sim.activityIntegrate.setMaxSteps(simSteps/10);
        sim.getController().actionPerformed();
        System.out.println("equilibrated");
        sim.integrator.getMoveManager().setEquilibrating(false);
        sim.getController().reset();
        meterNormalMode.reset();

        WriteS sWriter = new WriteS(sim.space);
        sWriter.setFilename(filename);
        sWriter.setOverwrite(true);
        sWriter.setMeter(meterNormalMode);
        sWriter.setWaveVectorFactory(waveVectorFactory);
        sWriter.setTemperature(temperature);
        sim.integrator.addIntervalAction(sWriter);
        sim.integrator.setActionInterval(sWriter, (int)simSteps/10);
        
        sim.activityIntegrate.setMaxSteps(simSteps);
        sim.getController().actionPerformed();
        PDBWriter pdbWriter = new PDBWriter(sim.box);
        pdbWriter.setFileName("calcS_n"+exponent+"_D"+density+".pdb");
        pdbWriter.actionPerformed();
       
    }

    private static final long serialVersionUID = 1L;
    public IntegratorMC integrator;
    public ActivityIntegrate activityIntegrate;
    public IBox box;
    public Boundary boundary;
    public Primitive primitive;
    public Basis basis;
    public int[] nCells;
    public CoordinateDefinition coordinateDefinition;
}