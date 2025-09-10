package link.rdcn.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
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
    private boolean isLoading = false;

    private long elapsedMs = 0;
    private double bytes = 0;

    @FXML
    private TableView<Row> tableView;

    private DataFrame df;

    private String currentUrl;

    @FXML
    private BorderPane rootPane;  // 对应主界面 FXML 根节点

    private MainController mainController;

    // === 分页控件去掉 ===
    // @FXML
    // private Pagination pagination;

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
        tableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        // 动态创建表头
        Seq<Column> fields = df.schema().columns().toSeq();
        List<Column> javaFields = JavaConverters.seqAsJavaList(fields);
        for (int colIndex = 0; colIndex < javaFields.size(); colIndex++) {
            final int index = colIndex;
            Column field = javaFields.get(colIndex);

            String colName = field.name();
            String colType = field.colType().toString();
            TableColumn<Row, String> column = new TableColumn<>(colName);

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
                private static final String INT_CELL_STYLE = "int-cell";
                private static final String DOUBLE_CELL_STYLE = "double-cell";
                private static final String LINK_CELL_STYLE = "link-cell";

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    setText(null);
                    setGraphic(null);
                    setCursor(Cursor.DEFAULT);
                    getStyleClass().removeAll(INT_CELL_STYLE, DOUBLE_CELL_STYLE, LINK_CELL_STYLE);

                    if (empty || item == null) {
//                        setText(null);
//                        setStyle("");
//                        setCursor(Cursor.DEFAULT);
                    } else {
                        setText(item);

                        boolean selected = getTableRow() != null && getTableRow().isSelected();

//                        if (selected) {
////                            getStyleClass().add("link-selected");
//                            setStyle("-fx-background-color: #3399FF; -fx-text-fill: white;");
//                            setCursor(Cursor.DEFAULT);
//                        } else if (item.contains("dacp://0.0.0.0:3101")) {
//                            setStyle("-fx-text-fill: blue; -fx-underline: true;");
//                            setCursor(Cursor.HAND);
//                        } else {
//                            switch (colType) { // Assumes colType is accessible here
//                                case "Int":
//                                    getStyleClass().add(INT_CELL_STYLE);
//                                    break;
//                                case "Double":
//                                    getStyleClass().add(DOUBLE_CELL_STYLE);
//                                    break;
//                                default:
//                                    // No special style class for default cells
//                                    break;
//                            }
//                            setCursor(Cursor.DEFAULT);
//                        }
                        if (item.contains("dacp://0.0.0.0:3101")) {
                            getStyleClass().add(LINK_CELL_STYLE);
                            setCursor(Cursor.HAND);
                        } else {
                            switch (colType) {
                                case "Int":
                                    getStyleClass().add(INT_CELL_STYLE);
                                    break;
                                case "Double":
                                    getStyleClass().add(DOUBLE_CELL_STYLE);
                                    break;
                                default:
                                    // 默认单元格无需额外样式
                                    break;
                            }
                        }
                    }
                }
            });

            tableView.getColumns().add(column);
        }

        // --- 滚动流式加载 ---
        final int rowsPerPage = 50;
        int[] offset = {0};  // 记录加载到哪一行

        ObservableList<Row> loadedRows = FXCollections.observableArrayList();
        tableView.setItems(loadedRows);

        // 第一次加载
        loadMoreRows(offset, rowsPerPage, loadedRows);

        // 监听滚动条
//        tableView.skinProperty().addListener((obs, oldSkin, newSkin) -> {
//            if (newSkin != null) {
//                ScrollBar vBar = (ScrollBar) tableView.lookup(".scroll-bar:vertical");
//                if (vBar != null) {
//                    vBar.valueProperty().addListener((o, oldVal, newVal) -> {
//                        if (newVal.doubleValue() == vBar.getMax()) {
//                            // 滚动到底部，加载更多
//                            loadMoreRows(offset, rowsPerPage, loadedRows);
//                        }
//                    });
//                }
//            }
//        });
        // 控制是否正在加载的标志

// 初始化时
        tableView.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null) {
                ScrollBar vBar = (ScrollBar) tableView.lookup(".scroll-bar:vertical");
                if (vBar != null) {
                    vBar.valueProperty().addListener((o, oldVal, newVal) -> {
                        // 滚动超过 80% 且未在加载状态时触发
                        if (!isLoading && newVal.doubleValue() >= vBar.getMax() * 0.8) {
                            isLoading = true;

                            Platform.runLater(() -> {
                                loadMoreRows(offset, rowsPerPage, loadedRows);
                                isLoading = false;
                            });
                        }
                    });
                }
            }
        });


        // === 点击跳转逻辑 ===
        if (currentUrl.contains("listDataSets")) {
            tableView.setRowFactory(tv -> {
                TableRow<Row> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 1 && !row.isEmpty()) {
                        TablePosition<Row, ?> pos = tableView.getSelectionModel().getSelectedCells().get(0);
                        int colIndex = pos.getColumn();

                        Object cellValue = row.getItem().get(colIndex);
                        if (cellValue != null && cellValue.toString().contains("dacp://0.0.0.0:3101")) {
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
                        if (!tableView.getSelectionModel().getSelectedCells().isEmpty()) {
                            TablePosition<Row, ?> pos = tableView.getSelectionModel().getSelectedCells().get(0);
                            int colIndex = pos.getColumn();

                            Object cellValue = row.getItem().get(colIndex);

                            if (cellValue != null && cellValue.toString().contains("Ref")) {
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




    private void loadMoreRows(int[] offset, int rowsPerPage, ObservableList<Row> loadedRows) {
        int from = offset[0];
        int to = from + rowsPerPage;

        List<Row> pageRows = new java.util.ArrayList<>();

        long startTime = System.nanoTime();
        final long[] bytesFetched = {0};

        df.mapIterator(iter -> {
            int i = 0;
            while (iter.hasNext() && i < to) {
                Row row = iter.next();
                if (i >= from) {
                    pageRows.add(row);
                    // 简单计算字节数（UTF-8）
                    bytesFetched[0] += row.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
                }
                i++;
            }
            return null;
        });
        long endTime = System.nanoTime();
        elapsedMs = (endTime - startTime) / 1_000_000;
        // ✅ 加载到表格
        loadedRows.addAll(pageRows);
        offset[0] += rowsPerPage;
        bytes = bytesFetched[0] / 1024.0;
        mainController.setTimeAndByteLabel("Status:   Run time: "+ elapsedMs + "ms" + "  Load Bytes: " + bytes);
    }
}
