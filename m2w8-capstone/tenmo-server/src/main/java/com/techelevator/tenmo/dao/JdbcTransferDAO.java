package com.techelevator.tenmo.dao;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

import com.techelevator.tenmo.model.Account;
import com.techelevator.tenmo.model.Transfer;
import com.techelevator.tenmo.model.TransferRequest;
import com.techelevator.tenmo.model.User;

@Component
public class JdbcTransferDAO implements TransferDAO {

	@Autowired
	private AccountDAO accountDAO;

	private JdbcTemplate jdbcTemplate;

	public JdbcTransferDAO(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	public List<Transfer> listOfAllTransfers() {
		// TODO Auto-generated method stub

		List<Transfer> allTransfers = new ArrayList<Transfer>();

		// Selects all information from transfers table, as well as usernames of the people sending/receiving the transfer
		String sql = "SELECT t.*, u.username AS userFrom, v.username AS userTo " + "FROM transfers t "
				+ "JOIN accounts a ON t.account_from = a.account_id "
				+ "JOIN accounts b ON t.account_to = b.account_id " + "JOIN users u ON a.user_id = u.user_id "
				+ "JOIN users v ON b.user_id = v.user_id ";

		SqlRowSet results = jdbcTemplate.queryForRowSet(sql);

		while (results.next()) { // Loops through the list of all results and adds them to a list
			Transfer singleTransfer = mapToTransfer(results);
			allTransfers.add(singleTransfer);
		}

		return allTransfers;
	}

	public Transfer listTransferDetails(int transferId) {
		// Selects all transfer details, usernames of parties involved, and status and type descriptions for specific transfer id
		String sql = "SELECT t.*, u.username AS userFrom, v.username AS userTo, ts.transfer_status_desc, tt.transfer_type_desc FROM transfers t "
				+ "JOIN accounts a ON t.account_from = a.account_id "
				+ "JOIN accounts b ON t.account_to = b.account_id " + "JOIN users u ON a.user_id = u.user_id "
				+ "JOIN users v ON b.user_id = v.user_id "
				+ "JOIN transfer_statuses ts ON t.transfer_status_id = ts.transfer_status_id "
				+ "JOIN transfer_types tt ON t.transfer_type_id = tt.transfer_type_id " + "WHERE t.transfer_id = ?";

		SqlRowSet result = jdbcTemplate.queryForRowSet(sql, transferId);

		result.next(); // Assigns this information to a Transfer Object to be printed upon selection
		Transfer singleTransfer = mapToTransfer(result);

		return singleTransfer;

	}

	@Override
	public String sendTransfer(TransferRequest transferRequest, int senderId) {
		// TODO Auto-generated method stub
		
		NumberFormat formatter = NumberFormat.getCurrencyInstance();
		
		try {
			// Retrieves the details for the sender where the user Id is gathered from the Principal of the individual logged in
			String sqlSelectSender = "SELECT account_id, balance, user_id FROM accounts " +
							         "WHERE user_id = ?;";
			SqlRowSet usernameResultSender = jdbcTemplate.queryForRowSet(sqlSelectSender, senderId);
			usernameResultSender.next();
			TransferRequest fromAccount = mapToRequest(usernameResultSender); // Assigns this information to the sender to be called later
			// Retrieves the details for the reciever based on the UserId and the Amount submitted by the user
			String sqlSelectReceiver = "SELECT account_id, balance, user_id FROM accounts " +
					   				   "WHERE user_id = ?;";
			SqlRowSet usernameResultReceiver = jdbcTemplate.queryForRowSet(sqlSelectReceiver, transferRequest.getReceiverId());
			usernameResultReceiver.next();
			TransferRequest toAccount = mapToRequest(usernameResultReceiver); // Assigns this to the receiver to be called later

			// If the sender of funds is equal to the receiver of funds, cannot complete transaction
			if (fromAccount.getDestinationId() == transferRequest.getDestinationId()) {
				return "You can not send money to your self.";
			} 
			// If the sender tries sending more than they have available, cannot complete transaction
			else if (fromAccount.getAmount() < transferRequest.getAmount() || fromAccount.getAmount() < 0) {
				return "Insufficient Funds";
			} 
			// If the sender tries entering a negative transfer amount, cannot complete transaction
			else if (transferRequest.getAmount() < 0) {
				return "Cannot send negative funds";
			} 
			// Happy case comes after eliminating the possibility of all bad cases
			else {
				String sql = "INSERT INTO transfers(transfer_type_id, transfer_status_id, account_from, account_to, amount) "
						+ "VALUES (2,2,?,?,?) ";
				jdbcTemplate.update(sql, fromAccount.getDestinationId(), toAccount.getDestinationId(),
						transferRequest.getAmount());
				// Updates the sender's account balance based on the amount entered
				double updatedSenderBalance =fromAccount.getAmount() - transferRequest.getAmount();
				// Updates the receiver's account balance in a similar manner
				double updatedReceiverBalance = accountDAO.retrieveBalance(toAccount.getReceiverId()) + transferRequest.getAmount();
				// Sends this information to the database
				String sqlToAccount = "UPDATE accounts SET account_id=?, user_id = ?, balance = ? WHERE account_id = ?;";
				jdbcTemplate.update(sqlToAccount, toAccount.getDestinationId(), toAccount.getReceiverId(), updatedReceiverBalance, toAccount.getDestinationId());

				String sqlFromAccount = "UPDATE accounts SET account_id=?, user_id = ?, balance = ? WHERE account_id = ?;";
				jdbcTemplate.update(sqlFromAccount,fromAccount.getDestinationId(), fromAccount.getReceiverId(), updatedSenderBalance, fromAccount.getDestinationId());
			
			}
		} catch (Exception ex) {
			System.out.println(ex);
		}

		return "Transfer Complete";
		
	}

	private Transfer mapToTransfer(SqlRowSet results) {

		Transfer transfer = new Transfer();

		transfer.setTransferId(results.getInt("transfer_id"));
		transfer.setTransferTypeId(results.getInt("transfer_type_id"));
		transfer.setTransferStatusId(results.getInt("transfer_status_id"));
		transfer.setAccountFrom(results.getInt("account_from"));
		transfer.setAccountTo(results.getInt("account_to"));
		transfer.setAmount(results.getDouble("amount"));

		try {
			transfer.setUserFrom(results.getString("userFrom"));
			transfer.setUserTo(results.getString("userTo"));
		} catch (Exception e) {
		}
		try {
			transfer.setTransferType(results.getString("transfer_type_desc"));
			transfer.setTransferStatus(results.getString("transfer_status_desc"));
		} catch (Exception e) {
		}

		return transfer;
	}

	private TransferRequest mapToRequest(SqlRowSet results) {
		TransferRequest request = new TransferRequest();

		request.setDestinationId(results.getInt("account_id"));
		request.setAmount(results.getDouble("balance"));
		request.setReceiverId(results.getInt("user_id"));

		return request;
	}

}
