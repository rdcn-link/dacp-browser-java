//package link.rdcn.controller;
//
//import javafx.fxml.FXML;
//import javafx.scene.control.ListView;
//
//public class FavoriteController {
//
//    @FXML
//    private ListView<String> favoriteListView;
//    private MainController mainController;
//    public void setMainController(MainController controller) {
//        this.mainController = controller;
//    }
//
//    @FXML
//    public void initialize() {
//        favoriteListView.getItems().setAll(FavoriteManager.getFavorites());
//
//        favoriteListView.setOnMouseClicked(event -> {
//            if (event.getClickCount() == 2) { // 双击打开
//                String selectedUrl = favoriteListView.getSelectionModel().getSelectedItem();
//                if (selectedUrl != null && mainController != null) {
//                    mainController.skipQueryList(selectedUrl); // 调用 TestController 的查询方法
//                }
//            }
//        });
//    }
//}
package link.rdcn.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;

import java.util.Map;
import java.util.stream.Collectors;

public class FavoriteController {

    @FXML
    private ListView<String> favoriteListView;
    private MainController mainController;

    public void setMainController(MainController controller) {
        this.mainController = controller;
    }

    @FXML
    public void initialize() {
        refreshFavorites();

        favoriteListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) { // 双击打开
                String selectedItem = favoriteListView.getSelectionModel().getSelectedItem();
                if (selectedItem != null && mainController != null) {
                    // 格式: "名称 -> URL"，只取 URL 部分
                    String[] parts = selectedItem.split(" -> ", 2);
                    if (parts.length == 2) {
                        String url = parts[1];
                        System.out.println(url);
                        mainController.skipQueryList(url);
                    }
                }
            }
        });
    }

    // 封装刷新方法
    private void refreshFavorites() {
        Map<String, String> favorites = FavoriteManager.getFavorites();
        favoriteListView.getItems().setAll(
                favorites.entrySet().stream()
                        .map(entry -> entry.getKey() + " -> " + entry.getValue())
                        .collect(Collectors.toList()) // 用 Collectors.toList() 替代 toList()
        );
    }
}
