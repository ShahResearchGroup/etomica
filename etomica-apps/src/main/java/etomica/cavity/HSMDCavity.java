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
import etomica.data.history.HistoryCollapsingDiscard;
import etomica.data.meter.MeterRDF;
import etomica.data.types.DataDouble;
import etomica.data.types.DataDoubleArray;
import etomica.data.types.DataFunction;
import etomica.data.types.DataGroup;
import etomica.graphics.DisplayPlot;
import etomica.graphics.SimulationGraphic;
import etomica.integrator.IntegratorHard;
import etomica.integrator.IntegratorListenerAction;
import etomica.lattice.LatticeCubicFcc;
import etomica.lattice.LatticeOrthorhombicHexagonal;
import etomica.math.numerical.PolynomialFit;
import etomica.nbr.list.NeighborListManager;
import etomica.nbr.list.PotentialMasterList;
import etomica.potential.PotentialMaster;
import etomica.potential.PotentialMasterMonatomic;
import etomica.simulation.Simulation;
import etomica.space3d.Space3D;
import etomica.species.SpeciesSpheresMono;
import etomica.units.dimensions.Length;
import etomica.units.dimensions.Null;
import etomica.util.ParameterBase;
import etomica.util.ParseArgs;

/**
 * Hard sphere simulation that allows one pair of atoms to overlap, which
 * allows the cavity function to be measured.
 *
 * @author Andrew Schultz
 */
public class HSMDCavity extends Simulation {

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
    public final P2HardSphereCavity potential;

    public final PotentialMaster potentialMaster;

