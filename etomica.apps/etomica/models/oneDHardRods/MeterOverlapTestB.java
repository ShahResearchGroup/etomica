package etomica.models.oneDHardRods;

import etomica.data.DataSourceScalar;
import etomica.units.Null;

public class MeterOverlapTestB extends DataSourceScalar {

    DataSourceScalar dataSourceA, dataSourceB;
        double temperature, alpha;
        
        public MeterOverlapTestB(DataSourceScalar dataSourceA, DataSourceScalar dataSourceB, double temperature){
            super("MeterOverlap", Null.DIMENSION);
            this.dataSourceA = dataSourceA;
            this.dataSourceB = dataSourceB;
            this.temperature = temperature;
            
            setAlpha(1.0);
        }
        
        public double getDataAsScalar() {
            double eB = Math.exp(-dataSourceB.getDataAsScalar()/temperature);
            double eA = Math.exp(-dataSourceA.getDataAsScalar()/temperature);
            
            return eA/(eA+alpha*eB);
            
        }

        public void setAlpha(double a){
            alpha = a;
        }
    }
