package etomica.data;

/**
 * An object that receives Data, processes it, and pushes the result on to a
 * DataSink.
 */

public abstract class DataProcessor implements DataPipe, java.io.Serializable {

    /**
     * Processes the input Data and returns Data for pushing to the next
     * DataSink. Returns null if output Data should not (yet) be pushed
     * downstream.
     * 
     * @param inputData
     *            the Data for processing
     * @return the processed Data, for sending downstream (if not null)
     */
    protected abstract Data processData(Data inputData);

    /**
     * Informs this DataProcessor of the DataInfo for the Data it will be
     * processing. Typically the subclass will use this information to make any
     * objects or otherwise prepare for calls to processData.
     * 
     * @inputDataInfo the DataInfo of the Data that will be input to this
     *                DataProcessor
     * @return the DataInfo of the Data that will be output by this
     *         DataProcessor
     */
    protected abstract DataInfo processDataInfo(DataInfo inputDataInfo);

    /**
     * Processes input Data and pushes it downstream if output Data and DataSink
     * are not null.
     */
    public void putData(Data data) {
        Data outputData = processData(data);
        if (dataSink != null && outputData != null) {
            dataSink.putData(outputData);
        }
    }

    /**
     * Invokes processDataInfo on the given DataInfo, and passes the returned
     * DataInfo to the dataSink (if not null).  Will insert a data caster before
     * the DataSink if appropriate.
     */
    public void putDataInfo(DataInfo inputDataInfo) {
        dataInfo = processDataInfo(inputDataInfo);
        insertTransformerIfNeeded();
        if (dataSink != null) {
            dataSink.putDataInfo(dataInfo);
        }
    }

    /**
     * @return Returns the data sink, which may be null.
     */
    public DataSink getDataSink() {
        return trueDataSink;
    }

    /**
     * Sets the sink receiving the data. Null value is permitted.
     * 
     * @param newDataSink
     *            The data sink to set.
     */
    public void setDataSink(DataSink newDataSink) {
        //trueDataSink is the sink that the caller acutally cares about
        //dataSink is the immeadiate sink for this processor (might be a transformer)
        trueDataSink = newDataSink;
        insertTransformerIfNeeded();
        if (dataSink != null && dataInfo != null) {
            dataSink.putDataInfo(dataInfo);
        }
    }

    private void insertTransformerIfNeeded() {
        if (trueDataSink == null || dataInfo == null)
            return;
        //remove transformer if one was previously inserted
        dataSink = trueDataSink;
        DataProcessor caster = dataSink.getDataCaster(dataInfo);
        if (caster != null) {
            caster.setDataSink(trueDataSink);
            dataSink = caster;
        }
    }

    protected DataSink dataSink;
    protected DataSink trueDataSink;
    protected DataInfo dataInfo;
}
