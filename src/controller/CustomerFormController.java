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
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import model.Customer;
import model.tm.CustomerTm;

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

public class CustomerFormController implements Initializable {
    public Label lblCustId;
    @FXML
    private TreeTableColumn colAddress;

    @FXML
    private TreeTableColumn colCustId;

    @FXML
    private TreeTableColumn colCustName;

    @FXML
    private TreeTableColumn colSalary;

    @FXML
    private TreeTableColumn colOption;

    @FXML
    private AnchorPane customerPane;

    @FXML
    private JFXTreeTableView<CustomerTm> tblCustomer;

    @FXML
    private JFXTextField txtAddress;

    @FXML
    private JFXTextField txtName;

    @FXML
    private JFXTextField txtSalary;

    @FXML
    private JFXTextField txtSearch;

    public void backButtonOnAction(ActionEvent actionEvent) {
        Stage stage = (Stage) customerPane.getScene().getWindow();
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
        txtName.clear();
        txtAddress.clear();
        txtSalary.clear();
        txtSearch.clear();
        tblCustomer.refresh();
    }

    @FXML
    void saveButtonOnAction(ActionEvent event) {
        Customer customer = new Customer(
                lblCustId.getText(),
                txtName.getText(),
                txtAddress.getText(),
                Double.parseDouble(txtSalary.getText())
        );
        try {
            Connection connection = DBConnection.getInstance().getConnection();
            String sql = "INSERT INTO customer VALUES(?,?,?,?)";
            PreparedStatement pstm = connection.prepareStatement(sql);
            pstm.setString(1,customer.getId());
            pstm.setString(2,customer.getName());
            pstm.setString(3,customer.getAddress());
            pstm.setDouble(4,customer.getSalary());

            if (pstm.executeUpdate()>0) {
                new Alert(Alert.AlertType.INFORMATION,"Customer Saved..!").show();
                loadTable();
                clearFields();
            }else{
                new Alert(Alert.AlertType.ERROR,"Something went wrong..!").show();
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void updateButtonOnAction(ActionEvent event) {
        Customer customer = new Customer(
                lblCustId.getText(),
                txtName.getText(),
                txtAddress.getText(),
                Double.parseDouble(txtSalary.getText())
        );
        try {
            Connection connection = DBConnection.getInstance().getConnection();
            PreparedStatement pstm = connection.prepareStatement("UPDATE customer SET name=? , address=?, salary=? WHERE id=?");
            pstm.setString(1,customer.getName());
            pstm.setString(2,customer.getAddress());
            pstm.setDouble(3,customer.getSalary());
            pstm.setString(4,customer.getId());

            if (pstm.executeUpdate()>0){
                new Alert(Alert.AlertType.INFORMATION,"Customer Updated..!").show();
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
        colCustId.setCellValueFactory(new TreeItemPropertyValueFactory<>("id"));
        colCustName.setCellValueFactory(new TreeItemPropertyValueFactory<>("name"));
        colAddress.setCellValueFactory(new TreeItemPropertyValueFactory<>("address"));
        colSalary.setCellValueFactory(new TreeItemPropertyValueFactory<>("salary"));
        colOption.setCellValueFactory(new TreeItemPropertyValueFactory<>("btn"));
        generateId();
        loadTable();

        tblCustomer.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) ->{
            if (newValue!=null){
                setData(newValue);
            }
        });

        txtSearch.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {
                tblCustomer.setPredicate(new Predicate<TreeItem<CustomerTm>>() {
                    @Override
                    public boolean test(TreeItem<CustomerTm> customerTmTreeItem) {
                        boolean flag = customerTmTreeItem.getValue().getId().contains(newValue) ||
                                customerTmTreeItem.getValue().getName().contains(newValue);
                        return flag;
                    }
                });
            }
        });
    }

    private void setData(TreeItem<CustomerTm> value) {
        lblCustId.setText(value.getValue().getId());
        txtName.setText(value.getValue().getName());
        txtAddress.setText(value.getValue().getAddress());
        txtSalary.setText(String.valueOf(value.getValue().getSalary()));
    }

    private void loadTable() {
        ObservableList<CustomerTm> tmList = FXCollections.observableArrayList();
        try {
            List<Customer> list = new ArrayList<>();
            Connection connection = DBConnection.getInstance().getConnection();
            PreparedStatement pstm = connection.prepareStatement("SELECT * FROM customer");
            ResultSet resultSet = pstm.executeQuery();

            while (resultSet.next()) {
                list.add(new Customer(
                        resultSet.getString(1),
                        resultSet.getString(2),
                        resultSet.getString(3),
                        resultSet.getDouble(4)
                ));
            }

            for (Customer customer:list) {
                JFXButton btn = new JFXButton("Delete");
                btn.setTextFill(Color.rgb(255,255,255));
                btn.setStyle("-fx-background-color: #e35c5c; -fx-font-weight: BOLD");

                btn.setOnAction(actionEvent -> {
                    try {
                        PreparedStatement pst = connection.prepareStatement("DELETE FROM customer WHERE id=?");
                        pst.setString(1,customer.getId());
                        Optional<ButtonType> buttonType = new Alert(Alert.AlertType.CONFIRMATION, "Do you want to delete " + customer.getId() + " customer ? ", ButtonType.YES, ButtonType.NO).showAndWait();
                        if (buttonType.get() == ButtonType.YES){
                            if (pst.executeUpdate()>0){
                                new Alert(Alert.AlertType.INFORMATION,"Customer Deleted..!").show();
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

                tmList.add(new CustomerTm(
                        customer.getId(),
                        customer.getName(),
                        customer.getAddress(),
                        customer.getSalary(),
                        btn
                ));
            }

            TreeItem<CustomerTm> treeItem = new RecursiveTreeItem<>(tmList, RecursiveTreeObject::getChildren);
            tblCustomer.setRoot(treeItem);
            tblCustomer.setShowRoot(false);

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void generateId() {
        try {
            Connection connection = DBConnection.getInstance().getConnection();
            PreparedStatement pstm = connection.prepareStatement("SELECT id FROM customer ORDER BY id DESC LIMIT 1");
            ResultSet resultSet = pstm.executeQuery();
            if (resultSet.next()){
                int num = Integer.parseInt(resultSet.getString(1).split("[C]")[1]);
                num++;
                lblCustId.setText(String.format("C%03d",num));
            }else {
                lblCustId.setText("C001");
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
