package controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.controls.JFXTreeTableView;
import com.jfoenix.controls.RecursiveTreeItem;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import db.DBConnection;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import model.Item;
import model.tm.ItemTm;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Predicate;

public class ItemFormController implements Initializable {

    @FXML
    private TreeTableColumn colCode;

    @FXML
    private TreeTableColumn colDesc;

    @FXML
    private TreeTableColumn colOption;

    @FXML
    private TreeTableColumn colQtyOnHand;

    @FXML
    private TreeTableColumn colUnitPrice;

    @FXML
    private AnchorPane itemPane;

    @FXML
    private Label lblCode;

    @FXML
    private JFXTreeTableView<ItemTm> tblItem;

    @FXML
    private JFXTextField txtDesc;

    @FXML
    private JFXTextField txtQty;

    @FXML
    private JFXTextField txtSearch;

    @FXML
    private JFXTextField txtUnitPrice;

    public void backButtonOnAction(ActionEvent actionEvent) {
        Stage stage = (Stage) itemPane.getScene().getWindow();
        try {
            stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("../view/DashBoard.fxml"))));
        } catch (IOException e) {
            e.printStackTrace();
        }
        stage.show();
    }

    @FXML
    void clearButtonOnAction(ActionEvent event) {
        clearFields();
    }

    private void clearFields() {
        generateId();
        txtDesc.clear();
        txtUnitPrice.clear();
        txtQty.clear();
        txtSearch.clear();
        tblItem.refresh();
    }

    @FXML
    void saveButtonOnAction(ActionEvent event) {
        Item item = new Item(
                lblCode.getText(),
                txtDesc.getText(),
                Double.parseDouble(txtUnitPrice.getText()),
                Integer.parseInt(txtQty.getText())
        );

        try {
            Connection connection = DBConnection.getInstance().getConnection();
            String sql = "INSERT INTO item VALUES(?,?,?,?)";
            PreparedStatement pstm = connection.prepareStatement(sql);
            pstm.setString(1,item.getCode());
            pstm.setString(2,item.getDescription());
            pstm.setDouble(3,item.getUnitPrice());
            pstm.setInt(4,item.getQtyOnHand());

            if (pstm.executeUpdate()>0) {
                new Alert(Alert.AlertType.INFORMATION,"Item Saved..!").show();
                loadTable();
                clearFields();
            }else{
                new Alert(Alert.AlertType.ERROR,"Something went wrong..!").show();
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void loadTable() {
        ObservableList<ItemTm> tmList = FXCollections.observableArrayList();
        try {
            List<Item> list = new ArrayList<>();
            Connection connection = DBConnection.getInstance().getConnection();
            PreparedStatement pstm = connection.prepareStatement("SELECT * FROM item");
            ResultSet resultSet = pstm.executeQuery();

            while (resultSet.next()) {
                list.add(new Item(
                        resultSet.getString(1),
                        resultSet.getString(2),
                        resultSet.getDouble(3),
                        resultSet.getInt(4)
                ));
            }

            for (Item item:list) {
                JFXButton btn = new JFXButton("Delete");
                btn.setTextFill(Color.rgb(255,255,255));
                btn.setStyle("-fx-background-color: #e35c5c; -fx-font-weight: BOLD");


                btn.setOnAction(actionEvent -> {
                    try {
                        PreparedStatement pst = connection.prepareStatement("DELETE FROM item WHERE code=?");
                        pst.setString(1,item.getCode());
                        Optional<ButtonType> buttonType = new Alert(Alert.AlertType.CONFIRMATION, "Do you want to delete " + item.getCode() + " item ? ", ButtonType.YES, ButtonType.NO).showAndWait();
                        if (buttonType.get() == ButtonType.YES){
                            if (pst.executeUpdate()>0){
                                new Alert(Alert.AlertType.INFORMATION,"Item Deleted..!").show();
                                loadTable();
                                generateId();
                            }else{
                                new Alert(Alert.AlertType.ERROR,"Something went wrong..!").show();
                            }
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });

                tmList.add(new ItemTm(
                        item.getCode(),
                        item.getDescription(),
                        item.getUnitPrice(),
                        item.getQtyOnHand(),
                        btn
                ));
            }

            TreeItem<ItemTm> treeItem = new RecursiveTreeItem<>(tmList, RecursiveTreeObject::getChildren);
            tblItem.setRoot(treeItem);
            tblItem.setShowRoot(false);

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void generateId() {
        try {
            Connection connection = DBConnection.getInstance().getConnection();
            PreparedStatement pstm = connection.prepareStatement("SELECT code FROM item ORDER BY code DESC LIMIT 1");
            ResultSet resultSet = pstm.executeQuery();
            if (resultSet.next()){
                int num = Integer.parseInt(resultSet.getString(1).split("[P]")[1]);
                num++;
                lblCode.setText(String.format("P%03d",num));
            }else {
                lblCode.setText("P001");
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void updateButtonOnAction(ActionEvent event) {
        Item item = new Item(
                lblCode.getText(),
                txtDesc.getText(),
                Double.parseDouble(txtUnitPrice.getText()),
                Integer.parseInt(txtQty.getText())
        );

        try {
            Connection connection = DBConnection.getInstance().getConnection();
            PreparedStatement pstm = connection.prepareStatement("UPDATE item SET description=? , unitPrice=?, qtyOnHand=? WHERE code=?");
            pstm.setString(4,item.getCode());
            pstm.setString(1,item.getDescription());
            pstm.setDouble(2,item.getUnitPrice());
            pstm.setInt(3,item.getQtyOnHand());

            if (pstm.executeUpdate()>0){
                new Alert(Alert.AlertType.INFORMATION,"Item Updated..!").show();
                clearFields();
                loadTable();
            }else{
                new Alert(Alert.AlertType.ERROR,"Something went wrong..!").show();
            }

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        colCode.setCellValueFactory(new TreeItemPropertyValueFactory<>("code"));
        colDesc.setCellValueFactory(new TreeItemPropertyValueFactory<>("description"));
        colUnitPrice.setCellValueFactory(new TreeItemPropertyValueFactory<>("unitPrice"));
        colQtyOnHand.setCellValueFactory(new TreeItemPropertyValueFactory<>("qtyOnHand"));
        colOption.setCellValueFactory(new TreeItemPropertyValueFactory<>("btn"));
        generateId();
        loadTable();

        tblItem.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) ->{
            if (newValue!=null){
                setData(newValue);
            }
        });

        txtSearch.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {
                tblItem.setPredicate(new Predicate<TreeItem<ItemTm>>() {
                    @Override
                    public boolean test(TreeItem<ItemTm> treeItem) {
                        boolean flag = treeItem.getValue().getCode().contains(newValue) ||
                                treeItem.getValue().getDescription().contains(newValue);
                        return flag;
                    }
                });
            }
        });

    }

    private void setData(TreeItem<ItemTm> value) {
        lblCode.setText(value.getValue().getCode());
        txtDesc.setText(value.getValue().getDescription());
        txtUnitPrice.setText(String.valueOf(value.getValue().getUnitPrice()));
        txtQty.setText(String.valueOf(value.getValue().getQtyOnHand()));
    }
}
