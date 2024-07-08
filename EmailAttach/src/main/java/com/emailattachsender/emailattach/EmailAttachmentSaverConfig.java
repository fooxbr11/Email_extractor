package com.emailattachsender.emailattach;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class EmailAttachmentSaverConfig extends Application {

    private TextField hostField;
    private TextField usernameField;
    private PasswordField passwordField;
    private TextField destDirField;
    private TextField inboxField;
    private Properties config;

    private static final Logger logger = Logger.getLogger(EmailAttachmentSaverConfig.class.getName());
    private static String CONFIG_FILE_PATH;

    public static void main(String[] args) {
        setupLogger();
        determineConfigFilePath();
        if (args.length > 0 && args[0].equals("service")) {
            runService();
        } else {
            launch(args);
        }
    }

    private static void determineConfigFilePath() {
        try {
            String jarDir = Paths.get(EmailAttachmentSaverConfig.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent().toString();
            CONFIG_FILE_PATH = jarDir + File.separator + "config.properties";
        } catch (URISyntaxException e) {
            e.printStackTrace();
            CONFIG_FILE_PATH = "config.properties"; // fallback
        }
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Configurações de Download de Anexos de E-mail");

        // Carregar o ícone
        try (InputStream iconStream = getClass().getResourceAsStream("/icon.png")) {
            if (iconStream != null) {
                primaryStage.getIcons().add(new Image(iconStream));
            } else {
                System.out.println("Ícone não encontrado. Continuando sem ícone.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.severe("Erro ao carregar ícone: " + e.getMessage());
        }

        // Carregar propriedades do arquivo de configuração
        config = new Properties();
        File configFile = new File(CONFIG_FILE_PATH);
        if (configFile.exists()) {
            try (InputStream input = new FileInputStream(configFile)) {
                config.load(input);
                logger.info("Configurações carregadas com sucesso.");
            } catch (IOException e) {
                e.printStackTrace();
                logger.severe("Erro ao carregar configurações: " + e.getMessage());
            }
        } else {
            logger.warning("Arquivo de configuração não encontrado. Usando valores padrão.");
            // Usar valores padrão
            config.setProperty("host", "imap.gmail.com");
            config.setProperty("username", "");
            config.setProperty("password", "");
            config.setProperty("inbox", "INBOX");
            String currentDir = System.getProperty("user.dir");
            String defaultDestDir = currentDir + File.separator + "pdfsaver";
            config.setProperty("destDir", defaultDestDir);
        }

        // Obter o diretório atual e definir o diretório padrão como "pdfsaver"
        String currentDir = System.getProperty("user.dir");
        String defaultDestDir = currentDir + File.separator + "pdfsaver";
        File pdfsaverDir = new File(defaultDestDir);
        if (!pdfsaverDir.exists()) {
            pdfsaverDir.mkdirs();
        }

        // Criação dos campos de entrada
        hostField = new TextField(config.getProperty("host", "imap.gmail.com"));
        usernameField = new TextField(config.getProperty("username"));
        passwordField = new PasswordField();
        passwordField.setText(config.getProperty("password"));
        destDirField = new TextField(config.getProperty("destDir", defaultDestDir));
        inboxField = new TextField(config.getProperty("inbox", "INBOX"));

        // Configuração do layout
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20, 20, 20, 20));
        grid.setVgap(10);
        grid.setHgap(10);
        grid.setAlignment(Pos.CENTER);

        // Adicionar componentes ao layout
        grid.add(new Label("Servidor IMAP:"), 0, 0);
        grid.add(hostField, 1, 0);
        grid.add(new Label("E-mail:"), 0, 1);
        grid.add(usernameField, 1, 1);
        grid.add(new Label("Senha de App:"), 0, 2);
        grid.add(passwordField, 1, 2);
        grid.add(new Label("Diretório de Destino:"), 0, 3);
        grid.add(destDirField, 1, 3);
        grid.add(new Label("Caixa de Entrada:"), 0, 4);
        grid.add(inboxField, 1, 4);

        Button browseButton = new Button("Procurar...");
        browseButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File selectedDirectory = directoryChooser.showDialog(primaryStage);
            if (selectedDirectory != null) {
                destDirField.setText(selectedDirectory.getAbsolutePath());
            }
        });
        grid.add(browseButton, 2, 3);

        Button saveButton = new Button("Salvar Configurações");
        Button runButton = new Button("Executar Download");

        HBox buttonBox = new HBox(10, saveButton, runButton);
        buttonBox.setAlignment(Pos.CENTER);
        grid.add(buttonBox, 1, 5);

        saveButton.setOnAction(e -> saveConfig());
        runButton.setOnAction(e -> runDownloader());

        // Mostrar a cena
        Scene scene = new Scene(grid, 500, 300);

        // Carregar o CSS
        String css = getClass().getResource("/style.css").toExternalForm();
        if (css != null) {
            scene.getStylesheets().add(css);
        } else {
            System.out.println("Arquivo CSS não encontrado. Continuando sem estilização.");
            logger.warning("Arquivo CSS não encontrado. Continuando sem estilização.");
        }

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void saveConfig() {
        config.setProperty("host", hostField.getText());
        config.setProperty("username", usernameField.getText());
        config.setProperty("password", passwordField.getText());
        config.setProperty("destDir", destDirField.getText());
        config.setProperty("inbox", inboxField.getText());

        try (OutputStream output = new FileOutputStream(CONFIG_FILE_PATH)) {
            config.store(output, null);
            showAlert(Alert.AlertType.INFORMATION, "Configurações Salvas", "As configurações foram salvas com sucesso.");
            logger.info("Configurações salvas com sucesso.");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erro", "Não foi possível salvar as configurações.");
            logger.severe("Erro ao salvar configurações: " + e.getMessage());
        }
    }

    private void runDownloader() {
        EmailAttachmentDownloader downloader = new EmailAttachmentDownloader();
        try {
            downloader.downloadAttachments(config);
            showAlert(Alert.AlertType.INFORMATION, "Sucesso", "Download concluído com sucesso.");
            logger.info("Download concluído com sucesso.");
        } catch (Exception e) {
            e.printStackTrace();
            if (e.getMessage().contains("Erro de autenticação")) {
                showAlert(Alert.AlertType.ERROR, "Erro de Autenticação", "Erro de autenticação: verifique seu e-mail e senha.");
                logger.warning("Erro de autenticação: " + e.getMessage());
            } else if (e.getMessage().contains("Nenhum novo arquivo encontrado")) {
                showAlert(Alert.AlertType.INFORMATION, "Nenhum Novo Arquivo", "Nenhum novo arquivo encontrado para download.");
                logger.info("Nenhum novo arquivo encontrado para download.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Erro", "Não foi possível realizar o download: " + e.getMessage());
                logger.severe("Erro ao realizar o download: " + e.getMessage());
            }
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void runService() {
        determineConfigFilePath();
        Properties config = new Properties();
        File configFile = new File(CONFIG_FILE_PATH);
        if (configFile.exists()) {
            try (InputStream input = new FileInputStream(configFile)) {
                config.load(input);
                logger.info("Configurações carregadas com sucesso para o modo serviço.");
            } catch (IOException e) {
                e.printStackTrace();
                logger.severe("Erro ao carregar configurações para o modo serviço: " + e.getMessage());
            }
        } else {
            logger.warning("Arquivo de configuração não encontrado. Usando valores padrão para o modo serviço.");
            // Configurações padrão para o modo serviço
            String currentDir = System.getProperty("user.dir");
            String defaultDestDir = currentDir + File.separator + "pdfsaver";
            config.setProperty("host", "imap.gmail.com");
            config.setProperty("username", ""); 
            config.setProperty("password", ""); 
            config.setProperty("inbox", "INBOX");
            config.setProperty("destDir", defaultDestDir);
        }

        // Verificar se o diretório de destino existe e criar se não existir
        String destDir = config.getProperty("destDir");
        File pdfsaverDir = new File(destDir);
        if (!pdfsaverDir.exists()) {
            pdfsaverDir.mkdirs();
            logger.info("Diretório de destino criado: " + destDir);
        }

        EmailAttachmentDownloader downloader = new EmailAttachmentDownloader();
        try {
            downloader.downloadAttachments(config);
            logger.info("Serviço executado com sucesso.");
        } catch (Exception e) {
            e.printStackTrace();
            logger.severe("Erro ao executar o serviço: " + e.getMessage());
        }
    }

    private static void setupLogger() {
        try {
            // Especificar um caminho absoluto ou relativo para o arquivo de log
            String logDir = "logs";
            File logDirFile = new File(logDir);
            if (!logDirFile.exists()) {
                logDirFile.mkdirs(); // Cria o diretório se não existir
            }
            FileHandler fh = new FileHandler(logDir + "/EmailAttachmentSaver.log", true);
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);
        } catch (IOException e) {
            e.printStackTrace();
            logger.severe("Erro ao configurar logger: " + e.getMessage());
        }
    }
}
