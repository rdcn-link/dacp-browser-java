package link.rdcn.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Side;
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
//import link.rdcn.server.AuthorProviderTest;
//import link.rdcn.server.DataProviderTest;
//import link.rdcn.server.DataReceiverTest;
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
    private Button loginButton; // top-right login button
    @FXML
    private Button registerButton; // top-right register button

    @FXML
    private TabPane tabPane;

    @FXML
    protected StackPane contentPane;

    @FXML
    private BorderPane rootPane;

    // top-right user icon
    @FXML
    private ImageView userIcon;
    // top-left back-to-home icon
//    @FXML
//    private ImageView back2main;

    @FXML
    private Button starButton;

    @FXML
    private Label timeAndByteLabel;

    public void setTimeAndByteLabel(String timeAndByte) {
        timeAndByteLabel.setText(timeAndByte);
    }

//    private long startTime;

    // state
    private boolean isCollected = false;
    private boolean loggedIn = false; // login state example
    private String username = "Alice"; // logged-in username example
    private double xOffset = 0;
    private double yOffset = 0;
    private double prevX, prevY, prevWidth, prevHeight;
    private boolean maximized = false;

    // DACP server
    private static DacpServer dacpServer;
    private FairdClient fairdClient;

    // user dropdown menu
    ContextMenu userMenu = new ContextMenu();
    MenuItem usernameItem = new MenuItem();
    MenuItem logoutItem = new MenuItem("Log out");

    private final Stack<String> backStack = new Stack<>();   // back history
    private final Stack<String> forwardStack = new Stack<>(); // forward history

//    private void queryAndShowWithoutStack(String url) {
//        try {
//            inputField.setText(url);
//
//            DataFrame df = fairdClient.get(url);
//            currentDf = df;
//
//            FXMLLoader loader = new FXMLLoader(getClass().getResource("/list.fxml"));
//            BorderPane listRoot = loader.load();
//
//            ListController controller = loader.getController();
//            controller.setCurrentUrl(url);
//            controller.setDataFrame(df);
//            controller.setMainController(this);
//
//            contentPane.getChildren().clear();
//            contentPane.getChildren().add(listRoot);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

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
            queryAndShow(previousUrl);
        }
    }

    @FXML
    private void goForward() {
        if (!forwardStack.isEmpty()) {
            backStack.push(this.currentUrl);
            String nextUrl = forwardStack.pop();
            queryAndShow(nextUrl);
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
            // fallback: stage not initialized
            throw new IllegalStateException("Stage not initialized");
        }
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
                // favorited -> highlight
                isCollected = true;
                starButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #FFD700; -fx-font-size: 18px;");
            } else {
                // not favorited -> gray
                isCollected = false;
                starButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #999999; -fx-font-size: 18px;");
            }
        });

        // menu items for not-logged-in state
        MenuItem loginItem = new MenuItem("Login");
        loginItem.setOnAction(e -> login());
//        MenuItem registerItem = new MenuItem("Register");
//        registerItem.setOnAction(e -> register());

        MenuItem favoriteItem = new MenuItem("Favorites");
        favoriteItem.setOnAction(event -> openFavorites());

        // logged-in state
        logoutItem.setOnAction(e -> logout());

        // click avatar to show menu
        userIcon.setOnMouseClicked(event -> {
            userMenu.getItems().clear();

            if (loggedIn) {
                usernameItem.setText(username);
                usernameItem.setDisable(true);
                userMenu.getItems().addAll(usernameItem, favoriteItem, logoutItem);
            } else {
//                userMenu.getItems().addAll(loginItem, registerItem);
                userMenu.getItems().add(loginItem);

            }
            userMenu.show(userIcon, Side.BOTTOM, 0, 5);
        });

