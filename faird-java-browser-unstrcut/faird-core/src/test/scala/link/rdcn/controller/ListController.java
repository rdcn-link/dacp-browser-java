package link.rdcn.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import link.rdcn.struct.Column;
import link.rdcn.struct.DataFrame;
import link.rdcn.struct.Row;

import scala.collection.JavaConverters;
import scala.collection.Seq;

import java.io.IOException;
import java.util.List;

public class ListController {

    @FXML
    private TableView<Row> tableView;

    private DataFrame df;

    private String currentUrl;

    @FXML
    private BorderPane rootPane;  // 对应主界面 FXML 根节点

    private MainController mainController;

    @FXML
    private Pagination pagination;

    public void setCurrentUrl(String currentUrl) {
        this.currentUrl = currentUrl;
    }

    public void setMainController(MainController controller) {
        this.mainController = controller;
    }

    // 供 TestController 调用，传入 DataFrame
    public void setDataFrame(DataFrame df) {
        this.df = df;
        showData();
    }


    private void showData() {
        if (df == null) return;

        // 清空之前的列
        tableView.getColumns().clear();

        // 动态创建表头 (这部分是正确的，无需修改)
        Seq<Column> fields = df.schema().columns().toSeq();
        List<Column> javaFields = JavaConverters.seqAsJavaList(fields);

        for (int colIndex = 0; colIndex < javaFields.size(); colIndex++) {
            final int index = colIndex;
            Column field = javaFields.get(colIndex);

            String colName = field.name();
            String colType = field.colType().toString();

            TableColumn<Row, String> column = new TableColumn<>(colName + " : " + colType);


            column.setCellValueFactory(cellData -> {
                Row row = cellData.getValue();
                Object value = row.get(index);
                return new javafx.beans.property.SimpleStringProperty(value != null ? value.toString() : "");
            });

            tableView.getColumns().add(column);
        }

        // --- 分页逻辑 ---

        // 提前获取所有数据，以供分页使用
        List<Row> allRows = JavaConverters.seqAsJavaList(df.collect().toSeq());

        // 假设每页显示 20 行数据
        final int rowsPerPage = 20;

        // 设置分页控件的总页数
        int pageCount = (int) Math.ceil((double) allRows.size() / rowsPerPage);
        pagination.setPageCount(pageCount);

        // 设置分页工厂方法，每次翻页时都会调用此方法
        pagination.setPageFactory(pageIndex -> {
            // 计算当前页数据的起始和结束索引
            int fromIndex = pageIndex * rowsPerPage;
            int toIndex = Math.min(fromIndex + rowsPerPage, allRows.size());
            // 从总数据列表中截取当前页的数据子集
            List<Row> sublist = allRows.subList(fromIndex, toIndex);
            // 将数据子集包装成 ObservableList 并设置给 TableView
            ObservableList<Row> data = FXCollections.observableArrayList(sublist);
            tableView.setItems(data);
            return new BorderPane();
        });

        // --- 双击行事件处理 (保持不变) ---
        if(currentUrl.contains("listDataSetNames")){
            tableView.setRowFactory(tv -> {
                TableRow<Row> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !row.isEmpty()) {
                        Row rowData = row.getItem();
                        String datasetId = rowData.get(0).toString();
                        String dfUrl = "dacp://0.0.0.0:3101/listDataFrameNames/" + datasetId;
                        mainController.inputField.setText(dfUrl);
                        mainController.skipQueryList(dfUrl);
                    }
                });
                return row;
            });
        }

        if(currentUrl.contains("listDataFrameNames")){
            tableView.setRowFactory(tv -> {
                TableRow<Row> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !row.isEmpty()) {
                        Row rowData = row.getItem();
                        String dataframeId = rowData.get(0).toString();
                        String dfUrl = "dacp://0.0.0.0:3101/get/" + dataframeId;
                        mainController.inputField.setText(dfUrl);
                        mainController.skipQueryList(dfUrl);
                    }
                });
                return row;
            });
        }
    }
}
