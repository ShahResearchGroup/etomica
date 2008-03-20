package etomica.data;

import etomica.api.IAction;

/**
 * A DataProcessor whose action is to actively take Data from a DataSource and send it to
 * DataSinks.  
 */
public class DataPump extends DataProcessor implements IAction {

    /**
	 * Constructs DataPump with the given DataSource and
	 * DataSink.  Data source cannot be null.  Data sink can 
     * be null and must be identified via setDataSink if DataPump
     * is to have any effect.
	 */
    public DataPump(DataSource dataSource, DataSink dataSink) {
        if(dataSource == null) throw new NullPointerException("Error: cannot construct data pump without a data source");
        this.dataSource = dataSource;
        dataSourceInfo = dataSource.getDataInfo();
        setDataSink(dataSink);
        putDataInfo(dataSource.getDataInfo());
	}
    
	/**
     * Transmits the data from the source to the sink. Before transmitting
     * the Data, this method will first check that the DataInfo from the source
     * is the same as it was last time this method was invoked.  If it has changed,
     * a call to putDataInfo in the sink will be invoked before passing along the Data.
	 */
	public void actionPerformed() {
        Data data = dataSource.getData();
        if (dataSourceInfo != dataSource.getDataInfo()) {
            dataSourceInfo = dataSource.getDataInfo();
            if (dataSink != null) {
                dataSink.putDataInfo(dataSourceInfo);
            }
        }
        putData(data);
    }
    
    /**
     * Returns the given Data.
     */
    public Data processData(Data inputData) {
        return inputData;
    }
    
    /**
     * Returns the given DataInfo.
     */
    public IDataInfo processDataInfo(IDataInfo inputDataInfo) {
        dataInfo = inputDataInfo.getFactory().makeDataInfo();
        dataInfo.addTag(getTag());
        return dataInfo;
    }
    
    /**
     * Returns null, indicating that this DataSink can handle any type of Data without casting.
     */
    public DataPipe getDataCaster(IDataInfo incomingDataInfo) {
        return null;
    }
    
    /**
     * @return Returns the dataSource.
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    private static final long serialVersionUID = 1L;
    private IDataInfo dataSourceInfo;
    private final DataSource dataSource;
}
