package etomica.data;

import etomica.api.IData;
import etomica.api.IDataInfo;
import etomica.api.IFunction;
import etomica.data.types.DataGroup;
import etomica.data.types.DataGroup.DataInfoGroupFactory;
import etomica.util.Arrays;
import etomica.util.Function;

/**
 * Accumulator for calculating ratio between two sums
 */
public class AccumulatorRatioAverage extends AccumulatorAverageFixed {
    
    private static final long serialVersionUID = 1L;
    public AccumulatorRatioAverage() {
        this(1000);
    }
    
    public AccumulatorRatioAverage(int blockSize) {
        super(blockSize);
        ratioTag = new DataTag();
        ratioStandardDeviationTag = new DataTag();
        ratioErrorTag = new DataTag();
    }
    
    public Object getTag(StatType statType) {
        if (statType == StatType.RATIO) {
            return ratioTag;
        }
        if (statType == StatType.RATIO_STANDARD_DEVIATION) {
            return ratioStandardDeviationTag;
        }
        if (statType == StatType.RATIO_ERROR) {
            return ratioError;
        }
        return super.getTag(statType);
    }
    
    public IData getData() {
        if (sum == null) return null;
        if (count > 0) {
            super.getData();

            double average0 = average.getValue(0);
            if (average0 == 0) {
                ratio.E(Double.NaN);
                ratioError.E(Double.NaN);
                ratioStandardDeviation.E(Double.NaN);
                return dataGroup;
            }

            ratio.E(sum);
            ratio.TE(1/sum.getValue(0));

            double errorRatio0 = error.getValue(0)/average0;
            errorRatio0 *= errorRatio0;
            ratioError.E(error);
            ratioError.DE(average);
            ratioError.TE(ratioError);
            ratioError.PE(errorRatio0);
            ratioError.TE(ratio);
            ratioError.TE(ratio);
            ratioError.map(Function.Sqrt.INSTANCE);

            double stdevRatio0 = standardDeviation.getValue(0)/average0;
            ratioStandardDeviation.E(standardDeviation);
            ratioStandardDeviation.DE(average);
            ratioStandardDeviation.TE(ratioStandardDeviation);
            ratioStandardDeviation.PE(stdevRatio0);
            ratioStandardDeviation.TE(ratio);
            ratioStandardDeviation.TE(ratio);
            ratioStandardDeviation.map(Function.Sqrt.INSTANCE);
        }
        return dataGroup;
    }

    public void reset() {
        if (sum == null || ratio == null) {
            //no data has been added yet, so nothing to reset
            return;
        }
        super.reset();
        ratio.E(Double.NaN);
        ratioError.E(Double.NaN);
        ratioStandardDeviation.E(Double.NaN);
    }
    
    public IEtomicaDataInfo processDataInfo(IEtomicaDataInfo incomingDataInfo) {
        super.processDataInfo(incomingDataInfo);

        ratio = incomingDataInfo.makeData();
        ratioError = incomingDataInfo.makeData();
        ratioStandardDeviation = incomingDataInfo.makeData();

        IData[] dataGroups = new IData[dataGroup.getNData()+3];
        int i;
        for (i=0; i<dataGroup.getNData(); i++) {
            dataGroups[i] = dataGroup.getData(i);
        }
        dataGroups[i++] = ratio;
        dataGroups[i++] = ratioError;
        dataGroups[i++] = ratioStandardDeviation;
        dataGroup = new DataGroup(dataGroups);
        reset();

        DataInfoGroupFactory groupFactory = (DataInfoGroupFactory)dataInfo.getFactory();
        IDataInfo[] subDataInfo = groupFactory.getSubDataInfo();

        IEtomicaDataInfoFactory factory = incomingDataInfo.getFactory();
        String incomingLabel = incomingDataInfo.getLabel();
        factory.setLabel(incomingLabel+" ratio");
        IEtomicaDataInfo ratioInfo = factory.makeDataInfo();
        ratioInfo.addTag(ratioTag);
        factory.setLabel(incomingLabel+" ratio error");
        IEtomicaDataInfo ratioErrorInfo = factory.makeDataInfo();
        ratioErrorInfo.addTag(ratioErrorTag);
        factory.setLabel(incomingLabel+" ratio");
        IEtomicaDataInfo ratioStandardDeviationInfo = factory.makeDataInfo();
        ratioStandardDeviationInfo.addTag(ratioStandardDeviationTag);
        
        subDataInfo = (IDataInfo[])Arrays.addObject(subDataInfo, ratioInfo);
        subDataInfo = (IDataInfo[])Arrays.addObject(subDataInfo, ratioErrorInfo);
        subDataInfo = (IDataInfo[])Arrays.addObject(subDataInfo, ratioStandardDeviationInfo);
        groupFactory.setSubDataInfo(subDataInfo);

        dataInfo = groupFactory.makeDataInfo();
        return dataInfo;
    }
    
    public static class StatType extends AccumulatorAverage.StatType {
        private static final long serialVersionUID = 1L;
        protected StatType(String label, int index) {super(label,index);}       
        public static final StatType RATIO = new StatType("Ratio",5);
        public static final StatType RATIO_ERROR = new StatType("Ratio error",6);
        public static final StatType RATIO_STANDARD_DEVIATION = new StatType("Ratio standard deviation",7);
        public static AccumulatorAverage.StatType[] choices() {
            AccumulatorAverage.StatType[] choices = AccumulatorAverage.StatType.choices();
            return new AccumulatorAverage.StatType[] {
                choices[0], choices[1], choices[2], choices[3], choices[4],
                RATIO, RATIO_ERROR, RATIO_STANDARD_DEVIATION};
        }
    }

    //need separate fields because ratio values are calculated from the non-ratio values.
    protected IData ratio, ratioStandardDeviation, ratioError;
    private final DataTag ratioTag, ratioStandardDeviationTag, ratioErrorTag;
}
