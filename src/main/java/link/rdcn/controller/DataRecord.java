package link.rdcn.controller;

import javafx.beans.property.SimpleStringProperty;

public class DataRecord {
    private final SimpleStringProperty dataSet;

    public DataRecord(String dataSet) {
        this.dataSet = new SimpleStringProperty(dataSet);
    }

    public String getDataSet() {
        return dataSet.get();
    }

    public void setDataSet(String value) {
        dataSet.set(value);
    }
}
