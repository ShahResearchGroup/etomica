package etomica.modules.dcvgcmd;

import etomica.api.IBox;
import etomica.data.IEtomicaDataSource;
import etomica.data.DataSourceScalar;
import etomica.units.Area;
import etomica.units.CompoundDimension;
import etomica.units.Dimension;
import etomica.units.DimensionRatio;
import etomica.units.Quantity;
import etomica.units.Time;

public class MeterFlux extends DataSourceScalar implements IEtomicaDataSource {
    
	public MeterFlux(MyMCMove move, IntegratorDCVGCMD integrator) {
		super("Flux",new DimensionRatio(Quantity.DIMENSION,new CompoundDimension(new Dimension[] {Area.DIMENSION, Time.DIMENSION}, new double[] {1,1})));
		mcMove = move;
		integratorMD = integrator;
	}
 
	public double getDataAsScalar() {
		double t1 = integratorMD.getCurrentTime();
        if(t1 == t0) return Double.NaN;
		int n1 = mcMove.getDeltaN();
		double rate = (n1 - n0)/(t1 - t0)/(box.getBoundary().getDimensions().getX(0)*box.getBoundary().getDimensions().getX(1));
		n0 = n1;
		t0 = t1;
		return rate;
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

    private IBox box;
    private double t0 = 0;
    private int n0 = 0;
    private MyMCMove mcMove;
    private IntegratorDCVGCMD integratorMD;
}