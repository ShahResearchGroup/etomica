/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package etomica.modules.glass;

import etomica.atom.AtomType;
import etomica.atom.IAtom;
import etomica.atom.IAtomList;
import etomica.box.Box;
import etomica.data.*;
import etomica.data.types.DataDoubleArray;
import etomica.data.types.DataFunction;
import etomica.space.Space;
import etomica.space.Vector;
import etomica.units.dimensions.Null;
import etomica.units.dimensions.Time;

import java.security.ProtectionDomain;
import java.util.Arrays;

/**
 * Computes the excess kurtosis (alpha2) for the distribution of displacements
 */
public class DataSourceFs implements IDataSource, ConfigurationStorage.ConfigurationStorageListener, DataSourceIndependent {

    protected final ConfigurationStorage configStorage;
    protected DataDoubleArray tData;
    protected DataDoubleArray.DataInfoDoubleArray tDataInfo;
    protected DataFunction data;
    protected DataFunction.DataInfoFunction dataInfo;
    protected double[] fsSum;
    protected final DataTag tTag, tag;
    protected long[] nSamples;
    protected Vector dr, q;
    protected AtomType type;
    protected Space space;

    public DataSourceFs(ConfigurationStorage configStorage) {
        this.configStorage = configStorage;
        space = configStorage.getBox().getSpace();
        fsSum = new double[0];
        nSamples = new long[0];
        tag = new DataTag();
        tTag = new DataTag();
        dr = space.makeVector();
        q = space.makeVector();
        q.setX(0,7.0);
        reset();
    }

    public void setQ(Vector q) {
        this.q = space.makeVector();
        for (int i = 0; i < q.getD(); i++) {
            this.q.setX(i, q.getX(i));
        }
    }


    public Vector getQ(){return  this.q;}

    public void reset() {
        int n = configStorage.getLastConfigIndex();
        if (n  == fsSum.length && data != null) return;
        if (n < 1) n = 0;
        fsSum = Arrays.copyOf(fsSum, n);
        nSamples = Arrays.copyOf(nSamples, n);
        data = new DataFunction(new int[]{n});
        tData = new DataDoubleArray(new int[]{n});
        tDataInfo = new DataDoubleArray.DataInfoDoubleArray("t", Time.DIMENSION, new int[]{n});
        dataInfo = new DataFunction.DataInfoFunction("Fs(t)", Null.DIMENSION, this);
        dataInfo.addTag(tag);

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
    public IData getData() {
        if (configStorage.getLastConfigIndex() < 1) return data;
        double[] y = data.getData();
        int nAtoms = configStorage.getSavedConfig(0).length;
        if(type != null){
            Box box = configStorage.getBox();
            nAtoms = box.getNMolecules(type.getSpecies());
        }

        for (int i = 0; i < fsSum.length; i++) {
            y[i] = fsSum[i] / (nAtoms * nSamples[i]) ; // Why subtract "-1" ?
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

    public void setAtomType(AtomType type) {
        this.type = type;
    }

    @Override
    public void newConfigruation() {
        reset(); // reallocates if needed
        long step = configStorage.getSavedSteps()[0];
        Vector[] positions = configStorage.getSavedConfig(0);
        Box box = configStorage.getBox();
        IAtomList atoms = box.getLeafList();
        for (int i = 1; i < fsSum.length; i++) {
            if (step % (1L << (i - 1)) == 0) {
                Vector[] iPositions = configStorage.getSavedConfig(i);
                for (int j = 0; j < positions.length; j++) {
                    IAtom jAtom = atoms.get(j);
                    if(type == null || jAtom.getType() == type){
                        dr.Ev1Mv2(positions[j], iPositions[j]);
                        fsSum[i-1] += Math.cos(q.dot(dr));
                    }
                }
                nSamples[i - 1]++;
            }
        }
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
}
