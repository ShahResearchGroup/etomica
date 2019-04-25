/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.cavity;

import etomica.action.BoxImposePbc;
import etomica.action.BoxInflate;
import etomica.action.activity.ActivityIntegrate;
import etomica.atom.AtomType;
import etomica.box.Box;
import etomica.config.ConfigurationLattice;
import etomica.data.*;
import etomica.data.meter.MeterPressureHard;
import etomica.data.meter.MeterRDF;
import etomica.data.meter.MeterWidomInsertion;
import etomica.data.types.DataDouble;
import etomica.data.types.DataFunction;
import etomica.data.types.DataGroup;
import etomica.graphics.DisplayPlot;
import etomica.graphics.DisplayTextBoxesCAE;
import etomica.graphics.SimulationGraphic;
import etomica.integrator.IntegratorHard;
import etomica.integrator.IntegratorListenerAction;
import etomica.lattice.LatticeCubicFcc;
import etomica.lattice.LatticeOrthorhombicHexagonal;
import etomica.nbr.list.NeighborListManager;
import etomica.nbr.list.PotentialMasterList;
import etomica.potential.P2HardSphere;
import etomica.potential.PotentialMaster;
import etomica.potential.PotentialMasterMonatomic;
import etomica.simulation.Simulation;
import etomica.space3d.Space3D;
import etomica.species.SpeciesSpheresMono;
import etomica.units.dimensions.Energy;
import etomica.units.dimensions.Null;
import etomica.util.ParameterBase;
import etomica.util.ParseArgs;

/**
 * Hard sphere simulation that allows one pair of atoms to overlap, which
 * allows the cavity function to be measured.
 *
 * @author Andrew Schultz
 */
public class HSMDWidom extends Simulation {

    public final ActivityIntegrate activityIntegrate;

    /**
     * The Box holding the atoms.
     */
    public final Box box;
    /**
     * The Integrator performing the dynamics.
     */
    public final IntegratorHard integrator;
    /**
     * The single hard-sphere species.
     */
    public final SpeciesSpheresMono species;
    /**
     * The hard-sphere potential governing the interactions.
     */
    public final P2HardSphere potential;

    public final PotentialMaster potentialMaster;

    /**
     * Makes a simulation according to the specified parameters.
     *
     * @param params Parameters as defined by the inner class CavityParam
     */
    public HSMDWidom(HSMDParam params) {

        super(Space3D.getInstance());

        species = new SpeciesSpheresMono(this, space);
        species.setIsDynamic(true);
        addSpecies(species);

        box = this.makeBox();

        double neighborRangeFac = 1.6;
        double sigma = 1.0;
        potentialMaster = params.useNeighborLists ? new PotentialMasterList(this, sigma * neighborRangeFac, space) : new PotentialMasterMonatomic(this);

        int numAtoms = params.nAtoms;

        integrator = new IntegratorHard(this, potentialMaster, box);
        integrator.setIsothermal(false);
        integrator.setTimeStep(0.005);

        activityIntegrate = new ActivityIntegrate(integrator);
        getController().addAction(activityIntegrate);

        potential = new P2HardSphere(space);
        AtomType leafType = species.getLeafType();

        potentialMaster.addPotential(potential, new AtomType[]{leafType, leafType});

        box.setNMolecules(species, numAtoms);
        BoxInflate inflater = new BoxInflate(box, space);
        inflater.setTargetDensity(params.eta * 2 * space.D() / Math.PI);
        inflater.actionPerformed();
        if (space.D() == 3) {
            new ConfigurationLattice(new LatticeCubicFcc(space), space).initializeCoordinates(box);
        } else {
            new ConfigurationLattice(new LatticeOrthorhombicHexagonal(space), space).initializeCoordinates(box);
        }

        if (params.useNeighborLists) {
            NeighborListManager nbrManager = ((PotentialMasterList) potentialMaster).getNeighborManager(box);
            integrator.getEventManager().addListener(nbrManager);
        } else {
            integrator.getEventManager().addListener(new IntegratorListenerAction(new BoxImposePbc(box, space)));
        }
    }

