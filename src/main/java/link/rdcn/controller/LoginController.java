package link.rdcn.controller;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
//import link.rdcn.client.dacp.FairdClient;
import link.rdcn.dacp.client.DacpClient;
import link.rdcn.user.UsernamePassword;

import java.util.HashMap;
import java.util.Map;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    public VBox loginVBox;

    private MainController mainController;

    public void setMainController(MainController controller) {
        this.mainController = controller;
    }

    // 模拟内存用户表：用户名 -> 密码
    private static final Map<String, String> userDatabase = new HashMap<>();

    static {
        userDatabase.put("admin", "123456");
        userDatabase.put("user", "password");
        userDatabase.put("Admin", "Admin");
    }

    @FXML
    void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "登录失败", "用户名和密码不能为空");
            return;
        }

        UsernamePassword user = new UsernamePassword(username, password);
        mainController.setFaridClient(DacpClient.connect(mainController.baseUrl, user));

        showAlert(Alert.AlertType.INFORMATION, "登录成功", "欢迎 " + username + "!");
//        openMainPage(event);
        Stage stage = (Stage) loginVBox.getScene().getWindow();
        stage.close();
        mainController.setLoggedIn(username);
    }

    @FXML
    void handleRegister(ActionEvent event) {
        showAlert(Alert.AlertType.INFORMATION, "注册", "注册功能暂未实现");
    }

    @FXML
    void handleForgotPassword(ActionEvent event) {
        showAlert(Alert.AlertType.INFORMATION, "找回密码", "找回密码功能暂未实现");
    }

    private void openMainPage(ActionEvent event) {
        try {
//            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main.fxml")); // 主界面 FXML
//            Parent root = loader.load();
//            TestController controller = loader.getController();
//            controller.setLoggedIn(usernameField.getText());
////            Stage stage = (Stage) loginButton.getScene().getWindow();
//            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
//            stage.setScene(new Scene(root));
//            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

