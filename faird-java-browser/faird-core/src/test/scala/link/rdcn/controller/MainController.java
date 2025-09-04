package link.rdcn.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.*;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import link.rdcn.FairdConfig;
import link.rdcn.client.dacp.DacpClient;
import link.rdcn.server.AuthorProviderTest;
import link.rdcn.server.DataProviderTest;
import link.rdcn.server.DataReceiverTest;
import link.rdcn.server.dacp.DacpServer;
import link.rdcn.struct.Column;
import link.rdcn.struct.DataFrame;
import link.rdcn.struct.Row;
import link.rdcn.user.Credentials;
import scala.Array;
import scala.collection.JavaConverters;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Stack;

public class MainController {

    @FXML
    private Button backButton;
    @FXML
    private Button forwardButton;

    private String currentUrl = "";


    private DataFrame currentDf;


    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    protected TextField inputField;

    @FXML
    private Button loginButton; // 右上角登录按钮
    @FXML
    private Button registerButton; // 右上角注册按钮

    @FXML
    private TabPane tabPane;

    @FXML
    protected StackPane contentPane;

    @FXML
    private BorderPane rootPane;

    // 右上角用户图标
    @FXML
    private ImageView userIcon;
    // 左上角退回主页图标
//    @FXML
//    private ImageView back2main;

    @FXML
    private Button starButton;

    // 状态变量
    private boolean isCollected = false;
    private boolean loggedIn = false; // 登录状态示例
    private String username = "Alice"; // 登录用户名示例
    private double xOffset = 0;
    private double yOffset = 0;
    private double prevX, prevY, prevWidth, prevHeight;
    private boolean maximized = false;

    // DACP服务器
    private static DacpServer dacpServer;
    private DacpClient dacpClient;

    // 创建用户下拉菜单
    ContextMenu userMenu = new ContextMenu();
    MenuItem usernameItem = new MenuItem();
    MenuItem logoutItem = new MenuItem("退出登录");


    private final Stack<String> backStack = new Stack<>();   // 回退历史
    private final Stack<String> forwardStack = new Stack<>(); // 前进历史

    private void queryAndShowWithoutStack(String url) {
        try {
            inputField.setText(url);

            DataFrame df = dacpClient.get(url, Array.emptyByteArray());
            currentDf = df;

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/list.fxml"));
            BorderPane listRoot = loader.load();

            ListController controller = loader.getController();
            controller.setCurrentUrl(url);
            controller.setDataFrame(df);
            controller.setMainController(this);

            contentPane.getChildren().clear();
            contentPane.getChildren().add(listRoot);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void refreshPage(){
        System.out.println("Attempting to refresh page for URL: " + this.currentUrl);

        // 1. 检查当前是否存在可以刷新的URL
        //    如果 currentUrl 为空或空白，说明当前是主页或未进行任何查询，无需刷新
        if (this.currentUrl != null && !this.currentUrl.isEmpty()) {

            // 2. 直接使用当前页面的URL调用 queryAndShow 方法
            //    该方法会异步重新获取数据并刷新界面
            queryAndShow(this.currentUrl);

        } else {
            // (可选) 如果没有页面可以刷新，可以在控制台打印一条信息
            System.out.println("No active page to refresh.");
        }
    }


    @FXML
    private void goBack() {
        if (!backStack.isEmpty()) {
            // 1. 将当前页面URL压入前进栈
            forwardStack.push(this.currentUrl);
            // 2. 从后退栈中弹出目标URL
            String previousUrl = backStack.pop();
            // 3. 显示目标页面
            queryAndShow(previousUrl);
        }
    }

    @FXML
    private void goForward() {
        if (!forwardStack.isEmpty()) {
            // 1. 将当前页面URL压入后退栈
            backStack.push(this.currentUrl);
            // 2. 从前进栈中弹出目标URL
            String nextUrl = forwardStack.pop();
            // 3. 显示目标页面
            queryAndShow(nextUrl);
        }
    }


    protected void setDacpClient(DacpClient dacpClient) {
        if (this.dacpClient != null) {
            this.dacpClient.close();
        }
        this.dacpClient = dacpClient;
    }


    private Stage getStage() {
        if (contentPane != null && contentPane.getScene() != null) {
            return (Stage) contentPane.getScene().getWindow();
        } else {
            // 兜底处理，可以返回 null 或抛异常
            throw new IllegalStateException("Stage 未初始化");
        }
    }

    @FXML
    void initialize() throws IOException {
        if (dacpClient == null) {
            dacpClient = getClient();
        }

        backButton.setDisable(backStack.isEmpty());
        forwardButton.setDisable(forwardStack.isEmpty());

        inputField.setOnAction(this::queryList);

        inputField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (FavoriteManager.containFavorites(newVal)) {
                // 已收藏 -> 高亮
                isCollected = true;
                starButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #FFD700; -fx-font-size: 18px;");
            } else {
                // 未收藏 -> 变灰
                isCollected = false;
                starButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #999999; -fx-font-size: 18px;");
            }
        });

        // 未登录状态
        MenuItem loginItem = new MenuItem("登录");
        loginItem.setOnAction(e -> login());
        MenuItem registerItem = new MenuItem("注册");
        registerItem.setOnAction(e -> register());

        MenuItem favoriteItem = new MenuItem("收藏历史");
        favoriteItem.setOnAction(event -> openFavorites());

        // 已登录状态
        // 显示用户名
        logoutItem.setOnAction(e -> logout());

        // 点击头像显示菜单
        userIcon.setOnMouseClicked(event -> {
            userMenu.getItems().clear(); // 清空之前内容

            if (loggedIn) {
                usernameItem.setText(username);
                usernameItem.setDisable(true); // 只是显示，不可点击
                userMenu.getItems().addAll(usernameItem, favoriteItem, logoutItem);
            } else {
                userMenu.getItems().addAll(loginItem, registerItem);
            }
            userMenu.show(userIcon, Side.BOTTOM, 0, 5); // 在头像下方弹出
        });

//        back2main.setOnMouseClicked(event -> {
//            contentPane.getChildren().clear();
//        });

        Platform.runLater(() -> {
            // 获取 Stage
//            Stage stage = (Stage) tabPane.getScene().getWindow();
            Stage stage = getStage();
            tabPane.setOnMousePressed(event -> {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            });

            tabPane.setOnMouseDragged(event -> {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            });
        });
    }

