////package link.rdcn.controller;
////
////import javafx.fxml.FXML;
////import javafx.scene.control.ListView;
////
////public class FavoriteController {
////
////    @FXML
////    private ListView<String> favoriteListView;
////    private MainController mainController;
////    public void setMainController(MainController controller) {
////        this.mainController = controller;
////    }
////
////    @FXML
////    public void initialize() {
////        favoriteListView.getItems().setAll(FavoriteManager.getFavorites());
////
////        favoriteListView.setOnMouseClicked(event -> {
////            if (event.getClickCount() == 2) { // 双击打开
////                String selectedUrl = favoriteListView.getSelectionModel().getSelectedItem();
////                if (selectedUrl != null && mainController != null) {
////                    mainController.skipQueryList(selectedUrl); // 调用 TestController 的查询方法
////                }
////            }
////        });
////    }
////}
//package link.rdcn.controller;
//
//import javafx.fxml.FXML;
//import javafx.scene.control.ListView;
//
//import java.util.Map;
//import java.util.stream.Collectors;
//
//public class FavoriteController {
//
//    @FXML
//    private ListView<String> favoriteListView;
//    private MainController mainController;
//
//    public void setMainController(MainController controller) {
//        this.mainController = controller;
//    }
//
//    @FXML
//    public void initialize() {
//        refreshFavorites();
//
//        favoriteListView.setOnMouseClicked(event -> {
//            if (event.getClickCount() == 1) { // 双击打开
//                String selectedItem = favoriteListView.getSelectionModel().getSelectedItem();
//                if (selectedItem != null && mainController != null) {
//                    // 格式: "名称 -> URL"，只取 URL 部分
//                    String[] parts = selectedItem.split(" -> ", 2);
//                    if (parts.length == 2) {
//                        String url = parts[1];
//                        System.out.println(url);
//                        mainController.skipQueryList(url);
//                    }
//                }
//            }
//        });
//    }
//
//    // 封装刷新方法
//    private void refreshFavorites() {
//        Map<String, String> favorites = FavoriteManager.getFavorites();
//        favoriteListView.getItems().setAll(
//                favorites.entrySet().stream()
//                        .map(entry -> entry.getKey() + " -> " + entry.getValue())
//                        .collect(Collectors.toList()) // 用 Collectors.toList() 替代 toList()
//        );
//    }
//}

package link.rdcn.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FavoriteController {

    @FXML
    private ListView<Map.Entry<String, String>> favoriteListView;
    private MainController mainController;

    public void setMainController(MainController controller) {
        this.mainController = controller;
    }

    @FXML
    public void initialize() {
        refreshFavorites();

        // 自定义 ListCell（兼容 JDK8）
        favoriteListView.setCellFactory(new Callback<ListView<Map.Entry<String, String>>, ListCell<Map.Entry<String, String>>>() {
            @Override
            public ListCell<Map.Entry<String, String>> call(ListView<Map.Entry<String, String>> listView) {
                return new ListCell<Map.Entry<String, String>>() {
                    private final Label nameLabel = new Label();
                    private final Label urlLabel = new Label();
                    private final HBox content = new HBox();

                    {
                        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
                        // 蓝色 + 下划线
                        urlLabel.setStyle("-fx-text-fill: #1a73e8; -fx-underline: true;");
                        content.setSpacing(10);
                        content.getChildren().addAll(nameLabel, urlLabel);
                        HBox.setHgrow(urlLabel, Priority.ALWAYS);
                    }

                    @Override
                    protected void updateItem(Map.Entry<String, String> item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setGraphic(null);
                        } else {
                            nameLabel.setText(item.getKey());
                            urlLabel.setText(item.getValue());
                            setGraphic(content);
                        }
                    }
                };
            }
        });

        // 双击打开链接
        favoriteListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                Map.Entry<String, String> selectedItem = favoriteListView.getSelectionModel().getSelectedItem();
                if (selectedItem != null && mainController != null) {
                    mainController.skipQueryList(selectedItem.getValue());
                }
            }
        });
    }

    // 刷新收藏夹列表
    private void refreshFavorites() {
        Map<String, String> favorites = FavoriteManager.getFavorites();
        List<Map.Entry<String, String>> items = new ArrayList<Map.Entry<String, String>>(favorites.entrySet());
        favoriteListView.getItems().setAll(items);
    }
}
