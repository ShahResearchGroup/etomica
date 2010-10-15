package etomica.models.oneDHardRods;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import etomica.action.activity.ActivityIntegrate;
import etomica.api.IAtomList;
import etomica.api.IAtomType;
import etomica.api.IBox;
import etomica.api.ISimulation;
import etomica.api.IVector;
import etomica.box.Box;
import etomica.data.AccumulatorRatioAverage;
import etomica.data.DataPump;
import etomica.data.IEtomicaDataSource;
import etomica.data.meter.MeterPotentialEnergy;
import etomica.data.types.DataDoubleArray;
import etomica.data.types.DataGroup;
import etomica.exception.ConfigurationOverlapException;
import etomica.graphics.SimulationGraphic;
import etomica.integrator.IntegratorMC;
import etomica.lattice.crystal.Basis;
import etomica.lattice.crystal.BasisCubicFcc;
import etomica.lattice.crystal.Primitive;
import etomica.lattice.crystal.PrimitiveCubic;
import etomica.lattice.crystal.PrimitiveOrthorhombic;
import etomica.listener.IntegratorListenerAction;
import etomica.nbr.list.PotentialMasterList;
import etomica.normalmode.BasisBigCell;
import etomica.normalmode.CoordinateDefinition;
import etomica.normalmode.CoordinateDefinitionLeaf;
import etomica.normalmode.MCMoveAtomCoupled;
import etomica.normalmode.NormalModes;
import etomica.normalmode.NormalModesFromFile;
import etomica.normalmode.WaveVectorFactory;
import etomica.potential.P2SoftSphere;
import etomica.potential.P2SoftSphericalTruncated;
import etomica.potential.Potential2SoftSpherical;
import etomica.simulation.Simulation;
import etomica.space.Boundary;
import etomica.space.BoundaryRectangularPeriodic;
import etomica.space.Space;
import etomica.space3d.Vector3D;
import etomica.species.SpeciesSpheresMono;
import etomica.units.Null;
import etomica.util.ParameterBase;
import etomica.util.ReadParameters;
import etomica.virial.overlap.AccumulatorVirialOverlapSingleAverage;
import etomica.virial.overlap.DataSourceVirialOverlap;
import etomica.virial.overlap.IntegratorOverlap;

/**
 * MC simulation
 * 3D Lennard Jones
 * FCC crystal
 * No graphic display
 * uses single big cell approach
 * Calculate free energy of solid using normal mode insertion method
 * 
 * Treats coupling of modes as overlap variable; 
 * 
 * Generate input files with HarmonicCrystalSoftSphereFCC
 * 
 * Uses overlap sampling.
 */

/*
 * Starts in notes 5/21/10
 */
public class SimDifferentImageSsFccBigCell extends Simulation {

    private static final long serialVersionUID = 1L;
    private static final String APP_NAME = "SimDifferentImageFCC";
    public Primitive primitive;
    NormalModes nmRef, nmTarg;
    public Basis basis;
    public ActivityIntegrate activityIntegrate;
    public CoordinateDefinition cDefTarget, cDefRef;
    WaveVectorFactory waveVectorFactoryRef, waveVectorFactoryTarg;
    
    double bennettParam;       //adjustable parameter - Bennett's parameter
    public IntegratorOverlap integratorSim; //integrator for the whole simulation
    public DataSourceVirialOverlap dsvo;
    IntegratorMC[] integrators;
    public AccumulatorVirialOverlapSingleAverage[] accumulators;
    public DataPump[] accumulatorPumps;
    public IEtomicaDataSource[] meters;
    public IBox boxTarget, boxRef;
    public Boundary bdryTarget, bdryRef;
    MeterPotentialEnergy meterTargInTarg, meterRef, meterRefInRef;
    MeterDifferentImageAdd meterTargInRef;
    MeterDifferentImageSubtract meterRefInTarg;
    