    public void setLoggedIn(String username) {
        this.loggedIn = true;
        this.username = username;
        // 可以在这里更新用户头像右上角显示用户名
        updateUserInfo();
    }

    private void updateUserInfo() {
        // 示例：更新右上角 Label
        if (loggedIn) {
            userMenu.getItems().clear();
            usernameItem.setText(username);
            usernameItem.setDisable(true); // 只是显示，不可点击
            userMenu.getItems().addAll(usernameItem, logoutItem);
        }
    }

    // 登录示例方法
    private void login() {

        System.out.println("执行登录逻辑...");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            AnchorPane loginRoot = loader.load();

            LoginController loginController = loader.getController();
            loginController.setMainController(this);

            // 创建新的 Stage
            Stage loginStage = new Stage();
            // 设置父窗口
            loginStage.initOwner(contentPane.getScene().getWindow());
            loginStage.initModality(Modality.APPLICATION_MODAL); // 阻塞主窗口
            loginStage.setTitle("登录");
            loginStage.setScene(new Scene(loginRoot));
            loginStage.setResizable(false);
            loginStage.showAndWait(); // 显示并等待关闭
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 注册示例方法
    private void register() {
        System.out.println("执行注册逻辑...");
    }

    // 退出登录
    private void logout() {
        loggedIn = false;
        username = "";
        System.out.println("已退出登录");
    }

    @FXML
    private void minimizeWindow() {
        ((Stage) tabPane.getScene().getWindow()).setIconified(true);
    }

    @FXML
    private void maximizeWindow() {
        Stage stage = (Stage) tabPane.getScene().getWindow();

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        if (!maximized) {
            // 保存当前窗口大小和位置
            prevX = stage.getX();
            prevY = stage.getY();
            prevWidth = stage.getWidth();
            prevHeight = stage.getHeight();

            // 最大化到屏幕可用区域（像 Chrome 一样）
            stage.setX(screenBounds.getMinX());
            stage.setY(screenBounds.getMinY());
            stage.setWidth(screenBounds.getWidth());
            stage.setHeight(screenBounds.getHeight());

            maximized = true;
        } else {
            // 恢复到之前的窗口大小（例如 70% 屏幕大小）
            stage.setX(prevX);
            stage.setY(prevY);
            stage.setWidth(prevWidth);
            stage.setHeight(prevHeight);
            maximized = false;
        }
    }

    @FXML
    private void closeWindow() {
        ((Stage) tabPane.getScene().getWindow()).close();
    }

    private DacpClient getClient() {
        if (dacpServer == null) {
            dacpServer = new DacpServer(new DataProviderTest(), new DataReceiverTest());
            dacpServer.addAuthHandler(new AuthorProviderTest());
            dacpServer.start(new FairdConfig());
        }
        if (dacpClient == null) {
            dacpClient = DacpClient.connect("dacp://0.0.0.0:3101", Credentials.ANONYMOUS());
        }
        return dacpClient;
    }

    @FXML
    private void downloadFile() {
        System.out.println("downloading!!!");
        if (currentDf == null) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("保存为 CSV 文件");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV 文件", "*.csv")
        );
        File file = fileChooser.showSaveDialog(rootPane.getScene().getWindow());

        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                // 写表头
                java.util.List<Column> javaFields = JavaConverters.seqAsJavaList(currentDf.schema().columns().toSeq());
                writer.write(String.join(",", javaFields.stream().map(Column::name).toArray(String[]::new)));
                writer.write("\n");

                // 写数据
                List<Row> rows = JavaConverters.seqAsJavaList(currentDf.collect().toSeq());
                for (Row row : rows) {
                    for (int i = 0; i < javaFields.size(); i++) {
                        Object value = row.get(i);
                        writer.write(value != null ? value.toString() : "");
                        if (i < javaFields.size() - 1) {
                            writer.write(",");
                        }
                    }
                    writer.write("\n");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleStar(ActionEvent event) {
        String currentUrl = inputField.getText();
        isCollected = !isCollected;
        if (isCollected) {
            starButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #FFD700; -fx-font-size: 18px;");
            System.out.println("收藏 URL: " + currentUrl);
            FavoriteManager.addFavorite(currentUrl);
        } else {
            starButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #999999; -fx-font-size: 18px;");
            System.out.println("取消收藏 URL: " + currentUrl);
            FavoriteManager.removeFavorite(currentUrl);
        }
    }

    @FXML
    private void openFavorites() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/favorite.fxml"));
            BorderPane favoriteRoot = loader.load();
            FavoriteController controller = loader.getController();
            controller.setMainController(this); // 注入 TestController
            contentPane.getChildren().clear();
            contentPane.getChildren().add(favoriteRoot);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void queryAndShow(String url) {


        try {
            if (url.contains("/get/") && !this.loggedIn) {
                // 弹出登录提示
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("权限不足");
                alert.setHeaderText(null);
                alert.setContentText("访问此数据需要登录，请先登录！");
                alert.showAndWait();

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
                Parent root = loader.load();
                // 获取 login.fxml 的控制器
                LoginController loginController = loader.getController();
                loginController.setMainController(this);
                // 打开登录弹窗
                Stage loginStage = new Stage();
                loginStage.initModality(Modality.APPLICATION_MODAL); // 阻塞父窗口
                loginStage.setTitle("用户登录");
                loginStage.setScene(new Scene(root));
                loginStage.setResizable(false);
                loginStage.showAndWait(); // 如果希望等待用户关闭再继续
                return;
            }
            // 2. 获取 DataFrame
            try {
                DataFrame df = dacpClient.get(url, Array.emptyByteArray());
                currentDf = df;
                // 3. 加载 list.fxml
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/list.fxml"));
                BorderPane listRoot = loader.load();
                // 4. 传递 DataFrame 给 ListController
                ListController controller = loader.getController();
//                controller.setUrl(url);
                controller.setCurrentUrl(url);
                controller.setDataFrame(df);
                controller.setMainController(this);
                // 替换 center 内容
                contentPane.getChildren().clear();
                contentPane.getChildren().add(listRoot);

                this.currentUrl = url;
                // 确保输入框与当前页面URL同步
                inputField.setText(url);

                backButton.setDisable(backStack.isEmpty());
                forwardButton.setDisable(forwardStack.isEmpty());



//                if (currentDf != null) {
//                    // 保存当前 URL 到回退栈
//                    backStack.push(inputField.getText());
//                    forwardStack.clear(); // 新访问，清空前进历史
//                }
//
//                System.out.println(backButton.isDisabled());
//                System.out.println(forwardButton.isDisabled());


            } catch (Exception e) {
                System.out.println(e.getMessage());
                if (e.getMessage().contains("DataFrame is not accessible")) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("权限不足");
                        alert.setHeaderText(null);
                        alert.setContentText("该用户没有访问权限，请切换用户登陆");
                        alert.showAndWait();
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void skipQueryList(String skipURL) {
        // 检查目标URL是否有效，以及是否与当前页面相同，避免不必要的重复加载
        if (skipURL == null || skipURL.trim().isEmpty() || skipURL.equals(this.currentUrl)) {
            return;
        }

        // 同样地，将当前页面URL存入后退历史
        // 确保 currentUrl 已经有值（不是第一次加载）
        if (!this.currentUrl.isEmpty()) {
            backStack.push(this.currentUrl);
        }

        // 清空前进历史，因为这是一次新的导航路径
        forwardStack.clear();

        // 最后，调用 queryAndShow 去加载新页面的内容
        queryAndShow(skipURL);
    }


    @FXML
    void queryList(ActionEvent event) {
//        try {
//            String url = inputField.getText();
//            System.out.println("输入的 URL: " + url);
//
//            queryAndShow(url);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        String newUrl = inputField.getText();
        System.out.println("输入的 URL: " + newUrl);

        // 如果新URL和当前URL相同，或者为空，则不执行任何操作
        if (newUrl == null || newUrl.trim().isEmpty() || newUrl.equals(this.currentUrl)) {
            return;
        }

        // 核心逻辑：
        // 1. 如果当前 URL 不为空，将其压入后退栈，因为它即将成为历史
        if (!this.currentUrl.isEmpty()) {
            backStack.push(this.currentUrl);
        }
        // 2. 用户发起了新的导航，前进历史应该被清空
        forwardStack.clear();

        // 3. 调用重构后的方法去加载内容
        queryAndShow(newUrl);

    }

    @FXML
    private void login(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml")); // 登录页面 FXML
            Parent root = loader.load();
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("用户登录");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 点击注册按钮跳转注册页面
    @FXML
    private void register(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/register.fxml")); // 注册页面 FXML
            Parent root = loader.load();
            Stage stage = (Stage) registerButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("用户注册");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
