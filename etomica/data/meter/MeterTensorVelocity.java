package etomica.data.meter;
import etomica.EtomicaInfo;
import etomica.api.IAtom;
import etomica.api.IAtomLeaf;
import etomica.api.IAtomSet;
import etomica.api.IBox;
import etomica.api.IData;
import etomica.api.IDataInfo;
import etomica.api.IVector;
import etomica.atom.IAtomKinetic;
import etomica.data.DataSourceAtomic;
import etomica.data.DataTag;
import etomica.data.IEtomicaDataInfo;
import etomica.data.types.DataTensor;
import etomica.data.types.DataTensor.DataInfoTensor;
import etomica.space.ISpace;
import etomica.units.Energy;

/**
 * A meter to compute the velocity component of the pressure tensor. 
 * Averages a tensor quantity formed from a dyad of the velocity of each atom. 
 * Specifically, the quantity averaged is 1/N * sum(pp/m), where p is the momentum,
 * m is the mass, and the sum is over all N atoms.
 * 
 * @author Rob Riggleman
 */

public class MeterTensorVelocity implements DataSourceAtomic, java.io.Serializable {

    public MeterTensorVelocity(ISpace space) {
        data = new DataTensor(space);
        dataInfo = new DataInfoTensor("pp/m",Energy.DIMENSION, space);
        atomData = new DataTensor(space);
        tag = new DataTag();
        dataInfo.addTag(tag);
    }
    
    public static EtomicaInfo getEtomicaInfo() {
        EtomicaInfo info = new EtomicaInfo("Velocity tensor, formed from averaging dyad of velocity vector for each atom");
        return info;
    }
    
    public IDataInfo getDataInfo() {
        return dataInfo;
    }
    
    public DataTag getTag() {
        return tag;
    }
       
    public IEtomicaDataInfo getAtomDataInfo() {
        return dataInfo;
    }
       
    /**
     * Returns the velocity dyad (mass*vv) summed over all atoms, and divided by N
     */
    public IData getData() {
        if (box == null) throw new IllegalStateException("must call setBox before using meter");
        data.E(0.0);
        int count = 0;
        IAtomSet leafList = box.getLeafList();
        int nLeaf = leafList.getAtomCount();
        for (int iLeaf=0; iLeaf<nLeaf; iLeaf++) {
            getData(leafList.getAtom(iLeaf));
            data.PE(atomData);
            count++;
        }
        data.TE(1.0/nLeaf);
        return data;
    }
    
    /**
     * Returns the velocity dyad (mass*vv) for the given atom.
     */
    public IData getData(IAtom atom) {
        IVector vel = ((IAtomKinetic)atom).getVelocity();
        atomData.x.Ev1v2(vel, vel);
        atomData.TE(((IAtomLeaf)atom).getType().rm());
        return atomData;
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
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    private static final long serialVersionUID = 1L;
    private String name;
    private IBox box;
    private final DataTensor data, atomData;
    private final DataInfoTensor dataInfo;
    protected DataTag tag;
}