    double refSumWVC, targSumWVC;
    int targModesCt, refModeCt;
    
    
    public SimDifferentImageSsFccBigCell(Space _space, int[] nCellsRef, 
            int[] nCellsTarget, double density, int blocksize, 
            double tems, int exponent, String inputFile) {
        super(_space);
        System.out.println("Running " + APP_NAME);
        
//        long seed = 0;
//        System.out.println("Seed explicitly set to " + seed);
//        IRandom rand = new RandomNumberGenerator(seed);
//        this.setRandom(rand);
        
        int targAtoms = 1;
        int refAtoms = 1;
        for(int i = 0; i < space.D(); i++){
            refAtoms *= nCellsRef[i];
            targAtoms *= nCellsTarget[i];
        }
        refAtoms *= 4;     //definitely fcc
        targAtoms *= 4;    //definitely fcc
        
        double temperature = tems;
        String rIn = inputFile + refAtoms;
        String tIn = inputFile + targAtoms;
        
        SpeciesSpheresMono species = new SpeciesSpheresMono(this, space);
        addSpecies(species);
        
        integrators = new IntegratorMC[2];
        accumulatorPumps = new DataPump[2];
        meters = new IEtomicaDataSource[2];
        accumulators = new AccumulatorVirialOverlapSingleAverage[2];
        
//REFERENCE
        // Set up reference system - B, 0
        boxRef = new Box(space);
        addBox(boxRef);
        boxRef.setNMolecules(species, refAtoms);
        
        double primitiveLength = Math.pow(4.0 / density, 1.0 / 3.0);
        bdryRef = new BoundaryRectangularPeriodic(space, 1.0);
        IVector edges = new Vector3D();
        double[] lengths = new double[3];
        lengths[0] = nCellsRef[0]*primitiveLength;
        lengths[1] = nCellsRef[1]*primitiveLength;
        lengths[2] = nCellsRef[2]*primitiveLength;
        ((Vector3D)edges).E(lengths);
        bdryRef.setBoxSize(edges);
        boxRef.setBoundary(bdryRef);
        primitive = new PrimitiveOrthorhombic(space, lengths[0], lengths[1],
                lengths[2]);
        
        Basis basisFCC = new BasisCubicFcc();
        basis = new BasisBigCell(space, basisFCC, nCellsRef);
        
        cDefRef = new CoordinateDefinitionLeaf(boxRef, primitive, basis, space);
        cDefRef.initializeCoordinates(new int[] { 1, 1, 1 });
        
        PotentialMasterList potentialMaster = new PotentialMasterList(this, space);
        //Choose the smallest side to define the neighborRange.
        double neighborRange = 0.0;
        if(nCellsRef[0] <= nCellsRef[1] && nCellsRef[0] <= nCellsRef[2]){
            neighborRange = 0.495 * lengths[0];
        } else if(nCellsRef[1] <= nCellsRef[2]) {
            neighborRange = 0.495 * lengths[1];
        }else {
            neighborRange = 0.495 * lengths[2];
        }
        
        Potential2SoftSpherical potentialBase = new P2SoftSphere(space, 1.0, 
                1.0, exponent);
        P2SoftSphericalTruncated potential = new P2SoftSphericalTruncated(
                space, potentialBase, neighborRange);
        potentialMaster.addPotential(potential, new IAtomType[] {
                species.getLeafType(), species.getLeafType()});
        potentialMaster.setRange(neighborRange);
        potentialMaster.lrcMaster().setEnabled(false);
        potentialMaster.getNeighborManager(boxRef).reset();
        
        IntegratorMC integratorRef = new IntegratorMC(potentialMaster, random, temperature);
        integratorRef.setBox(boxRef);
        integrators[0] = integratorRef;
        
        nmRef = new NormalModesFromFile(rIn, space.D());
        nmRef.setHarmonicFudge(1.0);
//        nmRef.setTemperature(temperature);  //not needed - deriv based
        double[][] omega = nmRef.getOmegaSquared();
        waveVectorFactoryRef = nmRef.getWaveVectorFactory();
        waveVectorFactoryRef.makeWaveVectors(boxRef);
        double[] wvc= nmRef.getWaveVectorFactory().getCoefficients();
        
        System.out.println("We have " + waveVectorFactoryRef.getWaveVectors().length
                +" reference wave vectors.");
        
        meterRefInRef = new MeterPotentialEnergy(potentialMaster);
        meterRefInRef.setBox(boxRef);
        double latticeEnergyRef = meterRefInRef.getDataAsScalar();
        System.out.println("Reference system lattice energy: " +latticeEnergyRef);
        
        MeterPotentialEnergy meterPE = new MeterPotentialEnergy(potentialMaster);
        meterPE.setBox(boxRef);
        MCMoveAtomCoupled mcMoveAtom = new MCMoveAtomCoupled(potentialMaster,
                meterPE, random, space);
        mcMoveAtom.setPotential(potential);
        mcMoveAtom.setBox(boxRef);
        mcMoveAtom.setStepSize(0.01);
        integratorRef.getMoveManager().addMCMove(mcMoveAtom);
        integratorRef.setMeterPotentialEnergy(meterRefInRef);
        
        
//TARGET
        // Set up target system
        boxTarget = new Box(space);
        addBox(boxTarget);
        boxTarget.setNMolecules(species, targAtoms);
        
        bdryTarget = new BoundaryRectangularPeriodic(space, 1.0);
        edges = new Vector3D();
        lengths = new double[3];
        lengths[0] = nCellsTarget[0]*primitiveLength;
        lengths[1] = nCellsTarget[1]*primitiveLength;
        lengths[2] = nCellsTarget[2]*primitiveLength;
        ((Vector3D)edges).E(lengths);
        bdryTarget.setBoxSize(edges);
        boxTarget.setBoundary(bdryTarget);
        primitive = new PrimitiveOrthorhombic(space, lengths[0], lengths[1],
                lengths[2]);
        basis = new BasisBigCell(space, basisFCC, nCellsTarget);
        
        cDefTarget = new CoordinateDefinitionLeaf(boxTarget, primitive, basis, space);
        cDefTarget.initializeCoordinates(new int[] {1, 1, 1});
        
        potentialMaster.getNeighborManager(boxTarget).reset();
        
        IntegratorMC integratorTarget = new IntegratorMC(potentialMaster,
                random, temperature);
        integrators[1] = integratorTarget;
        integratorTarget.setBox(boxTarget);
        
        nmTarg = new NormalModesFromFile(tIn, space.D());
        nmTarg.setHarmonicFudge(1.0);
//        nmTarg.setTemperature(temperature);  // notneeded, deriv based
        omega = nmTarg.getOmegaSquared();
        waveVectorFactoryTarg = nmTarg.getWaveVectorFactory();
        waveVectorFactoryTarg.makeWaveVectors(boxTarget);
        wvc = nmTarg.getWaveVectorFactory().getCoefficients();
        
        System.out.println("We have " + waveVectorFactoryTarg.getWaveVectors().length 
                +" target wave vectors.");
        
        meterTargInTarg = new MeterPotentialEnergy(potentialMaster);
        meterTargInTarg.setBox(boxTarget);
        double latticeEnergyTarget = meterTargInTarg.getDataAsScalar();
        System.out.println("Target system lattice energy: " +latticeEnergyTarget);
        
        meterPE = new MeterPotentialEnergy(potentialMaster);
        meterPE.setBox(boxTarget);
        mcMoveAtom = new MCMoveAtomCoupled(potentialMaster, meterPE,random, 
                space);
        mcMoveAtom.setPotential(potential);
        
        mcMoveAtom.setBox(boxTarget);
        mcMoveAtom.setStepSize(0.01);
        integratorTarget.getMoveManager().addMCMove(mcMoveAtom);
        integratorTarget.setMeterPotentialEnergy(meterTargInTarg);
        
        
//JOINT
        //measuring potential of target in reference system
        meterTargInRef = new MeterDifferentImageAdd((ISimulation)this, space, 
                temperature, cDefRef, nmRef, cDefTarget, potentialMaster, 
                new int[] {1,1,1,}, nmTarg, tIn);
        MeterOverlapSameGaussian meterOverlapInRef = new 
                MeterOverlapSameGaussian("MeterOverlapInB", Null.DIMENSION, 
                meterRefInRef, meterTargInRef, temperature);
        meterOverlapInRef.setDsABase(latticeEnergyRef);
        meterOverlapInRef.setDsBBase(latticeEnergyTarget);
        
        //measuring reference potential in target system
        meterRefInTarg = new MeterDifferentImageSubtract(this, space, cDefTarget,
                nmTarg, cDefRef, potentialMaster, new int[] {1,1,1}, nmRef, rIn);
        MeterOverlap meterOverlapInTarget = new MeterOverlap("MeterOverlapInA", 
                Null.DIMENSION, meterTargInTarg, meterRefInTarg, temperature);
        meterOverlapInTarget.setDsABase(latticeEnergyTarget);
        meterOverlapInTarget.setDsBBase(latticeEnergyRef);
        
        //Just to be sure!
        potential.setTruncationRadius(3000.0);
        
        meters[1] = meterOverlapInTarget;
        meters[0] = meterOverlapInRef;
        
        //Set up the rest of the joint stuff
        
        integratorSim = new IntegratorOverlap(new IntegratorMC[]{integratorRef,
                integratorTarget});
        
        setAccumulator(new AccumulatorVirialOverlapSingleAverage(10, 11, true), 0);
        setAccumulator(new AccumulatorVirialOverlapSingleAverage(10, 11, false), 1);
        
        setBennettParameter(1.0, 30);
        
        activityIntegrate = new ActivityIntegrate(integratorSim, 0, true);
        getController().addAction(activityIntegrate);
    }
    
