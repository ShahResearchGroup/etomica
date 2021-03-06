/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package etomica.modules.glass;

import etomica.data.*;
import etomica.data.types.DataDoubleArray;
import etomica.data.types.DataFunction;
import etomica.space.Space;
import etomica.space.Vector;
import etomica.units.dimensions.*;

import java.util.Arrays;

public class DataSourcePercolation implements IDataSource, ConfigurationStorage.ConfigurationStorageListener, DataSourceIndependent {

    protected final ConfigurationStorage configStorage;
    protected DataDoubleArray tData;
    protected DataDoubleArray.DataInfoDoubleArray tDataInfo;
    protected DataFunction data, immFracData;
    protected DataFunction.DataInfoFunction dataInfo, immFracDataInfo;
    protected double[] percP;
    protected long[] immFraction;
    protected final DataTag tTag, tag, immFracTag;

    protected long[] nSamples;
    protected final AtomNbrClusterer clusterer;
    protected AtomTestDeviation atomTest;
    protected final int[][] clusterSize;
    protected final int numAtoms;
    protected final int[] clusterStack;
    protected final boolean[] isVisited;
    protected final Space space;
    protected final Vector[] r;
    protected int log2StepMin, log2StepMax;


    public DataSourcePercolation(ConfigurationStorage configStorage, AtomTestDeviation atomTest, int log2StepMin, int log2StepMax) {
        this.configStorage = configStorage;

        clusterer = new AtomNbrClusterer(configStorage.getBox(), atomTest, true);
        this.atomTest = atomTest;
        this.log2StepMin = log2StepMin;
        this.log2StepMax = log2StepMax;
        numAtoms = configStorage.getBox().getLeafList().size();
        clusterSize = new int[numAtoms][2];
        clusterStack = new int[numAtoms];
        isVisited= new boolean[numAtoms];
        space = configStorage.box.getSpace();
        percP = new double[0];
        immFraction = new long[0];
        nSamples = new long[0];
        tag = new DataTag();
        immFracTag = new DataTag();
        tTag = new DataTag();
        r = space.makeVectorArray(numAtoms);
        reset();
    }

    public void setLog2StepStart(int newStart) {
        log2StepMin = newStart;
    }

    public int getLog2StepStart() {
        return log2StepMin;
    }

    public void setLog2StepEnd(int newEnd) {
        log2StepMax = newEnd;
    }

    public int getLog2StepEnd() {
        return log2StepMax;
    }

    public void setNbrMax(double nbrMax) { clusterer.setNbrMax(nbrMax);}

    public double getNbrMax() {
        return clusterer.getNbrMax();
    }


    public void reset() {
        int n = configStorage.getLastConfigIndex();
        if (n == percP.length && data != null) return;
        if (n < 1) n = 0;
        percP = Arrays.copyOf(percP, n);
        immFraction = Arrays.copyOf(immFraction, n);
        nSamples = Arrays.copyOf(nSamples, n);
        data = new DataFunction(new int[]{n});
        immFracData = new DataFunction(new int[]{n});
        tData = new DataDoubleArray(new int[]{n});
        tDataInfo = new DataDoubleArray.DataInfoDoubleArray("t", Time.DIMENSION, new int[]{n});
        tDataInfo.addTag(tTag);
        dataInfo = new DataFunction.DataInfoFunction("percProb", new CompoundDimension(new Dimension[]{Length.DIMENSION}, new double[]{2}), this);
        dataInfo.addTag(tag);
        immFracDataInfo = new DataFunction.DataInfoFunction("immFraction", Null.DIMENSION, this);
        immFracDataInfo.addTag(immFracTag);


        double[] t = tData.getData();
        if (t.length > 0) {
            double[] savedTimes = configStorage.getSavedTimes();
            double dt = savedTimes[0] - savedTimes[1];
            for (int i = 0; i < t.length; i++) {
                t[i] = dt * (1L << i);
            }
        }
    }

