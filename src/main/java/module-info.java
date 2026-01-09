module unze.ptf.woodcraft.woodcraft {
    requires javafx.controls;
    requires javafx.fxml;


    opens unze.ptf.woodcraft.woodcraft to javafx.fxml;
    exports unze.ptf.woodcraft.woodcraft;
}