    /**
     * Makes a simulation according to the specified parameters.
     *
     * @param params Parameters as defined by the inner class CavityParam
     */
    public HSMDCavity(CavityParam params) {

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

        ActivityIntegrate activityIntegrate = new ActivityIntegrate(integrator);
        getController().addAction(activityIntegrate);

        potential = new P2HardSphereCavity(space);
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
        final String APP_NAME = "HSMDCavity";

        CavityParam params = new CavityParam();
        if (args.length > 0) {
            ParseArgs.doParseArgs(params, args);
        } else {
            params.doGraphics = true;
        }
        final HSMDCavity sim = new HSMDCavity(params);

        MeterRDF meterRDF = new MeterRDF(sim.space);
        meterRDF.getXDataSource().setNValues(1000);
        meterRDF.getXDataSource().setXMax(2);
        meterRDF.setBox(sim.box);
        meterRDF.setResetAfterData(true);
        DataFork forkRDF = new DataFork();

        DataProcessorCavity cavityProcessor = new DataProcessorCavity(sim.integrator);
        MeterCavityMapped meterCavityMapped = new MeterCavityMapped(sim.integrator);
        meterCavityMapped.setResetAfterData(true);

        if (params.doGraphics) {
            final SimulationGraphic simGraphic = new SimulationGraphic(sim, SimulationGraphic.TABBED_PANE, APP_NAME);

            ColorSchemePaired colorScheme = new ColorSchemePaired(sim.potential);
            simGraphic.getDisplayBox(sim.box).setColorScheme(colorScheme);

            sim.integrator.getEventManager().addListener(new IntegratorListenerAction(meterRDF, 10));
            sim.integrator.addCollisionListener(meterCavityMapped);
            DisplayPlot cavityPlot = new DisplayPlot();
            cavityPlot.getDataSet().setUpdatingOnAnyChange(true);
            cavityPlot.getPlot().setYLog(true);
            forkRDF.addDataSink(cavityProcessor);

            AccumulatorAverageFixed accMapped = new AccumulatorAverageFixed(1);
            accMapped.setPushInterval(1);
            DataPumpListener pumpCavityMapped = new DataPumpListener(meterCavityMapped, accMapped, 100000);
            accMapped.addDataSink(cavityPlot.getDataSet().makeDataSink(), new AccumulatorAverage.StatType[]{accMapped.AVERAGE});
            DataProcessor mappedErr = new DataProcessorErrorBar("mapped y(r)+e");
            accMapped.addDataSink(mappedErr, new AccumulatorAverage.StatType[]{accMapped.AVERAGE, accMapped.ERROR});
            mappedErr.setDataSink(cavityPlot.getDataSet().makeDataSink());

            AccumulatorAverageFixed accRDF = new AccumulatorAverageFixed(1);
            accRDF.setPushInterval(1);
            forkRDF.addDataSink(accRDF);
            accRDF.addDataSink(cavityPlot.getDataSet().makeDataSink(), new AccumulatorAverage.StatType[]{accRDF.AVERAGE});

            AccumulatorAverageFixed accConv = new AccumulatorAverageFixed(1);
            accConv.setPushInterval(1);
            DataFork cavityFork = new DataFork();
            cavityProcessor.setDataSink(cavityFork);
            cavityFork.addDataSink(accConv);
            DataProcessorExtrapolation processorFit = new DataProcessorExtrapolation("yFit(r)", meterRDF.getXDataSource().getNValues() / 2 - 1, 4, true);
            accConv.addDataSink(processorFit, new AccumulatorAverage.StatType[]{accConv.AVERAGE, accConv.ERROR});
            processorFit.addDataSink(cavityPlot.getDataSet().makeDataSink());
            accConv.addDataSink(cavityPlot.getDataSet().makeDataSink(), new AccumulatorAverage.StatType[]{accConv.AVERAGE});
            DataProcessor convErr = new DataProcessorErrorBar("y(r)+e");
            accConv.addDataSink(convErr, new AccumulatorAverage.StatType[]{accConv.AVERAGE, accConv.ERROR});
            convErr.setDataSink(cavityPlot.getDataSet().makeDataSink());

            DataPumpListener pumpRDF = new DataPumpListener(meterRDF, forkRDF, 100000);
            sim.integrator.getEventManager().addListener(pumpRDF);
            sim.integrator.getEventManager().addListener(pumpCavityMapped);
            cavityPlot.setLabel("y(r)");

            DisplayPlot y0Plot = new DisplayPlot();
            DataProcessorExtract0 y0Extractor = new DataProcessorExtract0("conv");
            processorFit.addDataSink(y0Extractor);
            DataSourceCountTime dsTime = new DataSourceCountTime(sim.integrator);
            AccumulatorHistory y0History = new AccumulatorHistory(new HistoryCollapsingDiscard());
            y0History.setTimeDataSource(dsTime);
            y0Extractor.addDataSink(y0History);
            y0History.addDataSink(y0Plot.getDataSet().makeDataSink());
            y0Plot.setLabel("y(0)");
            DataProcessorExtract0 map0Extractor = new DataProcessorExtract0("mapped");
            accMapped.addDataSink(map0Extractor, new AccumulatorAverage.StatType[]{accMapped.AVERAGE});
            AccumulatorHistory map0History = new AccumulatorHistory(new HistoryCollapsingDiscard());
            map0History.setTimeDataSource(dsTime);
            map0Extractor.addDataSink(map0History);
            map0History.addDataSink(y0Plot.getDataSet().makeDataSink());

            simGraphic.add(cavityPlot);
            simGraphic.add(y0Plot);

            simGraphic.makeAndDisplayFrame(APP_NAME);

            simGraphic.getController().getDataStreamPumps().add(pumpRDF);
            simGraphic.getController().getDataStreamPumps().add(pumpCavityMapped);
        }
    }

    /**
     * Inner class for parameters understood by the HSMDCavity constructor
     */
    public static class CavityParam extends ParameterBase {
        /**
         * Number of atoms, default = 256
         */
        public int nAtoms = 256;
        /**
         * Packing fraction, default = 0.35
         */
        public double eta = 0.35;
        /**
         * Flag indicating whether neighbor list is to be used, default = true
         */
        public boolean useNeighborLists = true;

        public boolean doGraphics = false;
    }

    private static class DataProcessorErrorBar extends DataProcessor {
        protected DataFunction data;
        protected String label;

        public DataProcessorErrorBar(String label) {
            this.label = label;
        }

        @Override
        protected IData processData(IData inputData) {
            IData avg = ((DataGroup) inputData).getData(0);
            IData err = ((DataGroup) inputData).getData(1);
            double[] y = data.getData();
            for (int i = 0; i < y.length; i++) {
                y[i] = avg.getValue(i) + err.getValue(i);
            }
            return data;
        }

        @Override
        protected IDataInfo processDataInfo(IDataInfo inputDataInfo) {
            inputDataInfo = ((DataGroup.DataInfoGroup) inputDataInfo).getSubDataInfo(0);
            data = (DataFunction) inputDataInfo.makeData();
            IDataInfoFactory factory = inputDataInfo.getFactory();
            factory.setLabel(label);
            dataInfo = factory.makeDataInfo();
            dataInfo.addTag(tag);
            return dataInfo;
        }
    }

