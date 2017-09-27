package etomica.modules.statistics;

import etomica.data.DataPipe;
import etomica.data.IData;
import etomica.data.IDataSink;
import etomica.data.IEtomicaDataInfo;

public class DataAccPusher implements IDataSink {

    protected final int idx;
    protected final DataCollector collector;

    public DataAccPusher(int idx, DataCollector collector) {
        this.idx = idx;
        this.collector = collector;
    }

    @Override
    public void putData(IData data) {
        collector.setData(idx, data.getValue(0));
    }

    @Override
    public void putDataInfo(IEtomicaDataInfo dataInfo) {
        // don't care!
    }

    @Override
    public DataPipe getDataCaster(IEtomicaDataInfo inputDataInfo) {
        return null;
    }
}