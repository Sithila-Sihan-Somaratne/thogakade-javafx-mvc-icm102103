package controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTreeTableView;
import com.jfoenix.controls.RecursiveTreeItem;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import db.DBConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import model.Order;
import model.OrderDetails;
import model.tm.OrderDetailsTm;
import model.tm.OrderTm;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OrderDetailsFormController {

    public JFXTreeTableView<OrderTm> tblOrder;
    public TreeTableColumn colOrderId;
    public TreeTableColumn colDate;
    public TreeTableColumn colCustName;
    public TreeTableColumn colOption;
    public AnchorPane orderDetailsPane;

    public JFXTreeTableView<OrderDetailsTm> tblDetails;
    public TreeTableColumn colItemCode;
    public TreeTableColumn colDesc;
    public TreeTableColumn colQty;
    public TreeTableColumn colAmount;


    public void backButtonOnAction(ActionEvent actionEvent) {
        Stage stage = (Stage) orderDetailsPane.getScene().getWindow();
        try {
            stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("../view/DashBoard.fxml"))));
        } catch (IOException e) {
            e.printStackTrace();
        }
        stage.show();
    }

    public void initialize(){
        colOrderId.setCellValueFactory(new TreeItemPropertyValueFactory<>("id"));
        colDate.setCellValueFactory(new TreeItemPropertyValueFactory<>("date"));
        colCustName.setCellValueFactory(new TreeItemPropertyValueFactory<>("custName"));
        colOption.setCellValueFactory(new TreeItemPropertyValueFactory<>("btn"));

        colItemCode.setCellValueFactory(new TreeItemPropertyValueFactory<>("itemCode"));
        colDesc.setCellValueFactory(new TreeItemPropertyValueFactory<>("desc"));
        colQty.setCellValueFactory(new TreeItemPropertyValueFactory<>("qty"));
        colAmount.setCellValueFactory(new TreeItemPropertyValueFactory<>("amount"));

        loadOrders();

        tblOrder.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) ->{
            if (newValue!=null){
                loadDetails(newValue);
            }
        });
    }

    private void loadDetails(TreeItem<OrderTm> newValue) {
        ObservableList<OrderDetailsTm> tmList = FXCollections.observableArrayList();
        try {
            List<OrderDetails> list = new ArrayList<>();
            Connection connection = DBConnection.getInstance().getConnection();
            PreparedStatement pstm = connection.prepareStatement("SELECT * FROM orderdetail WHERE orderId=?");
            pstm.setString(1,newValue.getValue().getId());
            ResultSet resultSet = pstm.executeQuery();

            while (resultSet.next()) {

                list.add(new OrderDetails(
                        resultSet.getString(1),
                        resultSet.getString(2),
                        resultSet.getInt(3),
                        resultSet.getDouble(4)
                ));
            }

            for (OrderDetails detail:list) {
                pstm = connection.prepareStatement("SELECT description FROM item WHERE code=?");
                pstm.setString(1,detail.getItemCode());
                ResultSet rsSet = pstm.executeQuery();

                rsSet.next();

                tmList.add(new OrderDetailsTm(
                        detail.getItemCode(),
                        rsSet.getString(1),
                        detail.getQty(),
                        detail.getUnitPrice()*detail.getQty()
                ));
            }

            TreeItem<OrderDetailsTm> treeItem = new RecursiveTreeItem<>(tmList, RecursiveTreeObject::getChildren);
            tblDetails.setRoot(treeItem);
            tblDetails.setShowRoot(false);

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void loadOrders() {
        ObservableList<OrderTm> tmList = FXCollections.observableArrayList();
        try {
            List<Order> list = new ArrayList<>();
            Connection connection = DBConnection.getInstance().getConnection();
            PreparedStatement pstm = connection.prepareStatement("SELECT * FROM orders");
            ResultSet resultSet = pstm.executeQuery();

            while (resultSet.next()) {
                list.add(new Order(
                        resultSet.getString(1),
                        resultSet.getString(2),
                        resultSet.getString(3)
                ));
            }

            for (Order order:list) {
                JFXButton btn = new JFXButton("Delete");
                btn.setTextFill(Color.rgb(255,255,255));
                btn.setStyle("-fx-background-color: #e35c5c; -fx-font-weight: BOLD");


                btn.setOnAction(actionEvent -> {
                    try {
                        PreparedStatement pst = connection.prepareStatement("DELETE FROM orders WHERE id=?");
                        pst.setString(1, order.getId());
                        Optional<ButtonType> buttonType = new Alert(Alert.AlertType.CONFIRMATION, "Do you want to delete " + order.getId() + " order ? ", ButtonType.YES, ButtonType.NO).showAndWait();
                        if (buttonType.get() == ButtonType.YES){
                            if (pst.executeUpdate()>0){
                                new Alert(Alert.AlertType.INFORMATION,"Order Deleted..!").show();
                                loadOrders();
                            }else{
                                new Alert(Alert.AlertType.ERROR,"Something went wrong..!").show();
                            }
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });

                PreparedStatement pt = connection.prepareStatement("SELECT name FROM customer WHERE id=?");
                pt.setString(1,order.getCustomerId());
                ResultSet rst = pt.executeQuery();
                rst.next();

                tmList.add(new OrderTm(
                        order.getId(),
                        order.getDate(),
                        rst.getString(1),
                        btn
                ));
            }

            TreeItem<OrderTm> treeItem = new RecursiveTreeItem<>(tmList, RecursiveTreeObject::getChildren);
            tblOrder.setRoot(treeItem);
            tblOrder.setShowRoot(false);

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}