    public void setBennettParameter(double benParamCenter, double span) {
        bennettParam = benParamCenter;
        accumulators[0].setBennetParam(benParamCenter,span);
        accumulators[1].setBennetParam(benParamCenter,span);
    }
    
    public void setBennettParameter(double newBennettParameter) {
        System.out.println("setting ref pref (explicitly) to "+
                newBennettParameter);
        setAccumulator(new AccumulatorVirialOverlapSingleAverage(1,true),0);
        setAccumulator(new AccumulatorVirialOverlapSingleAverage(1,false),1);
        setBennettParameter(newBennettParameter,1);
        
    }
    
    public void initBennettParameter(String fileName, int initSteps, int initBlockSize) {
        // benParam = -1 indicates we are searching for an appropriate value
        bennettParam = -1.0;
        integratorSim.getMoveManager().setEquilibrating(true);
        
        if (fileName != null) {
            try { 
                FileReader fileReader = new FileReader(fileName);
                BufferedReader bufReader = new BufferedReader(fileReader);
                String benParamString = bufReader.readLine();
                bennettParam = Double.parseDouble(benParamString);
                bufReader.close();
                fileReader.close();
                System.out.println("setting ref pref (from file) to "+bennettParam);
                setAccumulator(new AccumulatorVirialOverlapSingleAverage(1,true),0);
                setAccumulator(new AccumulatorVirialOverlapSingleAverage(1,false),1);
                setBennettParameter(bennettParam,1);
            }
            catch (IOException e) {
                System.out.println("Bennett parameter not from file");
                // file not there, which is ok.
            }
        }
        
        if (bennettParam == -1) {
            
            // equilibrate off the lattice to avoid anomolous contributions
            activityIntegrate.setMaxSteps(initSteps);
            
            getController().actionPerformed();
            getController().reset();

            setAccumulator(new AccumulatorVirialOverlapSingleAverage(initBlockSize,41,true),0);
            setAccumulator(new AccumulatorVirialOverlapSingleAverage(initBlockSize,41,false),1);
            setBennettParameter(1,10);
            activityIntegrate.setMaxSteps(initSteps);
            
            getController().actionPerformed();
            getController().reset();

            int newMinDiffLoc = dsvo.minDiffLocation();
            bennettParam = accumulators[0].getBennetAverage(newMinDiffLoc)
                /accumulators[1].getBennetAverage(newMinDiffLoc);
            
            if (Double.isNaN(bennettParam) || bennettParam == 0 || 
                    Double.isInfinite(bennettParam)) {
                throw new RuntimeException("Simulation failed to find a valid ref pref");
            }
            System.out.println("setting ref pref to "+bennettParam);
            
            setAccumulator(new AccumulatorVirialOverlapSingleAverage(11,true),0);
            setAccumulator(new AccumulatorVirialOverlapSingleAverage(11,false),1);
            setBennettParameter(bennettParam,2);
            
            // set benParam back to -1 so that later on we know that we've been looking for
            // the appropriate value
            bennettParam = -1;
            getController().reset();
        }
        integratorSim.getMoveManager().setEquilibrating(false);
    }
    