    @Override
    public void newConfigruation() {
        reset(); // reallocates if needed
        long step = configStorage.getSavedSteps()[0];
        Vector[] positions = configStorage.getSavedConfig(0);
        for (int i = log2StepMin; i < percP.length && i <= log2StepMax; i++) {
            if (step % (1L << i) == 0) {
                for(int j=0; j<numAtoms; j++){
                    isVisited[j] = false;
                }

                atomTest.setConfigIndex(i);
                clusterer.findClusters();
                int[] firstAtom = clusterer.getFirstAtom();
                int[] nextAtom = clusterer.getNextAtom();
                int nClusters = 0;
                for (int j = 0; j < firstAtom.length; j++) {
                    if (firstAtom[j] == -1) break;
                    nClusters++;
                    clusterSize[j][0] = j;
                    clusterSize[j][1] = 0;
                    for (int ii = firstAtom[j]; ii != -1; ii = nextAtom[ii]) {
                        clusterSize[j][1]++;
                        immFraction[i]++;

                    }
                }
                java.util.Arrays.sort(clusterSize, 0, nClusters, new java.util.Comparator<int[]>() {
                    public int compare(int[] a, int[] b) {
                        return Integer.compare(b[1], a[1]);
                    }
                });



                outer:
                for (int j = 0; j < nClusters; j++) {
                    if(clusterSize[j][1] > 2*numAtoms/3) {
                        percP[i]++;
                        break outer; //do not look for further clusters; go to next i.
                    }
                    int c = clusterSize[j][0]; //cluster No.
                    if(clusterSize[j][1] == 1) break;
                    int a = firstAtom[c]; // a is 1st atom in cluster
                    clusterStack[0] = a; //push a
                    isVisited[a] = true; // a is visited
                    r[a].E(positions[a]);
                    Vector tmp = space.makeVector();
                    int k_top = 0;
                    while (k_top > -1) {//BFS
                        a = clusterStack[k_top];
                        k_top--;
                        int[] nbrs = clusterer.nbrList[a];
                        for (int m = 0; m < nbrs.length && nbrs[m] != -1; m++) {
                            int b = nbrs[m];
                            tmp.Ev1Mv2(positions[b], positions[a]);
                            configStorage.getBox().getBoundary().nearestImage(tmp);
                            tmp.PE(r[a]);
                            if(isVisited[b] && r[b].Mv1Squared(tmp) > 1e-8){//percolation
                                percP[i]++;
                                break outer;//
                            }
                            if(!isVisited[b]){//New nbrs
                                k_top++;
                                clusterStack[k_top] = b; //push b
                                isVisited[b] = true; // b is visited
                            }
                            r[b].E(tmp);
                        }
                    }
                }//loop over clusters
                nSamples[i]++;
            }//2^i check
        }// loop over i
    }


    @Override
    public IData getData() {
        if (configStorage.getLastConfigIndex() < 1) return data;
        double[] y = data.getData();
        for (int i = 0; i < percP.length; i++) {
            y[i] = percP[i]/nSamples[i];
        }
        return data;
    }



    @Override
    public DataTag getTag() {
        return tag;
    }

    @Override
    public IDataInfo getDataInfo() {
        return dataInfo;
    }

    @Override
    public DataDoubleArray getIndependentData(int i) {
        return tData;
    }

    @Override
    public DataDoubleArray.DataInfoDoubleArray getIndependentDataInfo(int i) {
        return tDataInfo;
    }

    @Override
    public int getIndependentArrayDimension() {
        return 1;
    }

    @Override
    public DataTag getIndependentTag() {
        return tTag;
    }





    public ImmFractionSource makeImmFractionSource () {return new ImmFractionSource();}

    public class ImmFractionSource implements IDataSource {
        @Override
        public IData getData() {
            if (configStorage.getLastConfigIndex() < 1) return immFracData;
            double[] yImmFrac = immFracData.getData();
            for (int i = 0; i < immFraction.length; i++) {
                yImmFrac[i] = (double)immFraction[i]/nSamples[i]/numAtoms;

            }
            return immFracData;
        }

        @Override
        public DataTag getTag() {
            return immFracTag;
        }

        @Override
        public IDataInfo getDataInfo() {
            return immFracDataInfo;
        }
    }

}







