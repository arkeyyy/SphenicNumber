module org.example.sphenic_number {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.sphenic_number to javafx.fxml;
    exports org.example.sphenic_number;
}