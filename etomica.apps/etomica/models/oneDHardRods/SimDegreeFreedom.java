package etomica.models.oneDHardRods;

import etomica.action.activity.ActivityIntegrate;
import etomica.api.IAtomType;
import etomica.api.IBox;
import etomica.box.Box;
import etomica.data.AccumulatorHistogram;
import etomica.data.DataPump;
import etomica.data.DataSplitter;
import etomica.integrator.IntegratorMC;
import etomica.lattice.crystal.BasisMonatomic;
import etomica.lattice.crystal.Primitive;
import etomica.lattice.crystal.PrimitiveCubic;
import etomica.listener.IntegratorListenerAction;
import etomica.nbr.list.PotentialMasterList;
import etomica.normalmode.CoordinateDefinition;
import etomica.normalmode.CoordinateDefinitionLeaf;
import etomica.normalmode.MCMoveAtomCoupled;
import etomica.normalmode.NormalModes;
import etomica.normalmode.NormalModes1DHR;
import etomica.normalmode.P2XOrder;
import etomica.normalmode.WaveVectorFactory;
import etomica.potential.P2HardSphere;
import etomica.potential.Potential2;
import etomica.potential.Potential2HardSpherical;
import etomica.simulation.Simulation;
import etomica.space.Boundary;
import etomica.space.BoundaryRectangularPeriodic;
import etomica.space.Space;
import etomica.species.SpeciesSpheresMono;
import etomica.util.DoubleRange;
import etomica.util.HistogramExpanding;
import etomica.util.ParameterBase;
import etomica.util.ReadParameters;

/**
 * MD simulation of hard spheres in 1D or 3D with tabulation of the
 * collective-coordinate S-matrix. No graphic display of simulation.
 */
public class SimDegreeFreedom extends Simulation {

    private static final long serialVersionUID = 1L;
    private static final String APP_NAME = "SimDegreeFreedom1DHR";
    public Primitive primitive;
    int[] nCells;
    NormalModes nm;
    public IntegratorMC integrator;
    public BasisMonatomic basis;
    public ActivityIntegrate activityIntegrate;
    
    public IBox box;
    public Boundary bdry;
    public CoordinateDefinition coordinateDefinition;
    MeterNormalModeCoordinate meternmc;
    WaveVectorFactory waveVectorFactory;
    MCMoveAtomCoupled mcMoveAtom;
    MCMoveChangeSingleMode mcMoveMode;
    AccumulatorHistogram[] hists;
    int harmonicWV;

    public SimDegreeFreedom(Space _space, int numAtoms, double density, int blocksize) {
        super(_space, true);
        
//        long seed = 3;
//        System.out.println("Seed explicitly set to " + seed);
//        IRandom rand = new RandomNumberGenerator(seed);
//        this.setRandom(rand);
        
        PotentialMasterList potentialMaster = new PotentialMasterList(this, space);

        SpeciesSpheresMono species = new SpeciesSpheresMono(this, space);
        getSpeciesManager().addSpecies(species);
        
        basis = new BasisMonatomic(space);
        
        box = new Box(space);
        addBox(box);
        box.setNMolecules(species, numAtoms);
       
        Potential2 potential = new P2HardSphere(space, 1.0, true);
        potential = new P2XOrder(space, (Potential2HardSpherical)potential);
        potential.setBox(box);
//        AtomTypeSphere sphereType = (AtomTypeSphere)species.getLeafType();
        potentialMaster.addPotential(potential, new IAtomType[] {species.getLeafType(), species.getLeafType()});

        primitive = new PrimitiveCubic(space, 1.0/density);
        bdry = new BoundaryRectangularPeriodic(space, numAtoms/density);
        nCells = new int[]{numAtoms};
        box.setBoundary(bdry);
        
        coordinateDefinition = new CoordinateDefinitionLeaf(this, box, primitive, basis, space);
        coordinateDefinition.initializeCoordinates(nCells);
        
        double neighborRange = 1.01/density;
        potentialMaster.setRange(neighborRange);
        //find neighbors now.  Don't hook up NeighborListManager since the
        //  neighbors won't change
        potentialMaster.getNeighborManager(box).reset();
        
        integrator = new IntegratorMC(this, potentialMaster);
        integrator.setBox(box);
        
        nm = new NormalModes1DHR(space.D());
        nm.setHarmonicFudge(1.0);
        nm.setTemperature(1.0);
        nm.getOmegaSquared(box);
        
        waveVectorFactory = nm.getWaveVectorFactory();
        waveVectorFactory.makeWaveVectors(box);
        
        mcMoveAtom = new MCMoveAtomCoupled(potentialMaster, random, space);
        mcMoveAtom.setPotential(potential);
        mcMoveAtom.setBox(box);
        integrator.getMoveManager().addMCMove(mcMoveAtom);
        mcMoveAtom.setStepSizeMin(0.001);
        mcMoveAtom.setStepSize(0.01);
        
        mcMoveMode = new MCMoveChangeSingleMode(potentialMaster, random);
        mcMoveMode.setBox(box);
        integrator.getMoveManager().addMCMove(mcMoveMode);
        mcMoveMode.setCoordinateDefinition(coordinateDefinition);
        mcMoveMode.setEigenVectors(nm.getEigenvectors(box));
        mcMoveMode.setOmegaSquared(nm.getOmegaSquared(box));
        mcMoveMode.setWaveVectorCoefficients(nm.getWaveVectorFactory().getCoefficients());
        mcMoveMode.setWaveVectors(nm.getWaveVectorFactory().getWaveVectors());
        
        meternmc = new MeterNormalModeCoordinate(coordinateDefinition, nm.getWaveVectorFactory().getWaveVectors());
        meternmc.setEigenVectors(nm.getEigenvectors(box));
        meternmc.setOmegaSquared(nm.getOmegaSquared(box));
        
        int coordinateDim = coordinateDefinition.getCoordinateDim();
        int coordNum = nm.getWaveVectorFactory().getWaveVectors().length*coordinateDim*2;
        hists = new AccumulatorHistogram[coordNum];
        DataSplitter splitter = new DataSplitter();
        DataPump pumpFromMeter = new DataPump(meternmc, splitter);
        
        DoubleRange range = new DoubleRange(-1.0, 1.0);
        int nBins = 200;
        HistogramExpanding template; 
        for(int i = 0; i < coordNum; i++){
            template = new HistogramExpanding(nBins, range);
            hists[i] = new AccumulatorHistogram(template, nBins);
            splitter.setDataSink(i, hists[i]);
        }
        
        IntegratorListenerAction pumpFromMeterListener = new IntegratorListenerAction(pumpFromMeter);
        pumpFromMeterListener.setInterval(blocksize);
        integrator.getEventManager().addListener(pumpFromMeterListener);
        
        activityIntegrate = new ActivityIntegrate(integrator, 0, true);
        getController().addAction(activityIntegrate);
        
        
    }

