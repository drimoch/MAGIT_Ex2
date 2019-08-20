package main.mainWindow;

import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Menu;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import main.MainEngine;
import javafx.beans.property.SimpleBooleanProperty;


public class MainWindowController {
    @FXML
    public Menu changesMenu;
    @FXML
    private Button HistoryBtn;
    @FXML
    private Button OpenChangesBtn;
    private MainEngine m_mainEngine;

    private Stage m_primaryStage;
    private SimpleBooleanProperty m_isRepositoryLoaded;

    public MainWindowController() {
        m_isRepositoryLoaded = new SimpleBooleanProperty(false);
    }

    @FXML
    private void initialize() {
        changesMenu.disableProperty().bind(m_isRepositoryLoaded.not());
        HistoryBtn.disableProperty().bind(m_isRepositoryLoaded.not());
        OpenChangesBtn.disableProperty().bind(m_isRepositoryLoaded.not());

    }

    public void setPrimaryStage(Stage i_primaryStage) {
        m_primaryStage = i_primaryStage;
    }

    public void setEngine(MainEngine i_mainEngine) {
        m_mainEngine = i_mainEngine;
    }

    public void OnClick_LoadRepoMenuItem(ActionEvent actionEvent) {
        m_isRepositoryLoaded.set(true);
    }
}