    public void setAccumulator(AccumulatorVirialOverlapSingleAverage 
            newAccumulator, int iBox) {
        accumulators[iBox] = newAccumulator;
        if (accumulatorPumps[iBox] == null) {
            accumulatorPumps[iBox] = new DataPump(meters[iBox], newAccumulator);
            IntegratorListenerAction pumpListener = new IntegratorListenerAction(accumulatorPumps[iBox]);
            pumpListener.setInterval(getBox(iBox).getLeafList().getAtomCount());
            integrators[iBox].getEventManager().addListener(pumpListener);
        }
        else {
            accumulatorPumps[iBox].setDataSink(newAccumulator);
        }
        if (integratorSim != null && accumulators[0] != null && 
                accumulators[1] != null) {
            dsvo = new DataSourceVirialOverlap(accumulators[0],accumulators[1]);
            integratorSim.setDSVO(dsvo);
        }
        
    }
    
    public void setAccumulatorBlockSize(int newBlockSize) {
        for (int i=0; i<2; i++) {
            accumulators[i].setBlockSize(newBlockSize);
        }
        try {
            // reset the integrator so that it will re-adjust step frequency
            // and ensure it will take enough data for both ref and target
            integratorSim.reset();
        }
        catch (ConfigurationOverlapException e) { /* meaningless */ }
    }
    public void equilibrate(String fileName, int initSteps, int initBlockSize) {
        // run a short simulation to get reasonable MC Move step sizes and
        // (if needed) narrow in on a reference preference
        activityIntegrate.setMaxSteps(initSteps);
        
        integratorSim.getMoveManager().setEquilibrating(true);
        
        for (int i=0; i<2; i++) {
            if (integrators[i] instanceof IntegratorMC) {
                ((IntegratorMC)integrators[i]).getMoveManager().setEquilibrating(true);
            }
        }
        getController().actionPerformed();
        getController().reset();
        for (int i=0; i<2; i++) {
            if (integrators[i] instanceof IntegratorMC) {
                ((IntegratorMC)integrators[i]).getMoveManager().setEquilibrating(false);
            }
        }
        
        if (bennettParam == -1) {
            int newMinDiffLoc = dsvo.minDiffLocation();
            bennettParam = accumulators[0].getBennetAverage(newMinDiffLoc)
                /accumulators[1].getBennetAverage(newMinDiffLoc);
            System.out.println("setting ref pref to "+bennettParam+" ("+newMinDiffLoc+")");
            setAccumulator(new AccumulatorVirialOverlapSingleAverage(initBlockSize,1,true),0);
            setAccumulator(new AccumulatorVirialOverlapSingleAverage(initBlockSize,1,false),1);
            
            setBennettParameter(bennettParam,1);
            if (fileName != null) {
                try {
                    FileWriter fileWriter = new FileWriter(fileName);
                    BufferedWriter bufWriter = new BufferedWriter(fileWriter);
                    bufWriter.write(String.valueOf(bennettParam)+"\n");
                    bufWriter.close();
                    fileWriter.close();
                }
                catch (IOException e) {
                    throw new RuntimeException("couldn't write to Bennet parameter file");
                }
            }
        }
        else {
            dsvo.reset();
        }
        integratorSim.getMoveManager().setEquilibrating(false);
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
        }
        
