package controller;

import com.jfoenix.controls.*;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import db.DBConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import model.Order;
import model.OrderDetails;
import model.tm.CartTm;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class PlaceOrderFormController implements Initializable {
    @FXML
    private JFXComboBox cmbCustomerId;

    @FXML
    private JFXComboBox cmbItemCode;

    @FXML
    private TreeTableColumn colAmount;

    @FXML
    private TreeTableColumn colDescription;

    @FXML
    private TreeTableColumn colItemCode;

    @FXML
    private TreeTableColumn colOption;

    @FXML
    private TreeTableColumn colQty;

    @FXML
    private TreeTableColumn colUnitPrice;

    @FXML
    private Label lblOrderId;

    @FXML
    private Label lblQtyOnHand;

    @FXML
    private Label lblTotal;

    @FXML
    private Label lblUnitPrice;

    @FXML
    private AnchorPane placeOrderPane;

    @FXML
    private JFXTreeTableView<CartTm> tblOrder;

    @FXML
    private JFXTextField txtCustomerName;

    @FXML
    private JFXTextField txtDescription;

    @FXML
    private JFXTextField txtQty;

    @FXML
    private JFXTextField txtSearch;

    ObservableList<CartTm> tmList = FXCollections.observableArrayList();

    public void backButtonOnAction(ActionEvent actionEvent) {
        Stage stage = (Stage) placeOrderPane.getScene().getWindow();
        try {
            stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("../view/DashBoard.fxml"))));
        } catch (IOException e) {
            e.printStackTrace();
        }
        stage.show();
    }

    private double findTotal(){
        double total = 0;
        for (CartTm tm:tmList) {
            total += tm.getAmount();
        }
        return total;
    }

    @FXML
    void addToCartButtonOnAction(ActionEvent event) {
        boolean isExist = false;
        for (CartTm tm:tmList) {
            if (tm.getCode().equals(cmbItemCode.getValue().toString())){
                tm.setQty(tm.getQty()+Integer.parseInt(txtQty.getText()));
                tm.setAmount(tm.getQty()*tm.getAmount());
                isExist = true;
            }
        }

        if (!isExist){
            JFXButton btn = new JFXButton("Delete");
            btn.setTextFill(Color.rgb(255,255,255));
            btn.setStyle("-fx-background-color: #e35c5c; -fx-font-weight: BOLD");
            String unitPrice = lblUnitPrice.getText().replace(",",".");
            CartTm cartTm = new CartTm(
                    cmbItemCode.getValue().toString(),
                    txtDescription.getText(),
                    Double.parseDouble(unitPrice),
                    Integer.parseInt(txtQty.getText()),
                    Double.parseDouble(unitPrice)*Integer.parseInt(txtQty.getText()),
                    btn
            );

            btn.setOnAction(actionEvent -> {
                tmList.remove(cartTm);
                lblTotal.setText(String.format("%.2f",findTotal()));
                tblOrder.refresh();
            });

            tmList.add(cartTm);

            TreeItem<CartTm> treeItem = new RecursiveTreeItem<>(tmList, RecursiveTreeObject::getChildren);
            tblOrder.setRoot(treeItem);
            tblOrder.setShowRoot(false);
        }
        lblTotal.setText(String.format("%.2f",findTotal()));
        tblOrder.refresh();
    }

    @FXML
    void clearButtonOnAction(ActionEvent event) {
        clearFields();
    }

    private void clearFields() {
        cmbCustomerId.setValue("");
        cmbItemCode.setValue("");
        txtCustomerName.clear();
        txtSearch.clear();
        txtDescription.clear();
        lblUnitPrice.setText("0.00");
        lblQtyOnHand.setText("");
        txtQty.clear();

    }

    @FXML
    void placeOrderButtonOnAction(ActionEvent event) throws SQLException {
        List<OrderDetails> detailsList = new ArrayList<>();

        for (CartTm tm:tmList) {
            detailsList.add(new OrderDetails(
                    lblOrderId.getText(),
                    tm.getCode(),
                    tm.getQty(),
                    tm.getUnitPrice()
            ));
        }

        Order order = new Order(
                lblOrderId.getText(),
                LocalDate.now().toString(),
                cmbCustomerId.getValue().toString()
        );
        Connection connection = null;
        boolean isOrderPlaced = true;
        try {
            connection = DBConnection.getInstance().getConnection();
            connection.setAutoCommit(false);

            String sql = "INSERT INTO orders VALUES(?,?,?)";
            PreparedStatement pstm = connection.prepareStatement(sql);
            pstm.setString(1,order.getId());
            pstm.setDate(2, Date.valueOf(order.getDate()));
            pstm.setString(3,order.getCustomerId());

            if (pstm.executeUpdate()>0) {
                for (OrderDetails detail:detailsList) {
                    String query = "INSERT INTO orderdetail VALUES(?,?,?,?)";
                    PreparedStatement ptm = connection.prepareStatement(query);
                    ptm.setString(1,detail.getOrderId());
                    ptm.setString(2, detail.getItemCode());
                    ptm.setInt(3,detail.getQty());
                    ptm.setDouble(4,detail.getUnitPrice());

                    if (ptm.executeUpdate()<=0){
                        isOrderPlaced = false;
                    }

                }
            }else{
                isOrderPlaced = false;
                new Alert(Alert.AlertType.ERROR,"Something went wrong..!").show();
                connection.rollback();
            }

            if (isOrderPlaced){
                new Alert(Alert.AlertType.INFORMATION,"Order Placed..!").show();
                connection.commit();
                tmList.clear();
                tblOrder.refresh();
                clearFields();
            }else{
                new Alert(Alert.AlertType.ERROR,"Something went wrong..!").show();
                connection.rollback();
            }
        } catch (SQLException | ClassNotFoundException e) {
            connection.rollback();
            e.printStackTrace();
        } finally {
            connection.setAutoCommit(true);
        }
    }


    @FXML
    void updateButtonOnAction(ActionEvent event) {

    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        colItemCode.setCellValueFactory(new TreeItemPropertyValueFactory<>("code"));
        colDescription.setCellValueFactory(new TreeItemPropertyValueFactory<>("desc"));
        colUnitPrice.setCellValueFactory(new TreeItemPropertyValueFactory<>("unitPrice"));
        colQty.setCellValueFactory(new TreeItemPropertyValueFactory<>("qty"));
        colAmount.setCellValueFactory(new TreeItemPropertyValueFactory<>("amount"));
        colOption.setCellValueFactory(new TreeItemPropertyValueFactory<>("btn"));

        generateId();
        loadCustomerId();
        loadItemCodes();

        cmbCustomerId.setOnAction(actionEvent -> {
            setCustomerName();
        });

        cmbItemCode.setOnAction(actionEvent -> {
            setItemDetails();
        });
    }

    private void setItemDetails() {
        try {
            Connection connection = DBConnection.getInstance().getConnection();
            PreparedStatement pstm = connection.prepareStatement("SELECT * FROM item WHERE code=?");
            pstm.setString(1,cmbItemCode.getValue().toString());
            ResultSet resultSet = pstm.executeQuery();

            if (resultSet.next()){
                txtDescription.setText(resultSet.getString(2));
                lblUnitPrice.setText(String.format("%.2f",resultSet.getDouble(3)));
                lblQtyOnHand.setText(resultSet.getString(4));
            }

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void setCustomerName() {
        try {
            Connection connection = DBConnection.getInstance().getConnection();
            PreparedStatement pstm = connection.prepareStatement("SELECT name FROM customer WHERE id=?");
            pstm.setString(1,cmbCustomerId.getValue().toString());
            ResultSet resultSet = pstm.executeQuery();

            if (resultSet.next()){
                txtCustomerName.setText(resultSet.getString(1));
            }

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void loadItemCodes() {
        try {
            Connection connection = DBConnection.getInstance().getConnection();
            PreparedStatement pstm = connection.prepareStatement("SELECT code FROM item");
            ResultSet resultSet = pstm.executeQuery();

            ObservableList<String> itemCodes = FXCollections.observableArrayList();

            while (resultSet.next()){
                itemCodes.add(resultSet.getString(1));
            }

            cmbItemCode.setItems(itemCodes);
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void loadCustomerId() {

        try {
            Connection connection = DBConnection.getInstance().getConnection();
            PreparedStatement pstm = connection.prepareStatement("SELECT id FROM customer");
            ResultSet resultSet = pstm.executeQuery();

            ObservableList<String> customerIds = FXCollections.observableArrayList();

            while (resultSet.next()){
                customerIds.add(resultSet.getString(1));
            }

            cmbCustomerId.setItems(customerIds);
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    private void generateId() {
        try {
            Connection connection = DBConnection.getInstance().getConnection();
            PreparedStatement pstm = connection.prepareStatement("SELECT id FROM orders ORDER BY id DESC LIMIT 1");
            ResultSet resultSet = pstm.executeQuery();
            if (resultSet.next()){
                int num = Integer.parseInt(resultSet.getString(1).split("[D]")[1]);
                num++;
                lblOrderId.setText(String.format("D%03d",num));
            }else {
                lblOrderId.setText("D001");
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
