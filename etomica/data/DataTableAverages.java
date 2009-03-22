package etomica.data;

import etomica.action.ActionGroupSeries;
import etomica.api.IIntegrator;
import etomica.data.AccumulatorAverage.StatType;

/**
 * Data table that collects the AccumulatorAverage statistics for a collection
 * of DataSource instances. Permits convenient setup of accumulator and piping
 * of data. Data sources may be specified at construction or added afterward.
 * For each source added, this class automatically constructs an
 * AccumulatorAverage, arranges for pumping of data from source to accumulator,
 * and from accumulator to a column in this table.
 * 
 * @author David Kofke
 *  
 */
public class DataTableAverages extends DataSinkTable {

    /**
     * Sets up table with default types that give the current value, the
     * average, and the error bars.
     */
    public DataTableAverages(IIntegrator integrator) {
        this(integrator, 1000);
    }
    
    public DataTableAverages(IIntegrator integrator, int blockSize) {
        this(integrator, new StatType[] { StatType.MOST_RECENT,
                StatType.AVERAGE, StatType.ERROR }, 
                blockSize, null);
    }

    /**
     * Sets up table with no sources.
     */
    public DataTableAverages(IIntegrator integrator, StatType[] types, int blockSize, 
            IEtomicaDataSource[] sources) {
        super();
        this.types = types.clone();
        this.integrator = integrator;
        actionGroup = new ActionGroupSeries();
        integrator.addIntervalAction(actionGroup);
        this.blockSize = blockSize;
        if (sources != null) {
            for (int i = 0; i < sources.length; i++) {
                addDataSource(sources[i]);
            }
        }
    }

    /**
     * Adds the given data source to those feeding the table.
     */
    public void addDataSource(IEtomicaDataSource newSource) {
        AccumulatorAverage accumulator = new AccumulatorAverageFixed(blockSize);
        DataPump dataPump = new DataPump(newSource, accumulator);
        actionGroup.addAction(dataPump);
        accumulator.setPushInterval(tableUpdateInterval);
        accumulator.addDataSink(makeDataSink(),types);
    }

    /**
     * @return Returns the table update interval.
     */
    public int getTableUpdateInterval() {
        return tableUpdateInterval;
    }

    /**
     * Sets the interval for updating the table. The table receives values from
     * the accumulators only after they have received this many data values from
     * their sources. This value does not affect the averages, only the
     * frequency that they are piped to the table.  Default is 100.
     */
    public void setTableUpdateInterval(int tableUpdateInterval) {
        this.tableUpdateInterval = tableUpdateInterval;
        for (int i = 0; i < accumulators.length; i++) {
            accumulators[i].setPushInterval(tableUpdateInterval);
        }
    }

    /**
     * @return Returns the accumulatorUpdateInterval.
     */
    public int getAccumulatorUpdateInterval() {
        return accumulatorUpdateInterval;
    }

    /**
     * Sets the interval for updates to the accumulators feeding this table.
     * Accumulators receive new data only after the integrator fires this many
     * interval events. This value affects the averages. Default is 1.
     */
    public void setAccumulatorUpdateInterval(int accumulatorUpdateInterval) {
        this.accumulatorUpdateInterval = accumulatorUpdateInterval;
        integrator.setActionInterval(actionGroup, accumulatorUpdateInterval);
    }

    /**
     * Returns a clone of the accumulator array. Fields in the accumulators may
     * be modified to affect the behavior of the accumulators. Note that
     * pushInterval of all accumulators can be changed via setTableUpdate.
     */
    public AccumulatorAverage[] getAccumulators() {
        return accumulators.clone();
    }

    private static final long serialVersionUID = 1L;
    private AccumulatorAverage[] accumulators = new AccumulatorAverage[0];
    private final StatType[] types;
    private final ActionGroupSeries actionGroup;
    private int tableUpdateInterval = 100;
    private int accumulatorUpdateInterval = 1;
    private int blockSize;
    private IIntegrator integrator;
}