    private static class DataProcessorExtrapolation extends DataProcessorForked {
        protected String label;
        protected final int order;
        protected final boolean log;
        protected final double[] x, y, w;
        protected final DataFunction data;
        protected DataDoubleArray xData;

        public DataProcessorExtrapolation(String label, int nPoints, int order, boolean log) {
            this.label = label;
            this.order = order;
            this.log = log;
            x = new double[nPoints];
            y = new double[nPoints];
            w = new double[nPoints];
            DataSourceUniform xDataSource = new DataSourceUniform();
            xDataSource.setTypeMin(DataSourceUniform.LimitType.INCLUSIVE);
            xDataSource.setTypeMax(DataSourceUniform.LimitType.INCLUSIVE);
            xDataSource.setNValues(101);
            xDataSource.setXMin(0);
            xDataSource.setXMax(1);
            data = new DataFunction(new int[]{xDataSource.getNValues()});
            double[] xOut = ((DataDoubleArray) xDataSource.getData()).getData();
            DataSourceIndependentSimple rData = new DataSourceIndependentSimple(xOut, new DataDoubleArray.DataInfoDoubleArray("r", Length.DIMENSION, new int[]{xOut.length}));
            dataInfo = new DataFunction.DataInfoFunction(label, Null.DIMENSION, rData);
            dataInfo.addTag(tag);
        }

        @Override
        protected IData processData(IData inputData) {
            IData yData = ((DataGroup) inputData).getData(0);
            IData eData = ((DataGroup) inputData).getData(1);

            int i = 0;
            int nGood = 0;
            for (int j = 0; i < x.length && j < yData.getLength(); j++) {
                if (yData.getValue(j) == 0) continue;
                x[i] = xData.getValue(j);
                if (log) {
                    y[i] = Math.log(yData.getValue(j));
                    double ratio = eData.getValue(j) / yData.getValue(j);
                    w[i] = 1 / (ratio * ratio);
                    if (ratio > 0.2) w[i] = 0;
                } else {
                    y[i] = yData.getValue(i);
                    w[i] = 1.0 / (eData.getValue(j) * eData.getValue(j));
                }
                if (Double.isNaN(w[i]) || w[i] == 0) w[i] = 0;
                else nGood++;
                i++;
            }
            double[] yOut = data.getData();
//            System.out.println("Found "+nGood+" good points");
            if (nGood < 4) {
                for (int j = 0; j < yOut.length; j++) yOut[j] = Double.NaN;
                return data;
            }
            for (; i < x.length; i++) {
                x[i] = y[i] = w[i] = 0;
            }
            double[] poly = null;
            for (int o = 1; o <= order && o < nGood * 2; o++) {
                poly = PolynomialFit.doFit(o, x, y, w);
                double chi = PolynomialFit.getChi(x, y, w, poly);
//                System.out.println(o+" chi " + chi);
                if (chi < 1) break;
            }
            DataDoubleArray rData = ((DataFunction.DataInfoFunction) dataInfo).getXDataSource().getIndependentData(0);
            for (int j = 0; j < data.getLength(); j++) {
                double r = rData.getValue(j);
                yOut[j] = 0;
                double rPow = 1;
                for (int k = 0; k < poly.length; k++) {
                    yOut[j] += poly[k] * rPow;
                    rPow *= r;
                }
                if (log) yOut[j] = Math.exp(yOut[j]);
            }
            return data;
        }

        @Override
        protected IDataInfo processDataInfo(IDataInfo inputDataInfo) {
            xData = ((DataFunction.DataInfoFunction) (((DataGroup.DataInfoGroup) inputDataInfo).getSubDataInfo(0))).getXDataSource().getIndependentData(0);
            return dataInfo;
        }
    }

    private static class DataProcessorExtract0 extends DataProcessorForked {
        protected String label;
        protected final DataDouble data;

        public DataProcessorExtract0(String label) {
            this.label = label;
            data = new DataDouble();
            dataInfo = new DataDouble.DataInfoDouble(label, Null.DIMENSION);
            dataInfo.addTag(tag);
        }

        @Override
        protected IData processData(IData inputData) {
            data.x = inputData.getValue(0);
            return data;
        }

        @Override
        protected IDataInfo processDataInfo(IDataInfo inputDataInfo) {
            return dataInfo;
        }
    }

}