    public static void main(String[] args) {
        final String APP_NAME = "HSMD";

        HSMDParam params = new HSMDParam();
        if (args.length > 0) {
            ParseArgs.doParseArgs(params, args);
        } else {
            params.doGraphics = false;
            params.steps = 1000000;
        }
        final HSMDWidom sim = new HSMDWidom(params);

        int xMax = (int) (sim.box.getBoundary().getBoxSize().getX(0) * 0.5);
        if (params.mappingCut > 0 && params.mappingCut < xMax) xMax = params.mappingCut;

        MeterRDF meterRDF = new MeterRDF(sim.space);
        meterRDF.getXDataSource().setNValues(500 * xMax);
        meterRDF.getXDataSource().setXMax(xMax);
        meterRDF.setBox(sim.box);
        meterRDF.setResetAfterData(true);
        DataFork forkRDF = new DataFork();

        MeterWidomInsertion meterWidom = new MeterWidomInsertion(sim.space, sim.getRandom());
        meterWidom.setIntegrator(sim.integrator);
        meterWidom.setNInsert(params.nAtoms / 5);
        meterWidom.setSpecies(sim.species);

        DataProcessorGContactP dpPContact = new DataProcessorGContactP(sim.box);
        DataProcessorPContactG dpGContact = new DataProcessorPContactG(sim.box);

        if (params.doGraphics) {
            final SimulationGraphic simGraphic = new SimulationGraphic(sim, SimulationGraphic.TABBED_PANE, APP_NAME, 100);

            int rdfInterval = (5 * 200 + params.nAtoms - 1) / params.nAtoms;
            sim.integrator.getEventManager().addListener(new IntegratorListenerAction(meterRDF, rdfInterval));

            AccumulatorAverageFixed accRDF = new AccumulatorAverageFixed(100);
            accRDF.setPushInterval(1);
            forkRDF.addDataSink(accRDF);
            DisplayPlot gPlot = new DisplayPlot();
            accRDF.addDataSink(gPlot.getDataSet().makeDataSink(), new AccumulatorAverage.StatType[]{accRDF.AVERAGE});
            gPlot.setLabel("g(r)");
            simGraphic.add(gPlot);

            DataProcessorFit dpFit = new DataProcessorFit("g(r) fit", 100, 3, false, 1, 1.1);
            accRDF.addDataSink(dpFit, new AccumulatorAverage.StatType[]{accRDF.AVERAGE, accRDF.ERROR});
            dpFit.setDataSink(gPlot.getDataSet().makeDataSink());

            DataProcessorExtract0 gCExtractor = new DataProcessorExtract0("g(sigma)", true);
            gCExtractor.setErrorSource(dpFit);
            dpFit.addDataSink(gCExtractor);
            DisplayTextBoxesCAE gCDisplay = new DisplayTextBoxesCAE();
            gCExtractor.addDataSink(gCDisplay);
            gCDisplay.setDoShowCurrent(false);
            simGraphic.add(gCDisplay);
            DisplayTextBoxesCAE displayGCMap = new DisplayTextBoxesCAE();
            displayGCMap.setDoShowCurrent(false);
            simGraphic.add(displayGCMap);
            gCExtractor.addDataSink(dpGContact);
            DisplayTextBoxesCAE displayPfromGC = new DisplayTextBoxesCAE();
            dpGContact.addDataSink(displayPfromGC);
            displayPfromGC.setDoShowCurrent(false);
            displayPfromGC.setLabel("P from g(sigma)");


            DataPumpListener pumpRDF = new DataPumpListener(meterRDF, forkRDF, 100);
            sim.integrator.getEventManager().addListener(pumpRDF);

            MeterPressureHard meterP = new MeterPressureHard(sim.integrator);
            MeterPressureCollisionCount meterPCC = new MeterPressureCollisionCount(sim.integrator);

            AccumulatorAverageCollapsing accP = new AccumulatorAverageCollapsing(200);
            DataPumpListener pumpP = new DataPumpListener(meterP, accP, 100);
            sim.integrator.getEventManager().addListener(pumpP);
            DisplayTextBoxesCAE displayP = new DisplayTextBoxesCAE();
            displayP.setAccumulator(accP);
            displayP.setDoShowCurrent(false);
            displayP.setDoShowCorrelation(true);
            accP.addDataSink(dpPContact, new AccumulatorAverage.StatType[]{accP.MOST_RECENT, accP.AVERAGE, accP.ERROR});
            DisplayTextBoxesCAE displayContact = new DisplayTextBoxesCAE();
            dpPContact.addDataSink(displayContact);
            displayContact.setLabel("g(sigma) from P");
            displayContact.setDoShowCurrent(false);
            simGraphic.add(displayContact);
            simGraphic.add(displayP);
            simGraphic.add(displayPfromGC);
            AccumulatorAverageCollapsing accPCC = new AccumulatorAverageCollapsing(200);
            DataPumpListener pumpPCC = new DataPumpListener(meterPCC, accPCC, 100);
            sim.integrator.getEventManager().addListener(pumpPCC);
            DisplayTextBoxesCAE displayPCC = new DisplayTextBoxesCAE();
            displayPCC.setDoShowCurrent(false);
            displayPCC.setDoShowCorrelation(true);
            displayPCC.setAccumulator(accPCC);
            simGraphic.add(displayPCC);

            if (params.doMappingRDF) {
                MeterRDFMapped meterRDFMapped = new MeterRDFMapped(sim.integrator);
                meterRDFMapped.getXDataSource().setNValues(xMax * 500 + 1);
                meterRDFMapped.getXDataSource().setXMax(xMax);
                meterRDFMapped.reset();
                meterRDFMapped.setResetAfterData(true);
                sim.integrator.removeCollisionListener(meterRDFMapped);

                AccumulatorAverageFixed accRDFMapped = new AccumulatorAverageFixed(1);
                accRDFMapped.setPushInterval(1);
                DataPumpListener pumpRDFMapped = new DataPumpListener(meterRDFMapped, accRDFMapped, 10000);
                accRDFMapped.addDataSink(gPlot.getDataSet().makeDataSink(), new AccumulatorAverage.StatType[]{accRDFMapped.AVERAGE});
                sim.integrator.getEventManager().addListener(pumpRDFMapped);

                DataProcessorErrorBar rdfMappedError = new DataProcessorErrorBar("mapped g(r)+");
                accRDFMapped.addDataSink(rdfMappedError, new AccumulatorAverage.StatType[]{accRDFMapped.AVERAGE, accRDFMapped.ERROR});
                rdfMappedError.setDataSink(gPlot.getDataSet().makeDataSink());

                DataProcessorExtract0 gMapCExtractor = new DataProcessorExtract0("mapped g(sigma)", true);
                accRDFMapped.addDataSink(gMapCExtractor, new AccumulatorAverage.StatType[]{accRDFMapped.MOST_RECENT, accRDFMapped.AVERAGE, accRDFMapped.ERROR});
                gMapCExtractor.addDataSink(displayGCMap);
                simGraphic.getController().getDataStreamPumps().add(pumpRDFMapped);
            }

            simGraphic.getController().getDataStreamPumps().add(pumpRDF);
            simGraphic.getController().getDataStreamPumps().add(pumpP);

            if (params.doWidom) {
                AccumulatorAverageCollapsing accWidom = new AccumulatorAverageCollapsing(200);
                DataPumpListener pumpWidom = new DataPumpListener(meterWidom, accWidom, 10);
                DisplayTextBoxesCAE displayWidom = new DisplayTextBoxesCAE();
                displayWidom.setAccumulator(accWidom);
                displayWidom.setDoShowCurrent(false);
                displayWidom.setDoShowCorrelation(true);
                simGraphic.add(displayWidom);
                sim.integrator.getEventManager().addListener(pumpWidom);
                DataProcessorForked widomProcessor = new DataProcessorForked() {
                    DataGroup data = new DataGroup(new DataDouble[]{new DataDouble(), new DataDouble(), new DataDouble()});

                    @Override
                    protected IData processData(IData inputData) {
                        ((DataDouble) data.getData(0)).x = Double.NaN;
                        ((DataDouble) data.getData(1)).x = -Math.log(inputData.getValue(0));
                        ((DataDouble) data.getData(2)).x = inputData.getValue(1) / inputData.getValue(0);
                        return data;
                    }

                    @Override
                    protected IDataInfo processDataInfo(IDataInfo inputDataInfo) {
                        DataDouble.DataInfoDouble subDataInfo = new DataDouble.DataInfoDouble("stuff", Null.DIMENSION);
                        dataInfo = new DataGroup.DataInfoGroup("chemical potential", Energy.DIMENSION, new IDataInfo[]{subDataInfo, subDataInfo, subDataInfo});
                        dataInfo.addTag(tag);
                        return dataInfo;
                    }
                };
                accWidom.addDataSink(widomProcessor, new AccumulatorAverage.StatType[]{accWidom.AVERAGE, accWidom.ERROR});
                DisplayTextBoxesCAE displayMu = new DisplayTextBoxesCAE();
                widomProcessor.addDataSink(displayMu);
                displayMu.setLabel("Chemical Potnetial");
                displayMu.setDoShowCurrent(false);
                simGraphic.add(displayMu);
                simGraphic.getController().getDataStreamPumps().add(pumpWidom);
            }

            simGraphic.makeAndDisplayFrame(APP_NAME);
            return;
        }

        long steps = params.steps;
        sim.activityIntegrate.setMaxSteps(steps / 10);
        sim.activityIntegrate.actionPerformed();
        sim.integrator.resetStepCount();


        AccumulatorAverageFixed accRDFMapped = new AccumulatorAverageFixed(1);
        if (params.doMappingRDF) {
            MeterRDFMapped meterRDFMapped = new MeterRDFMapped(sim.integrator);
            if (params.mappingCut > 0) meterRDFMapped.setMappingCut(params.mappingCut);
            meterRDFMapped.getXDataSource().setNValues(xMax * 500 + 1);
            meterRDFMapped.getXDataSource().setXMax(xMax);
            meterRDFMapped.reset();
            meterRDFMapped.setResetAfterData(true);

            DataPumpListener pumpRDFMapped = new DataPumpListener(meterRDFMapped, accRDFMapped, (int) (steps / 100));
            sim.integrator.getEventManager().addListener(pumpRDFMapped);
        }

        AccumulatorAverageFixed accRDF = new AccumulatorAverageFixed(1);
        if (params.doRDF) {
            int rdfInterval = (30 * params.nAtoms + 199) / 200;
            if (params.rdfInterval > 0) rdfInterval = params.rdfInterval;
            int stepsPerBlock = (int) (steps / 100);
            while (rdfInterval > stepsPerBlock && (steps % (stepsPerBlock * 2) == 0)) stepsPerBlock *= 2;
            while (stepsPerBlock % rdfInterval != 0) rdfInterval--;
            System.out.println("RDF interval: " + rdfInterval);
            System.out.println("steps per block: " + stepsPerBlock);
            sim.integrator.getEventManager().addListener(new IntegratorListenerAction(meterRDF, rdfInterval));

            DataPumpListener pumpRDF = new DataPumpListener(meterRDF, accRDF, stepsPerBlock);
            sim.integrator.getEventManager().addListener(pumpRDF);
        }

        MeterPressureHard meterP = new MeterPressureHard(sim.integrator);
        MeterPressureCollisionCount meterPCC = new MeterPressureCollisionCount(sim.integrator);

        AccumulatorAverageFixed accP = new AccumulatorAverageFixed(1);
        DataPumpListener pumpP = new DataPumpListener(meterP, accP, (int) (steps / 100));
        sim.integrator.getEventManager().addListener(pumpP);

        AccumulatorAverageFixed accPCC = new AccumulatorAverageFixed(1);
        DataPumpListener pumpPCC = new DataPumpListener(meterPCC, accPCC, (int) (steps / 100));
        sim.integrator.getEventManager().addListener(pumpPCC);

        long t1 = System.nanoTime();
        sim.activityIntegrate.setMaxSteps(steps);
        sim.activityIntegrate.actionPerformed();
        long t2 = System.nanoTime();


        double avgP = accP.getData(accP.AVERAGE).getValue(0);
        double errP = accP.getData(accP.ERROR).getValue(0);
        double corP = accP.getData(accP.BLOCK_CORRELATION).getValue(0);
        System.out.println("Pressure: " + avgP + " err: " + errP + " cor: " + corP);

        double avgPCC = accPCC.getData(accPCC.AVERAGE).getValue(0);
        double errPCC = accPCC.getData(accPCC.ERROR).getValue(0);
        double corPCC = accPCC.getData(accPCC.BLOCK_CORRELATION).getValue(0);
        System.out.println("Pressure(CC): " + avgPCC + " err: " + errPCC + " cor: " + corPCC);

        if (params.doRDF) {
            IData rData = ((DataFunction.DataInfoFunction) ((DataGroup.DataInfoGroup) accRDF.getDataInfo()).getSubDataInfo(0)).getXDataSource().getIndependentData(0);
            IData rdfDataAvg = accRDF.getData(accRDF.AVERAGE);
            IData rdfDataErr = accRDF.getData(accRDF.ERROR);
            System.out.println("\nRDF");
            for (int i = 0; i < rData.getLength(); i++) {
                System.out.println(rData.getValue(i) + " " + rdfDataAvg.getValue(i) + " " + rdfDataErr.getValue(i));
            }
        }
        if (params.doMappingRDF) {
            IData rData = ((DataFunction.DataInfoFunction) ((DataGroup.DataInfoGroup) accRDFMapped.getDataInfo()).getSubDataInfo(0)).getXDataSource().getIndependentData(0);
            IData rdfDataAvg = accRDFMapped.getData(accRDFMapped.AVERAGE);
            IData rdfDataErr = accRDFMapped.getData(accRDFMapped.ERROR);
            System.out.println("\nmapped RDF");
            for (int i = 0; i < rData.getLength(); i++) {
                System.out.println(rData.getValue(i) + " " + rdfDataAvg.getValue(i) + " " + rdfDataErr.getValue(i));
            }
        }

        System.out.println("\ntime: " + (t2 - t1) / 1e9);

    }

    /**
     * Inner class for parameters understood by the HSMDCavity constructor
     */
    public static class HSMDParam extends ParameterBase {
        public long steps = 1000000;
        public int nAtoms = 256;
        public double eta = 0.35;
        public int mappingCut = 0;
        public boolean useNeighborLists = true;
        public boolean doGraphics = false;
        public boolean doWidom = false;
        public boolean doRDF = false;
        public int rdfInterval = 0;
        public boolean doMappingRDF = false;
    }

}
