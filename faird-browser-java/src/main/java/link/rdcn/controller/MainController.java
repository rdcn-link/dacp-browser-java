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
import org.apache.arrow.vector.VarCharVector;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.apache.arrow.vector.types.pojo.Schema;
import scala.collection.JavaConverters;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ArrowFileWriter;


import java.io.FileOutputStream;
import java.nio.channels.Channels;

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
        FairdClient oldClient = this.fairdClient;
        // 先断开与旧 client 相关的 DataFrame（如果存在）
        if (this.currentDf != null) {
            try {
                // 假设没有 close 方法，则至少把引用清掉，避免后续访问
                this.currentDf = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 先关闭旧 client（确保所有 FlightStream/资源都应已被释放）
        if (oldClient != null) {
            try {
                oldClient.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 最后设置新 client
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
            List<String> favorites = new ArrayList<>(FavoriteManager.getFavorites().values());
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
                        suggestionMenu.hide();
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
                viewFavoritesItem.setHideOnClick(false);
                viewFavoritesLabel.setCursor(Cursor.HAND);// 点击不会自动隐藏
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
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV File", "*.csv"),
                new FileChooser.ExtensionFilter("Arrow File", "*.arrow")
        );
        File file = fileChooser.showSaveDialog(rootPane.getScene().getWindow());

        if (file != null) {
            String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(".csv")) {
                saveAsCsv(file);
            } else if (fileName.endsWith(".arrow")) {
                saveAsArrow(file);
            }
        }
    }

    private static void writeCsvBatchSafe(FileWriter writer, List<Row> batch, List<Column> javaFields) throws Exception {
        for (Row row : batch) {
            for (int i = 0; i < javaFields.size(); i++) {
                Object value = row.get(i);
                String safeValue = value == null ? "" : value.toString().replace("\"", "\"\"");

                // 处理数字列，避免空字符串报错
                if (isNumericColumn(javaFields.get(i))) {
                    if (safeValue.isEmpty()) {
                        safeValue = "0"; // 或者用 "" 保持空值
                    }
                }

                writer.write("\"" + safeValue + "\"");
                if (i < javaFields.size() - 1) writer.write(",");
            }
            writer.write("\n");
        }
    }

    // 判断是否是数字列，可以根据你的实际类型判断
    private static boolean isNumericColumn(Column column) {
        String typeName = column.colType().toString(); // 这里假设 Column 有 dataType 方法
        return typeName.equalsIgnoreCase("double") || typeName.equalsIgnoreCase("float") ||
                typeName.equalsIgnoreCase("int") || typeName.equalsIgnoreCase("long");
    }



    private void saveAsCsv(File file) {
        final int batchSize = 1000; // 每 1000 行 flush 一次

        try (FileWriter writer = new FileWriter(file)) {
            // 写表头
            List<Column> javaFields = JavaConverters.seqAsJavaList(currentDf.schema().columns().toSeq());
            writer.write(String.join(",", javaFields.stream().map(Column::name).toArray(String[]::new)));
            writer.write("\n");

            // 准备一个用于批处理的 List
            List<Row> batch = new ArrayList<>(batchSize);

            // 使用 mapIterator，但进行两处关键修改
            currentDf.mapIterator(it -> {
                // 在 lambda 内部处理所有数据
                while (it.hasNext()) {
                    Row row = it.next();
                    batch.add(row);

                    if (batch.size() >= batchSize) {
                        try {
                            writeCsvBatchSafe(writer, batch, javaFields);
                        } catch (Exception e) {
                            // 在 lambda 内部，通常需要将检查型异常转换为运行时异常
                            throw new RuntimeException(e);
                        }
//                        System.out.println("写入批次，当前已处理批次数：" + batch.size());
                        batch.clear();
                    }
                }
                // 1. 【重要修改】将写入剩余数据的逻辑移到 lambda 内部
                // 确保在迭代器消费完毕后，立刻处理最后一批数据。
                if (!batch.isEmpty()) {
                    try {
                        writeCsvBatchSafe(writer, batch, javaFields);
                        System.out.println("写入最后一批剩余数据。");
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                // 2. 【重要修改】返回一个空的迭代器，而不是 null
                // 这遵守了 mapIterator 的 API 约定，可以防止框架崩溃。
                return java.util.Collections.emptyIterator();
            });
            System.out.println("CSV 文件导出操作已提交。");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private void saveAsArrow(File file) {
        try (RootAllocator allocator = new RootAllocator(Long.MAX_VALUE)) {

            List<Column> javaFields = JavaConverters.seqAsJavaList(currentDf.schema().columns().toSeq());
            List<Field> fields = new ArrayList<>();

            // 构建 Arrow Schema（先全部 Utf8，如果需要可以细化 Int/Double）
            for (Column col : javaFields) {
                FieldType fieldType = new FieldType(true, ArrowType.Utf8.INSTANCE, null);
                fields.add(new Field(col.name(), fieldType, null));
            }

            Schema schema = new Schema(fields);

            try (VectorSchemaRoot root = VectorSchemaRoot.create(schema, allocator);
                 FileOutputStream fos = new FileOutputStream(file);
                 WritableByteChannel channel = Channels.newChannel(fos);
                 ArrowFileWriter writer = new ArrowFileWriter(root, null, channel)) {

                writer.start();

                int batchSize = 1024;
                List<Row> buffer = new ArrayList<>(batchSize);
                AtomicInteger cnt = new AtomicInteger();

                // ✅ 用 mapIterator 消费 ClosableIterator<Row>
                currentDf.mapIterator(it -> {
                    try {
                        while (it.hasNext()) {
                            buffer.clear();
                            for (int i = 0; i < batchSize && it.hasNext(); i++) {
                                buffer.add(it.next());
                            }
                            int rowCount = buffer.size();
                            // 填充 Arrow 向量
                            for (int j = 0; j < javaFields.size(); j++) {
                                VarCharVector vec = (VarCharVector) root.getVector(j);
                                vec.allocateNew(rowCount);
                                for (int i = 0; i < rowCount; i++) {
                                    Object value = buffer.get(i).get(j);
                                    if (value != null) {
                                        vec.setSafe(i, value.toString().getBytes());
                                    }
                                }
                                vec.setValueCount(rowCount);
                            }

                            root.setRowCount(rowCount);
                            writer.writeBatch();
                            cnt.getAndIncrement();
//                            System.out.println(cnt.get());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null; // 因为 mapIterator 要有返回值，这里没用就返回 null
                });

                writer.end();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }





//    @FXML
//    private void handleStar(ActionEvent event) {
//        String currentUrl = inputField.getText();
//        isCollected = !isCollected;
//        if (isCollected) {
//            starButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #FFD700; -fx-font-size: 18px;");
//            System.out.println("Favorited URL: " + currentUrl);
//            FavoriteManager.addFavorite(currentUrl);
//        } else {
//            starButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #999999; -fx-font-size: 18px;");
//            System.out.println("Unfavorited URL: " + currentUrl);
//            FavoriteManager.removeFavorite(currentUrl);
//        }
//    }
    @FXML
    private void handleStar(ActionEvent event) {
        String currentUrl = inputField.getText();
        isCollected = !isCollected;

        if (isCollected) {
            // 弹出输入框，获取收藏名
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("收藏网址");
            dialog.setHeaderText("请输入收藏URL的名称");
            dialog.setContentText("名称：");

            dialog.showAndWait().ifPresent(name -> {
                starButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #FFD700; -fx-font-size: 18px;");
                System.out.println("Favorited: " + name + " -> " + currentUrl);
                FavoriteManager.addFavorite(name, currentUrl);
            });

        } else {
            // 如果取消收藏，默认按 URL 删除
            starButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #999999; -fx-font-size: 18px;");
            System.out.println("Unfavorited URL: " + currentUrl);
            FavoriteManager.removeFavorite(currentUrl);
        }
    }


    @FXML
    private void openFavorites() {
        try {
            currentUrl = "";
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
        ignoreTextChange = true;
        queryAndShow(skipURL);
        ignoreTextChange = false;
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
