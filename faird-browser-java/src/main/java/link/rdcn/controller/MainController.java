package link.rdcn.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import link.rdcn.FairdConfig;

import link.rdcn.client.dacp.FairdClient;
import link.rdcn.AuthorProviderTest;
import link.rdcn.DataProviderTest;
import link.rdcn.DataReceiverTest;

import link.rdcn.server.dacp.DacpServer;
import link.rdcn.struct.Column;
import link.rdcn.struct.DataFrame;
import link.rdcn.struct.Row;
import link.rdcn.user.Credentials;
import scala.collection.JavaConverters;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

public class MainController {

    private boolean ignoreTextChange = false;

    // 在 MainController 中新增
    private final List<String> historyUrls = new ArrayList<>();
    private ContextMenu suggestionMenu = new ContextMenu();

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
    private TabPane tabPane;

    @FXML
    protected StackPane contentPane;

    @FXML
    private BorderPane rootPane;

    @FXML
    private Button starButton;

    @FXML
    private Label timeAndByteLabel;

    public void setTimeAndByteLabel(String timeAndByte) {
        timeAndByteLabel.setText(timeAndByte);
    }

    // state
    private boolean isCollected = false;
    private boolean loggedIn = false; // login state
    private String username = "";     // logged-in username
    private double xOffset = 0;
    private double yOffset = 0;
    private double prevX, prevY, prevWidth, prevHeight;
    private boolean maximized = false;

    // DACP server
    private static DacpServer dacpServer;
    private FairdClient fairdClient;

    private final Stack<String> backStack = new Stack<>();   // back history
    private final Stack<String> forwardStack = new Stack<>(); // forward history


    @FXML
    private void refreshPage(){
        System.out.println("Attempting to refresh page for URL: " + this.currentUrl);
        if (this.currentUrl != null && !this.currentUrl.isEmpty()) {
            queryAndShow(this.currentUrl);
        } else {
            System.out.println("No active page to refresh.");
        }
    }

    @FXML
    private void goBack() {
        if (!backStack.isEmpty()) {
            forwardStack.push(this.currentUrl);
            String previousUrl = backStack.pop();
            ignoreTextChange = true;    // 防止触发提示
            queryAndShow(previousUrl);
            ignoreTextChange = false;
        }
    }

    @FXML
    private void goForward() {
        if (!forwardStack.isEmpty()) {
            backStack.push(this.currentUrl);
            String nextUrl = forwardStack.pop();
            ignoreTextChange = true;    // 防止触发提示
            queryAndShow(nextUrl);
            ignoreTextChange = false;
        }
    }

    protected void setFaridClient(FairdClient faridClient) {
        if (this.fairdClient != null) {
            this.fairdClient.close();
        }
        this.fairdClient = faridClient;
    }

    private Stage getStage() {
        if (contentPane != null && contentPane.getScene() != null) {
            return (Stage) contentPane.getScene().getWindow();
        } else {
            throw new IllegalStateException("Stage not initialized");
        }
    }


    private boolean hasAccess(String url) {
        return !(url.matches("^dacp://(\\d{1,3}\\.){3}\\d{1,3}:\\d+/.+$") && !url.contains("listDataSets") && !url.contains("listDataFrames") && !url.contains("listHostInfo"));
    }

