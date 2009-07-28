package etomica.data.meter;
import etomica.action.IAction;
import etomica.api.IAtomList;
import etomica.api.IAtomPositioned;
import etomica.api.IBoundary;
import etomica.api.IBox;
import etomica.api.IVectorMutable;
import etomica.atom.iterator.ApiLeafAtoms;
import etomica.atom.iterator.AtomsetIteratorBoxDependent;
import etomica.data.DataSourceIndependent;
import etomica.data.DataSourceUniform;
import etomica.data.DataTag;
import etomica.data.IData;
import etomica.data.IEtomicaDataInfo;
import etomica.data.IEtomicaDataSource;
import etomica.data.DataSourceUniform.LimitType;
import etomica.data.types.DataDoubleArray;
import etomica.data.types.DataFunction;
import etomica.data.types.DataDoubleArray.DataInfoDoubleArray;
import etomica.data.types.DataFunction.DataInfoFunction;
import etomica.space.ISpace;
import etomica.units.Length;
import etomica.units.Null;

/**
 * Meter for tabulation of the atomic radial distribution function (RDF).  The
 * meter takes data via actionPerformed and returns the average RDF via
 * getData.
 *
 * @author David Kofke
 */
public class MeterRDF implements IAction, IEtomicaDataSource, DataSourceIndependent, java.io.Serializable {
	
	/**
	 * Creates meter with default to compute pair correlation for all
	 * leaf atoms in a box.
	 * @param parent
	 */
    public MeterRDF(ISpace space) {
	    this.space = space;

        xDataSource = new DataSourceUniform("r", Length.DIMENSION);
        xDataSource.setTypeMax(LimitType.HALF_STEP);
        xDataSource.setTypeMin(LimitType.HALF_STEP);
        
        rData = (DataDoubleArray)xDataSource.getData();
        data = new DataFunction(new int[] {rData.getLength()});
        gSum = new long[rData.getLength()];
        dataInfo = new DataInfoFunction("g(r)", Null.DIMENSION, this);

	    iterator = new ApiLeafAtoms();
        dr = space.makeVector();
        tag = new DataTag();
        dataInfo.addTag(tag);
    }
    
    public IEtomicaDataInfo getDataInfo() {
        return dataInfo;
    }
    
    public DataTag getTag() {
        return tag;
    }
    
    /**
     * Zero's out the RDF sum tracked by this meter.
     */
    public void reset() {
        rData = (DataDoubleArray)xDataSource.getData();
        xMax = xDataSource.getXMax();
        data = new DataFunction(new int[] {rData.getLength()});
        gSum = new long[rData.getLength()];
        dataInfo = new DataInfoFunction("g(r)", Null.DIMENSION, this);
        dataInfo.addTag(tag);
        callCount = 0;
    }
    
    /**
     * Takes the RDF for the current configuration of the given box.
     */
    public void actionPerformed() {
        if (rData != xDataSource.getData() ||
            data.getLength() != rData.getLength() ||
            xDataSource.getXMax() != xMax) {
            reset();
        }
        
        double xMaxSquared = xMax*xMax;
        iterator.setBox(box);
        iterator.reset();
        // iterate over all pairs
        for (IAtomList pair = iterator.next(); pair != null;
             pair = iterator.next()) {
            dr.Ev1Mv2(((IAtomPositioned)pair.getAtom(1)).getPosition(),((IAtomPositioned)pair.getAtom(0)).getPosition());
            boundary.nearestImage(dr);
            double r2 = dr.squared();       //compute pair separation
            if(r2 < xMaxSquared) {
                int index = xDataSource.getIndex(Math.sqrt(r2));  //determine histogram index
                gSum[index]++;                        //add once for each atom
            }
        }
        callCount++;
    }
    
	/**
	 * Returns the RDF, averaged over the calls to actionPerformed since the
     * meter was reset or had some parameter changed (xMax or # of bins).
	 */
	public IData getData() {
        if (rData != xDataSource.getData() ||
            data.getLength() != rData.getLength() ||
            xDataSource.getXMax() != xMax) {
            reset();
            //that zeroed everything.  just return the zeros.
            return data;
        }
        
        final double[] y = data.getData();
	    double norm = 0.5 * callCount * (box.getMoleculeList().getMoleculeCount() / box.getBoundary().volume())*(box.getLeafList().getAtomCount()-1);
	    double[] r = rData.getData();
	    double dx2 = 0.5*(xMax - xDataSource.getXMin())/r.length;
	    for(int i=0;i<r.length; i++) {
	        double vShell = space.sphereVolume(r[i]+dx2)-space.sphereVolume(r[i]-dx2);
	        y[i] = gSum[i] / (norm*vShell);
	    }
	    return data;
	}
    
    public DataSourceUniform getXDataSource() {
        return xDataSource;
    }
	
    public DataDoubleArray getIndependentData(int i) {
        return (DataDoubleArray)xDataSource.getData();
    }
    
    public DataInfoDoubleArray getIndependentDataInfo(int i) {
        return (DataInfoDoubleArray)xDataSource.getDataInfo();
    }
    
    public int getIndependentArrayDimension() {
        return 1;
    }
    
    /**
     * @return Returns the box.
     */
    public IBox getBox() {
        return box;
    }
    /**
     * @param box The box to set.
     */
    public void setBox(IBox box) {
        this.box = box;
        boundary = box.getBoundary();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    private static final long serialVersionUID = 1L;
    protected IBox box;
    protected final ISpace space;
    protected long[] gSum;
    protected DataFunction data;
    private IEtomicaDataInfo dataInfo;
    protected DataDoubleArray rData;
    protected AtomsetIteratorBoxDependent iterator;
    private final IVectorMutable dr;
    private IBoundary boundary;
    protected final DataSourceUniform xDataSource;
    protected double xMax;
    private String name;
    protected final DataTag tag;
    protected int callCount;
}