        double density = params.density;
        int D = params.D;
        double harmonicFudge = params.harmonicFudge;
        String filename = params.filename;
        if(filename.length() == 0){
            filename = "nmi_3DSS_FCC_";
        }
        String inputFile = params.inputFile;
        double temperature = params.temperature;
        int runNumSteps = params.numSteps;
        int runBlockSize = params.runBlockSize;
        int subBlockSize = params.subBlockSize;
        int eqNumSteps = params.eqNumSteps;
        int benNumSteps = params.bennettNumSteps;
        int exp = params.exponent;
        boolean first = params.first;
        int[] refCells = params.refShape;
        int[] targCells = params.targShape;
        int nRefA = 1;
        int nTargA = 1;
        for(int i = 0; i < D; i++){
            nRefA *= refCells[i];
            nTargA *= targCells[i];
        }
        nRefA *= 4;     //definitely fcc
        nTargA *= 4;    //definitely fcc
        
        filename = filename + "_" + nRefA + "_" + nTargA + "_" + temperature;
        
        // instantiate simulation
        SimDifferentImageSsFccBigCell sim = new SimDifferentImageSsFccBigCell(
                Space.getInstance(D), refCells, targCells, density, runBlockSize,
                temperature, exp, inputFile);
        System.out.println("Dimension " + sim.space.D());
        System.out.println("Temperature " + temperature);
        System.out.println("Ref system is " +nRefA + " atoms at density " + density);
        System.out.println("Targ system is " +nTargA + " atoms at density " + density);
        System.out.println("Add scaling: " + sim.meterTargInRef.getScaling());
        System.out.println("Sub scaling: " + sim.meterRefInTarg.getScaling());
        System.out.println(runNumSteps + " steps, " + runBlockSize + " blocksize");
        System.out.println("Target input data from " + inputFile);
        System.out.println("output data to " + filename);
        System.out.println("instantiated");
        
        if(false) {
            SimulationGraphic graphic = new SimulationGraphic(sim, sim.space, 
                    sim.getController());
            graphic.makeAndDisplayFrame();
            return;
        }
        