    @FXML
    void initialize() throws IOException {
        if (fairdClient == null) {
            fairdClient = getClient();
        }

        backButton.setDisable(backStack.isEmpty());
        forwardButton.setDisable(forwardStack.isEmpty());

        inputField.setOnAction(this::queryList);

        inputField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (FavoriteManager.containFavorites(newVal)) {
                isCollected = true;
                starButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #FFD700; -fx-font-size: 18px;");
            } else {
                isCollected = false;
                starButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #999999; -fx-font-size: 18px;");
            }
        });

        inputField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (ignoreTextChange) {
                return; // 页面前进/后退或程序设置，不显示提示
            }

            if (newVal == null || newVal.trim().isEmpty()) {
                suggestionMenu.hide();
                return;
            }

            List<String> favorites = FavoriteManager.getFavorites();
            List<String> historyMatches = historyUrls.stream()
                    .filter(url -> url.startsWith(newVal))
                    .collect(Collectors.toList());

            List<String> favoriteMatches = favorites.stream()
                    .filter(url -> url.contains(newVal))
                    .limit(5)
                    .collect(Collectors.toList());

            if (historyMatches.isEmpty() && favoriteMatches.isEmpty()) {
                suggestionMenu.hide();
                return;
            }

            suggestionMenu.getItems().clear();

            // 历史记录部分
            if (!historyMatches.isEmpty()) {
                CustomMenuItem header = new CustomMenuItem(new Label("历史记录"));
                header.setHideOnClick(false);
                header.setDisable(true);
                suggestionMenu.getItems().add(header);
                for (String url : historyMatches) {
                    HBox itemBox = new HBox();
                    itemBox.setSpacing(5);
                    Label urlLabel = new Label(url);
                    itemBox.getChildren().add(urlLabel);

                    if (!hasAccess(url)) {
                        Label lockLabel = new Label("\uD83D\uDD12"); // 🔒
                        itemBox.getChildren().add(lockLabel);
                    }

                    CustomMenuItem item = new CustomMenuItem(itemBox);
                    item.setOnAction(e -> {
                        inputField.setText(url);
                        suggestionMenu.hide(); // 点击时隐藏
                    });
                    item.setHideOnClick(false); // 可以保留 false 或 true

                    itemBox.setCursor(Cursor.HAND);
                    suggestionMenu.getItems().add(item);
                }
            }

            // 收藏夹部分
            if (!favoriteMatches.isEmpty()) {
                CustomMenuItem header = new CustomMenuItem(new Label("收藏夹"));
                header.setHideOnClick(false);
                header.setDisable(true);
                suggestionMenu.getItems().add(header);
                for (String url : favoriteMatches) {
                    HBox itemBox = new HBox();
                    itemBox.setSpacing(5);
                    Label urlLabel = new Label(url);
                    itemBox.getChildren().add(urlLabel);

                    if (!hasAccess(url)) {
                        Label lockLabel = new Label("\uD83D\uDD12"); // 🔒
                        itemBox.getChildren().add(lockLabel);
                    }

                    CustomMenuItem item = new CustomMenuItem(itemBox);
                    item.setOnAction(e -> {
                        inputField.setText(url);
//                        favoriteMatches.hide(); // 点击时隐藏
                    });
                    item.setHideOnClick(false);
                    itemBox.setCursor(Cursor.HAND);

                    suggestionMenu.getItems().add(item);
                }


                // 添加 “查看收藏夹” 链接
                Label viewFavoritesLabel = new Label("查看收藏夹");
                viewFavoritesLabel.setStyle("-fx-text-fill: blue; -fx-underline: true;"); // 蓝色下划线，像链接
                viewFavoritesLabel.setOnMouseClicked(event -> {
                    openFavorites(); // 调用你的收藏详情方法
                    suggestionMenu.hide();
                });
                CustomMenuItem viewFavoritesItem = new CustomMenuItem(viewFavoritesLabel);
                viewFavoritesItem.setHideOnClick(false); // 点击不会自动隐藏
                suggestionMenu.getItems().add(viewFavoritesItem);
            }


            if (!suggestionMenu.isShowing()) {
                suggestionMenu.show(inputField, Side.BOTTOM, 0, 0);
            }
        });

        Platform.runLater(() -> {
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
    }

    private void logout() {
        loggedIn = false;
        username = "";
        System.out.println("Logged out");
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
            prevX = stage.getX();
            prevY = stage.getY();
            prevWidth = stage.getWidth();
            prevHeight = stage.getHeight();

            stage.setX(screenBounds.getMinX());
            stage.setY(screenBounds.getMinY());
            stage.setWidth(screenBounds.getWidth());
            stage.setHeight(screenBounds.getHeight());

            maximized = true;
        } else {
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

    private FairdClient getClient() {
        if (dacpServer == null) {
            dacpServer = new DacpServer(new DataProviderTest(), new DataReceiverTest(), new AuthorProviderTest());
            dacpServer.start(new FairdConfig());
        }
        if (fairdClient == null) {
            fairdClient = FairdClient.connect("dacp://0.0.0.0:3101", Credentials.ANONYMOUS());
        }
        return fairdClient;
    }

    @FXML
    private void downloadFile() {
        System.out.println("downloading!!!");
        if (currentDf == null) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save as CSV File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV File", "*.csv")
        );
        File file = fileChooser.showSaveDialog(rootPane.getScene().getWindow());

        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                List<Column> javaFields = JavaConverters.seqAsJavaList(currentDf.schema().columns().toSeq());
                writer.write(String.join(",", javaFields.stream().map(Column::name).toArray(String[]::new)));
                writer.write("\n");

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
            System.out.println("Favorited URL: " + currentUrl);
            FavoriteManager.addFavorite(currentUrl);
        } else {
            starButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #999999; -fx-font-size: 18px;");
            System.out.println("Unfavorited URL: " + currentUrl);
            FavoriteManager.removeFavorite(currentUrl);
        }
    }

    @FXML
    private void openFavorites() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/favorite.fxml"));
            BorderPane favoriteRoot = loader.load();
            FavoriteController controller = loader.getController();
            controller.setMainController(this);
            contentPane.getChildren().clear();
            contentPane.getChildren().add(favoriteRoot);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void queryAndShow(String url) {
        try {
            if (!hasAccess(url) && !this.loggedIn) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
                Parent root = loader.load();
                LoginController loginController = loader.getController();
                loginController.setMainController(this);

                Stage loginStage = new Stage();
                loginStage.initModality(Modality.APPLICATION_MODAL);
                loginStage.setTitle("User Login");
                loginStage.setScene(new Scene(root));
                loginStage.setResizable(false);
                loginStage.showAndWait();
                return;
            }
            try {
                DataFrame df = fairdClient.get(url);
                currentDf = df;
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/list.fxml"));
                BorderPane listRoot = loader.load();
                ListController controller = loader.getController();
                controller.setMainController(this);
                controller.setCurrentUrl(url);
                controller.setDataFrame(df);

                contentPane.getChildren().clear();
                contentPane.getChildren().add(listRoot);

                this.currentUrl = url;
                inputField.setText(url);

                backButton.setDisable(backStack.isEmpty());
                forwardButton.setDisable(forwardStack.isEmpty());

                if (!historyUrls.contains(url)) {
                    historyUrls.add(url);
                }

            } catch (Exception e) {
                System.out.println(e.getMessage());
                if (e.getMessage().contains("DataFrame is not accessible")) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Access Denied");
                        alert.setHeaderText(null);
                        alert.setContentText("This user does not have access permission. Please switch user and sign in.");
                        alert.showAndWait();
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void skipQueryList(String skipURL) {
        if (skipURL == null || skipURL.trim().isEmpty() || skipURL.equals(this.currentUrl)) {
            return;
        }
        if (!this.currentUrl.isEmpty()) {
            backStack.push(this.currentUrl);
        }
        forwardStack.clear();
        queryAndShow(skipURL);
    }

    @FXML
    void queryList(ActionEvent event) {
        String newUrl = inputField.getText();
        System.out.println("Input URL: " + newUrl);

        if (newUrl == null || newUrl.trim().isEmpty() || newUrl.equals(this.currentUrl)) {
            return;
        }

        if (!this.currentUrl.isEmpty()) {
            backStack.push(this.currentUrl);
        }
        forwardStack.clear();

        queryAndShow(newUrl);
    }
}
