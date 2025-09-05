package link.rdcn.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;

public class FavoriteController {

    @FXML
    private ListView<String> favoriteListView;

    private MainController mainController;

    public void setMainController(MainController controller) {
        this.mainController = controller;
    }

    @FXML
    public void initialize() {
        favoriteListView.getItems().setAll(FavoriteManager.getFavorites());

        favoriteListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) { // 双击打开
                String selectedUrl = favoriteListView.getSelectionModel().getSelectedItem();
                if (selectedUrl != null && mainController != null) {
                    mainController.skipQueryList(selectedUrl); // 调用 TestController 的查询方法
                }
            }
        });
    }
}
