/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.data.meter;

import etomica.atom.AtomType;
import etomica.atom.IAtom;
import etomica.atom.IAtomList;
import etomica.box.Box;
import etomica.data.*;
import etomica.data.types.DataDoubleArray;
import etomica.data.types.DataDoubleArray.DataInfoDoubleArray;
import etomica.data.types.DataFunction;
import etomica.data.types.DataFunction.DataInfoFunction;
import etomica.lattice.crystal.Primitive;
import etomica.lattice.crystal.PrimitiveGeneral;
import etomica.space.Space;
import etomica.space.Vector;
import etomica.units.dimensions.Null;

import java.util.Arrays;

/**
 * Meter for calculation of structure factor of atoms for all wave vectors less
 * than a cutoff.
 *
 * @author Michael Sellers
 * @author Andrew Schultz
 */
public class MeterStructureFactor implements IDataSource, DataSourceIndependent {

    protected final Space space;
    protected Box box;
    protected double[] struct;
    protected Vector[] waveVec;
    protected IAtomList atomList;
    protected DataFunction data;
    protected DataInfoFunction dataInfo;
    protected final DataTag tag, xTag;
    protected DataDoubleArray xData;
    protected DataInfoDoubleArray xDataInfo;
    protected double[] atomTypeSignal;
    protected double cutoff;

    /**
     * Creates meter with default to compute the structure factor for all atoms
     * in the box.  All wave vectors consistent with the box shape and with
     * magnitude less than cutoff are included.
     */
	public MeterStructureFactor(Space space, Box aBox, double cutoff) {
	    this.space = space;
	    this.box = aBox;
        atomList = box.getLeafList();
        tag = new DataTag();
        xTag = new DataTag();
	    setCutoff(cutoff);
        atomTypeSignal = new double[0];
	}
	
	protected void resetData() {
        xData = new DataDoubleArray(waveVec.length);
        xDataInfo = new DataInfoDoubleArray("q", Null.DIMENSION, new int[]{waveVec.length});
        xDataInfo.addTag(xTag);

	    dataInfo = new DataInfoFunction("Structure Factor", Null.DIMENSION, this);
        dataInfo.addTag(tag);
        data = new DataFunction(new int[]{waveVec.length}, struct);
	}

	protected int makeWaveVector(double cutoff) {
        int nVec = 0;
        double[] x = xData == null ? null : xData.getData();
        Vector[] edges = new Vector[space.D()];
        for (int i = 0; i < space.D(); i++) {
            edges[i] = box.getBoundary().getEdgeVector(i);
        }
        Primitive primitiveBox = new PrimitiveGeneral(space, edges);
        Primitive recip = primitiveBox.makeReciprocal();
        Vector[] basis = recip.vectors();

        double cutoff2 = cutoff*cutoff;

        int[] iMax = new int[space.D()];
        // Be aggressive when look for wave vectors.  If the box is slanty,
        // we will need to go beyond cutoff/basis, but it's hard to know how
        // much.
        for (int i=0; i<space.D(); i++) {
            iMax[i] = 1+2*(int)(cutoff/Math.sqrt(basis[i].squared()));
        }

        int[] idx = new int[space.D()];
        while (true) {
            Vector v = space.makeVector();
            boolean success = false;
            for  (int i=idx.length-1; i>=0; i--) {
                idx[i]++;
                if (idx[i] <= iMax[i]) {
                    success = true;
                    break;
                }
                
                idx[i] = -iMax[i];
            }
            if (!success) break;
            v.E(0);
            for (int i=0; i<idx.length; i++) {
                v.PEa1Tv1(idx[i], basis[i]);
            }
            if (v.squared() > cutoff2) {
                continue;
            }
            if (waveVec != null) {
                waveVec[nVec] = v;
                x[nVec] = Math.sqrt(v.squared());
            }
            nVec++;
        }
        return nVec;
	}

    /**
     * Sets the wave vector cutoff.  All wave vectors consistent with the box
     * shape that have a magnitude less than the cutoff will be computed.
     * @param cutoff the cutoff for the wave vector magnitude
     */
	public void setCutoff(double cutoff) {
	    waveVec = null;
	    int nVec = makeWaveVector(cutoff);
        struct = new double[nVec];
	    waveVec = new Vector[nVec];
        resetData();
        makeWaveVector(cutoff);
        this.cutoff = cutoff;
    }

    public double getCutoff() {
        return cutoff;
    }
	
	/**
	 * @param waveVec Sets a custom wave vector array.
	 */
	public void setWaveVec(Vector[] waveVec){
	    this.waveVec = space.makeVectorArray(waveVec.length);
	    struct = new double[waveVec.length];
		for(int i=0; i<waveVec.length; i++){
			this.waveVec[i].E(waveVec[i]);
		}
		resetData();
	}
	
	/**
	 * @param atomList Sets the list of atoms for factor calculation.
	 */
	public void setAtoms(IAtomList atomList){
		this.atomList = atomList;
	}

    /**
     * Sets the given atom type to have the given form factor
     * https://en.wikipedia.org/wiki/Structure_factor
     */
    public void setAtomTypeFactor(AtomType atomType, double factor) {
        int idx = atomType.getIndex();
        if (idx >= atomTypeSignal.length) {
            int oldLength = atomTypeSignal.length;
            atomTypeSignal = Arrays.copyOf(atomTypeSignal, atomType.getIndex() + 1);
            for (int i = oldLength; i < idx; i++) atomTypeSignal[i] = 1;
        }
        atomTypeSignal[idx] = factor;
    }

    public IData getData() {
        long numAtoms = atomList.size();
        long n2 = numAtoms*numAtoms;
        for (int i = 0; i < struct.length; i++) struct[i] = 0;
        for(int k = 0; k<waveVec.length; k++){
            double term1 = 0;
            double term2 = 0;
            for(int i = 0; i<numAtoms; i++){
                IAtom atom = atomList.get(i);
                int typeIdx = atom.getType().getIndex();
                double signal = atomTypeSignal.length > typeIdx ? atomTypeSignal[typeIdx] : 1.0;
                double dotprod = waveVec[k].dot(atom.getPosition());
                term1 += signal * Math.cos(dotprod);
                term2 += signal * Math.sin(dotprod);
            }
            struct[k] = ((term1*term1) + (term2*term2))/n2;
        }
        return data;
    }

    public DataTag getTag() {
        return tag;
    }

    public IDataInfo getDataInfo() {
        return dataInfo;
    }
    
    public DataDoubleArray getIndependentData(int i) {
        return xData;
    }

    public DataInfoDoubleArray getIndependentDataInfo(int i) {
        return xDataInfo;
    }

    public int getIndependentArrayDimension() {
        return 1;
    }

    public DataTag getIndependentTag() {
        return xTag;
    }

}