    private void setHarmonicWV(int hwv){
        harmonicWV = hwv;
        mcMoveMode.setHarmonicWV(hwv);
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {

        SimParam params = new SimParam();
        String inputFilename = null;
        if(args.length > 0) {
            inputFilename = args[0];
        }
        if(inputFilename != null){
            ReadParameters readParameters = new ReadParameters(inputFilename, params);
            readParameters.readParameters();
            inputFilename = params.inputfilename;
        }
        
        int nA = params.numAtoms;
        double density = params.density;
        int D = params.D;
        double harmonicFudge = params.harmonicFudge;
        String filename = params.filename;
        if(filename.length() == 0){
            filename = "1DHR";
        }
        double temperature = params.temperature;
        int comparedWV = params.comparedWV;
        int nSteps = params.numSteps;
        int bs = params.blockSize;
        
        System.out.println("Running "
                + (D == 1 ? "1D" : (D == 3 ? "FCC" : "2D hexagonal"))
                + " hard sphere simulation");
        System.out.println(nA + " atoms at density " + density);
        System.out.println(nSteps + " steps, " + bs + " blocksize");
        System.out.println("input data from " + inputFilename);
        System.out.println("output data to " + filename);

        // construct simulation
        SimDegreeFreedom sim = new SimDegreeFreedom(Space.getInstance(D), nA, density, bs);
        
        // start simulation
        sim.activityIntegrate.setMaxSteps(nSteps/10);
        sim.setHarmonicWV(comparedWV);
        sim.getController().actionPerformed();
        System.out.println("equilibration finished");
        sim.getController().reset();
       
        sim.activityIntegrate.setMaxSteps(nSteps);
        sim.getController().actionPerformed();
        
        /* 
         * This loop creates a new write class for each histogram from each 
         * AccumulatorHistogram, changes the filename for the histogram output,
         * connects the write class with this histogram, and 
         * writes out the results to the file.
         */
        int accumulatorLength = sim.hists.length;
        WriteHistograms wh;
        for(int i = 0; i < accumulatorLength; i++){
            String outputName = new String("hist_" + i);
            wh = new WriteHistograms(outputName);
            wh.setHistogram(sim.hists[i].getHistograms());
            wh.actionPerformed();
        }
        
        System.out.println("Fini.");
    }
    
    public static class SimParam extends ParameterBase {
        public int numAtoms = 32;
        public double density = 0.50;
        public int D = 1;
        public double harmonicFudge = 1.0;
        public String filename = "HR1D_";
        public String inputfilename = "input";
        public double temperature = 1.0;
        public int comparedWV = numAtoms/2;
        
        public int blockSize = 100;
        public int numSteps = 10000000;
    }

}