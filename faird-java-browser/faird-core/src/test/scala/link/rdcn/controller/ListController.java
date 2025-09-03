package link.rdcn.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
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

    @FXML
    private Label subtitleLabel;

    @FXML
    private Button favoriteButton;

    private DataFrame df;

    private String currentUrl;

    @FXML
    private BorderPane rootPane;  // 对应主界面 FXML 根节点

    private MainController mainController;

    public void setCurrentUrl(String currentUrl) {
        this.currentUrl = currentUrl;
    }

    public void setMainController(MainController controller) {
        this.mainController = controller;
    }

    // 供 TestController 调用，传入 DataFrame
    public void setDataFrame(DataFrame df) {
        this.df = df;
        rootPane.setCenter(tableView);
        showData();
    }

    public void setUrl(String url) {
        subtitleLabel.setText(url);
    }


    private void showData() {


        if (df == null) return;

        favoriteButton.setOnAction(e -> {
            FavoriteManager.addFavorite(currentUrl);
            favoriteButton.setText("✔ 已收藏");
            favoriteButton.setDisable(true);
        });

        // 清空之前的列
        tableView.getColumns().clear();

        // 动态创建表头
        Seq<Column> fields = df.schema().columns().toSeq();
        List<Column> javaFields = JavaConverters.seqAsJavaList(fields);

        for (int colIndex = 0; colIndex < javaFields.size(); colIndex++) {
            final int index = colIndex;
            Column field = javaFields.get(colIndex);

            TableColumn<Row, String> column = new TableColumn<>(field.name());

            column.setCellValueFactory(cellData -> {
                Row row = cellData.getValue();
                Object value = row.get(index);
                return new javafx.beans.property.SimpleStringProperty(value != null ? value.toString() : "");
            });

            tableView.getColumns().add(column);
        }

        // 填充数据
        List<Row> rows = JavaConverters.seqAsJavaList(df.collect().toSeq());
        ObservableList<Row> data = FXCollections.observableArrayList(rows);
        tableView.setItems(data);


        if(currentUrl.contains("listDataSetNames")){
            tableView.setRowFactory(tv -> {
                TableRow<Row> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !row.isEmpty()) {
                        Row rowData = row.getItem();
                        String datasetId = rowData.get(0).toString(); // 假设第一列是 datasetId
                        String dfUrl = "dacp://0.0.0.0:3101/listDataFrameNames/" + datasetId; // 构造 DataFrame 列表查询 URL
                        mainController.inputField.setText(dfUrl);
                        mainController.skipQueryList(dfUrl); // 交回主控制器处理
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
                        String dataframeId = rowData.get(0).toString(); // 假设第一列是 datasetId
                        String dfUrl = "dacp://0.0.0.0:3101/get/" + dataframeId; // 构造 DataFrame 列表查询 URL
                        mainController.inputField.setText(dfUrl);
                        mainController.skipQueryList(dfUrl); // 交回主控制器处理
                    }
                });
                return row;
            });
        }

    }
}
