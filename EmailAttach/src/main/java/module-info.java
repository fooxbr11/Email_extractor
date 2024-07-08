module com.emailattach.emailattachmentsaver {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.mail;
    requires java.sql;
    requires commons.daemon;



    opens com.emailattachsender.emailattach to javafx.fxml;
    exports com.emailattachsender.emailattach;
}