//        back2main.setOnMouseClicked(event -> {
//            contentPane.getChildren().clear();
//        });

        Platform.runLater(() -> {
            // get Stage
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
        // update user info on top-right
        updateUserInfo();
    }

    private void updateUserInfo() {
        // example: update top-right label
        if (loggedIn) {
            userMenu.getItems().clear();
            usernameItem.setText(username);
            usernameItem.setDisable(true);
            userMenu.getItems().addAll(usernameItem, logoutItem);
        }
    }

    // login example
    private void login() {
        System.out.println("Executing login logic...");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            AnchorPane loginRoot = loader.load();
            LoginController loginController = loader.getController();
            loginController.setMainController(this);
            // create new Stage
            Stage loginStage = new Stage();
            // set owner
            loginStage.initOwner(contentPane.getScene().getWindow());
            loginStage.initModality(Modality.APPLICATION_MODAL); // block parent window
            loginStage.setTitle("Login");
            loginStage.setScene(new Scene(loginRoot));
            loginStage.setResizable(false);
            loginStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // logout
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
            // save current window position & size
            prevX = stage.getX();
            prevY = stage.getY();
            prevWidth = stage.getWidth();
            prevHeight = stage.getHeight();

            // maximize to visual bounds
            stage.setX(screenBounds.getMinX());
            stage.setY(screenBounds.getMinY());
            stage.setWidth(screenBounds.getWidth());
            stage.setHeight(screenBounds.getHeight());

            maximized = true;
        } else {
            // restore
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
                // write header
                List<Column> javaFields = JavaConverters.seqAsJavaList(currentDf.schema().columns().toSeq());
                writer.write(String.join(",", javaFields.stream().map(Column::name).toArray(String[]::new)));
                writer.write("\n");

                // write rows
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
            controller.setMainController(this); // inject TestController
            contentPane.getChildren().clear();
            contentPane.getChildren().add(favoriteRoot);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void queryAndShow(String url) {

        try {
            if (url.contains("/get/") && !this.loggedIn) {
                // show login required
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Access Denied");
                alert.setHeaderText(null);
                alert.setContentText("This data requires sign-in. Please log in first!");
                alert.showAndWait();

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
                Parent root = loader.load();
                // get controller
                LoginController loginController = loader.getController();
                loginController.setMainController(this);
                // open login dialog
                Stage loginStage = new Stage();
                loginStage.initModality(Modality.APPLICATION_MODAL); // block parent
                loginStage.setTitle("User Login");
                loginStage.setScene(new Scene(root));
                loginStage.setResizable(false);
                loginStage.showAndWait();
                return;
            }
            // 2. get DataFrame
            try {
//                startTime = System.currentTimeMillis();
                DataFrame df = fairdClient.get(url);
//                long elapsed = System.currentTimeMillis() - startTime;

//                long hours = elapsed / (1000 * 60 * 60);
//                long minutes = (elapsed / (1000 * 60)) % 60;
//                long seconds = (elapsed / 1000) % 60;
//                long millis = elapsed % 1000;

//                String time = String.format("Run Time: %02d:%02d:%02d.%03d", hours, minutes, seconds, millis);
//                runtimeLabel.setText(time);

                currentDf = df;
                // 3. load list.fxml
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/list.fxml"));
                BorderPane listRoot = loader.load();
                // 4. pass DataFrame to ListController
                ListController controller = loader.getController();
                controller.setMainController(this);

//                controller.setUrl(url);
                controller.setCurrentUrl(url);
                controller.setDataFrame(df);
                // replace center content
                contentPane.getChildren().clear();
                contentPane.getChildren().add(listRoot);

                this.currentUrl = url;
                // keep input field synced
                inputField.setText(url);

                backButton.setDisable(backStack.isEmpty());
                forwardButton.setDisable(forwardStack.isEmpty());

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
        // validate target URL and avoid redundant reload
        if (skipURL == null || skipURL.trim().isEmpty() || skipURL.equals(this.currentUrl)) {
            return;
        }
        // push current URL into back history if exists
        if (!this.currentUrl.isEmpty()) {
            backStack.push(this.currentUrl);
        }
        // clear forward history for a new navigation path
        forwardStack.clear();
        // load new page
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

    @FXML
    private void login(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml")); // login page FXML
            Parent root = loader.load();
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("User Login");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
