package etomica.normalmode;

import etomica.api.IData;
import etomica.data.DataTag;
import etomica.data.IEtomicaDataInfo;
import etomica.data.IEtomicaDataSource;
import etomica.data.meter.MeterPotentialEnergyFromIntegrator;
import etomica.data.types.DataDoubleArray;
import etomica.data.types.DataDoubleArray.DataInfoDoubleArray;
import etomica.integrator.IntegratorBox;
import etomica.units.Null;

/**
 * Meter used for overlap sampling in the target-sampled system.  The meter
 * measures the ratio of the Boltzmann factors for the harmonic and target
 * potentials.
 * 
 * @author Andrew Schultz
 */
public class MeterBoltzmannTarget implements IEtomicaDataSource {
    
    public MeterBoltzmannTarget(IntegratorBox integrator, MeterHarmonicEnergy meterHarmonicEnergy) {
        meterEnergy = new MeterPotentialEnergyFromIntegrator(integrator);
        this.integrator = integrator;
        this.meterHarmonicEnergy = meterHarmonicEnergy;
        data = new DataDoubleArray(2);
        dataInfo = new DataInfoDoubleArray("Scaled Harmonic and hard sphere Energies", Null.DIMENSION, new int[]{2});
        // AccumulatorVirialOverlapSingleAverage expects the first data value to
        // be e_tar / pi_tar, but e_tar = pi_tar
        data.getData()[0] = 1.0;
        tag = new DataTag();
    }

    public IData getData() {
        data.getData()[1] = Math.exp(-(meterHarmonicEnergy.getDataAsScalar() -
                (meterEnergy.getDataAsScalar() - latticeEnergy)) / integrator.getTemperature());
        return data;
    }

    public void setLatticeEnergy(double newLatticeEnergy) {
        latticeEnergy = newLatticeEnergy;
    }
    
    public IEtomicaDataInfo getDataInfo() {
        return dataInfo;
    }

    public DataTag getTag() {
        return tag;
    }

    protected final MeterPotentialEnergyFromIntegrator meterEnergy;
    protected final MeterHarmonicEnergy meterHarmonicEnergy;
    protected final IntegratorBox integrator;
    protected final DataDoubleArray data;
    protected final DataInfoDoubleArray dataInfo;
    protected final DataTag tag;
    protected double latticeEnergy;
}
