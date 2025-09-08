package link.rdcn.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import link.rdcn.struct.Column;
import link.rdcn.struct.DataFrame;
import link.rdcn.struct.Row;
import scala.collection.JavaConverters;
import scala.collection.Seq;

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

        // 动态创建表头
        Seq<Column> fields = df.schema().columns().toSeq();
        List<Column> javaFields = JavaConverters.seqAsJavaList(fields);

        for (int colIndex = 0; colIndex < javaFields.size(); colIndex++) {
            final int index = colIndex;
            Column field = javaFields.get(colIndex);

            String colName = field.name();
            String colType = field.colType().toString();
            TableColumn<Row, String> column = new TableColumn<>(colName);

//            TableColumn<Row, String> column = new TableColumn<>(colName + " : " + colType);

            column.setCellValueFactory(cellData -> {
                Row row = cellData.getValue();
                Object value = row.get(index);
                if (value instanceof link.rdcn.struct.DFRef) {
                    value = ((link.rdcn.struct.DFRef) value).value(); // 获取 URL
                }
                return new javafx.beans.property.SimpleStringProperty(value != null ? value.toString() : "");
            });

            // 👉 自定义单元格渲染：包含 "ref" 时蓝色带下划线
            column.setCellFactory(col -> new TableCell<Row, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);

                        if (isSelected()) {
                            // 如果单元格所在的行被选中，无条件应用蓝底白字样式
                            setStyle("-fx-background-color: #3399FF; -fx-text-fill: white;");
                            setCursor(Cursor.DEFAULT); // 选中时通常使用默认光标
                        } else {
                            // 规则2：如果未被选中，则应用你的其他所有样式规则
                            if (item != null && item.contains("dacp://0.0.0.0:3101")) {
                                // dacp 链接样式
                                setStyle("-fx-text-fill: blue; -fx-underline: true;");
                                setCursor(Cursor.HAND);
                            } else {
                                // 根据数据类型设置背景色
                                switch (colType) { // 确保 colType 在这里是可用的
                                    case "Int":
                                        setStyle("-fx-background-color: #E6FFE6; -fx-text-fill: black;");
                                        break;
                                    case "Double":
                                        setStyle("-fx-background-color: #FFF5E6; -fx-text-fill: black;");
                                        break;
                                    default:
                                        // ！！！非常重要：为不满足任何特殊条件的单元格清除样式
                                        // 这可以防止因单元格复用机制导致的样式错乱问题
                                        setStyle("");
                                        break;
                                }
                                // 非链接单元格使用默认光标
                                setCursor(Cursor.DEFAULT);
                            }
                        }

                    }
                }

            });

            tableView.getColumns().add(column);
        }

        // --- 分页逻辑 ---
//        List<Row> allRows = JavaConverters.seqAsJavaList(df.collect().toSeq());
//        final int rowsPerPage = 20;
//
//        int pageCount = (int) Math.ceil((double) allRows.size() / rowsPerPage);
//        pagination.setPageCount(pageCount);

//        pagination.setPageFactory(pageIndex -> {
//            int fromIndex = pageIndex * rowsPerPage;
//            int toIndex = Math.min(fromIndex + rowsPerPage, allRows.size());
//            List<Row> sublist = allRows.subList(fromIndex, toIndex);
//            ObservableList<Row> data = FXCollections.observableArrayList(sublist);
//            tableView.setItems(data);
//            return new BorderPane();
//        });
        pagination.setPageFactory(pageIndex -> {
            final int rowsPerPage = 20;
            final int from = pageIndex * rowsPerPage;
            final int to = from + rowsPerPage;

            List<Row> pageRows = new java.util.ArrayList<>();

            df.mapIterator(iter -> {
                int i = 0;
                while (iter.hasNext() && i < to) {
                    Row row = iter.next();
                    if (i >= from) pageRows.add(row);
                    i++;
                }
                return null; // mapIterator 需要返回 T，这里直接返回 null
            });

            tableView.setItems(FXCollections.observableArrayList(pageRows));
            return new BorderPane();
        });





        if (currentUrl.contains("listDataSets")) {
            tableView.setRowFactory(tv -> {
                TableRow<Row> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 1 && !row.isEmpty()) {
                        // 获取点击的单元格位置
                        TablePosition<Row, ?> pos = tableView.getSelectionModel().getSelectedCells().get(0);
                        int colIndex = pos.getColumn();

                        // 获取单元格的值
                        Object cellValue = row.getItem().get(colIndex);
                        if (cellValue != null && cellValue.toString().contains("dacp://0.0.0.0:3101")) {
                            // 👉 只有在该列值包含 "ref" 时才跳转
                            Row rowData = row.getItem();
                            String datasetId = rowData.get(0).toString();
                            String dfUrl = "dacp://0.0.0.0:3101/listDataFrames/" + datasetId;
                            mainController.inputField.setText(dfUrl);
                            mainController.skipQueryList(dfUrl);
                        }
                    }
                });
                return row;
            });
        }


        if (currentUrl.contains("listDataFrames")) {
            tableView.setRowFactory(tv -> {
                TableRow<Row> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 1 && !row.isEmpty()) {
                        // 获取点击的单元格位置
                        if (!tableView.getSelectionModel().getSelectedCells().isEmpty()) {
                            TablePosition<Row, ?> pos = tableView.getSelectionModel().getSelectedCells().get(0);
                            int colIndex = pos.getColumn();

                            // 获取单元格值
                            Object cellValue = row.getItem().get(colIndex);

                            if (cellValue != null && cellValue.toString().contains("Ref")) {
                                // 👉 只有包含 "ref" 的单元格才能跳转
                                Row rowData = row.getItem();
                                String dataframeId = rowData.get(0).toString();
                                String dfUrl = "dacp://0.0.0.0:3101/" + dataframeId;
                                mainController.inputField.setText(dfUrl);
                                mainController.skipQueryList(dfUrl);
                            }
                        }
                    }
                });
                return row;
            });
        }

    }
}