        //Divide out all the steps, so that the subpieces have the proper # of steps
        runNumSteps /= (int)subBlockSize;
        eqNumSteps /= (int)subBlockSize;
        benNumSteps /= subBlockSize;
        
        //start simulation & equilibrate
        sim.integratorSim.getMoveManager().setEquilibrating(true);
        sim.integratorSim.setNumSubSteps(subBlockSize);
        
        if(first){
            System.out.println("Init Bennett");
            sim.initBennettParameter(filename, benNumSteps, runBlockSize);
            if(Double.isNaN(sim.bennettParam) || sim.bennettParam == 0 || 
                    Double.isInfinite(sim.bennettParam)){
                throw new RuntimeException("Simulation failed to find a valid " +
                        "Bennett parameter");
            }
            
            System.out.println("equilibrate");
            sim.equilibrate("bennett" , eqNumSteps, runBlockSize);
            if(Double.isNaN(sim.bennettParam) || sim.bennettParam == 0 || 
                    Double.isInfinite(sim.bennettParam)){
                throw new RuntimeException("Simulation failed to find a valid " +
                        "Bennett parameter");
            }
            System.out.println("equilibration finished.");
        } else {
            System.out.println("Init Bennett");
            sim.initBennettParameter("bennett", benNumSteps, runBlockSize);
            System.out.println("equilibrate");
            sim.equilibrate(null, eqNumSteps, runBlockSize);
            System.out.println("equilibration finished.");
        }
        
        // start simulation
        sim.setAccumulatorBlockSize((int)runBlockSize);
        sim.integratorSim.getMoveManager().setEquilibrating(false);
        sim.activityIntegrate.setMaxSteps(runNumSteps);
        sim.getController().actionPerformed();
        System.out.println("final reference optimal step frequency " + 
                sim.integratorSim.getStepFreq0() + " (actual: " + 
                sim.integratorSim.getActualStepFreq0() + ")");
        
        
        //CALCULATION OF HARMONIC ENERGY
        
        double[] ratioAndError = sim.dsvo.getOverlapAverageAndError();
        double ratio = ratioAndError[0];
        double error = ratioAndError[1];
        System.out.println("ratio average: "+ratio+", error: "+error);
        DataGroup allYourBase = 
            (DataGroup)sim.accumulators[0].getData(sim.dsvo.minDiffLocation());
        System.out.println("reference ratio average (unscaled): " + 
                ((DataDoubleArray)allYourBase.getData(AccumulatorRatioAverage.
                        StatType.RATIO.index)).getData()[1] + " error: " + 
                ((DataDoubleArray)allYourBase.getData(AccumulatorRatioAverage.
                        StatType.RATIO_ERROR.index)).getData()[1]);
        
        allYourBase = (DataGroup)sim.accumulators[1].getData(sim.accumulators[1]
                .getNBennetPoints() - sim.dsvo.minDiffLocation()-1);
        System.out.println("target ratio average (unscaled): " + 
                ((DataDoubleArray)allYourBase.getData(AccumulatorRatioAverage.
                        StatType.RATIO.index)).getData()[1]
                 + " error: " + 
                ((DataDoubleArray)allYourBase.getData(AccumulatorRatioAverage.
                        StatType.RATIO_ERROR.index)).getData()[1]);
        
        System.out.println("calculated diff " + (temperature*
                (-Math.log(ratio) 
                - sim.meterTargInRef.getScaling() 
                - 0.5 * sim.space.D() * (nTargA - nRefA) * Math.log(2*Math.PI*temperature) 
                - 0.5 * sim.space.D() * Math.log(nTargA)
                + 0.5 * sim.space.D() * Math.log(nRefA))));
        
        System.out.println("Fini.");
    }
    
    public static class SimParam extends ParameterBase {
        public boolean first = true;
        public int[] refShape = {2, 2, 2};
        public int[] targShape = {2, 2, 4};
        public double density = 1.1964;
        public int D = 3;
        public double harmonicFudge = 1.0;
        public double temperature = 0.01;
        public int exponent = 12;
        
        public String inputFile = "inputSSDB_BC";
        public String filename = "output";
        
        public int numSteps = 100000000;
        public int runBlockSize = 10000;
        public int subBlockSize = 10000;    //# of steps in subintegrator per integrator step
        
        public int eqNumSteps = 100000;  
        public int bennettNumSteps = 50000;
    }
}