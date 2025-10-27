package link.rdcn.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class ErrorController {

    @FXML
    private Label statusLabel;

    @FXML
    private Label messageLabel;

    @FXML
    private Button actionButton; // 对应 FXML 中的按钮 fx:id="actionButton"

    private MainController mainController;

    public void setMainController(MainController controller) {
        this.mainController = controller;
    }

    /**
     * 设置错误页面信息
     * @param statusCode HTTP 状态码，例如 "403 Forbidden"
     * @param errorMessage 错误信息
     */
    public void showError(String statusCode, String errorMessage) {
        statusLabel.setText(statusCode);
        messageLabel.setText(errorMessage);

        // 判断是否为需要登录的错误（401 或 403）
        if ("401 Unauthorized".equalsIgnoreCase(statusCode) || "403 Forbidden".equalsIgnoreCase(statusCode)) {
            actionButton.setVisible(true);
            actionButton.setText("Login");
        } else {
            actionButton.setVisible(false);
        }
    }

    /**
     * 根据异常类型自动映射状态码
     */
    public void showErrorByException(Exception e) {
        String statusCode;
        System.out.println(e.getMessage());
        String err = e.toString();
        if (err.contains("UNAUTHORIZED") || err.contains("unauthorized")) {
            statusCode = "403 Forbidden";
        } else if (err.contains("UNAUTHENTICATED") || err.contains("unauthenticated")) {
            statusCode = "401 Unauthorized";
        } else if (err.contains("NOT FOUND") || err.contains("not found")) {
            statusCode = "404 Not Found";
        } else {
            statusCode = "500 Internal Error";
        }

        // 解析出更简短的信息
        String message = err.contains("Exception:") ? err.split("Exception:")[1].trim() : err;
        showError(statusCode, message);
    }

    /**
     * 登录按钮点击事件
     */
    @FXML
    private void onActionButtonClicked() {
        try {
            // 加载登录页面（请修改路径为你项目的登录 FXML）
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load();
            LoginController loginController = loader.getController();
            loginController.setMainController(mainController);

            Stage stage = (Stage) actionButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Login");
            stage.show();

        } catch (Exception ex) {
            System.err.println("跳转登录页失败: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * 如果仍保留“返回”逻辑，可保留该方法（不再关联按钮）
     */
    @FXML
    private void onBackClicked() {
        System.out.println("返回按钮被点击");
    }
}
