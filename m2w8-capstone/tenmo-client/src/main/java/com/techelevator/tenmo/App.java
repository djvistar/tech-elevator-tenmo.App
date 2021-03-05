package com.techelevator.tenmo;

import java.security.Principal;
import java.util.Scanner;

import com.techelevator.tenmo.models.Account;
import com.techelevator.tenmo.models.AuthenticatedUser;
import com.techelevator.tenmo.models.Transfer;
import com.techelevator.tenmo.models.TransferRequest;
import com.techelevator.tenmo.models.User;
import com.techelevator.tenmo.models.UserCredentials;
import com.techelevator.tenmo.services.AuthenticationService;
import com.techelevator.tenmo.services.AuthenticationServiceException;
import com.techelevator.tenmo.services.TransferService;
import com.techelevator.view.ConsoleService;

public class App {

	private static final String API_BASE_URL = "http://localhost:8080/";

	private static final String MENU_OPTION_EXIT = "Exit";
	private static final String LOGIN_MENU_OPTION_REGISTER = "Register";
	private static final String LOGIN_MENU_OPTION_LOGIN = "Login";
	private static final String[] LOGIN_MENU_OPTIONS = { LOGIN_MENU_OPTION_REGISTER, LOGIN_MENU_OPTION_LOGIN,
			MENU_OPTION_EXIT };
	private static final String MAIN_MENU_OPTION_VIEW_BALANCE = "View your current balance";
	private static final String MAIN_MENU_OPTION_SEND_BUCKS = "Send TE bucks";
	private static final String MAIN_MENU_OPTION_VIEW_PAST_TRANSFERS = "View your past transfers";
	private static final String MAIN_MENU_OPTION_REQUEST_BUCKS = "Request TE bucks";
	private static final String MAIN_MENU_OPTION_VIEW_PENDING_REQUESTS = "View your pending requests";
	private static final String MAIN_MENU_OPTION_LOGIN = "Login as different user";
	private static final String[] MAIN_MENU_OPTIONS = { MAIN_MENU_OPTION_VIEW_BALANCE, MAIN_MENU_OPTION_SEND_BUCKS,
			MAIN_MENU_OPTION_VIEW_PAST_TRANSFERS, MAIN_MENU_OPTION_REQUEST_BUCKS,
			MAIN_MENU_OPTION_VIEW_PENDING_REQUESTS, MAIN_MENU_OPTION_LOGIN, MENU_OPTION_EXIT };

	private AuthenticatedUser currentUser;
	private ConsoleService console;
	private AuthenticationService authenticationService;
	private TransferService transferService;

	public static void main(String[] args) {
		App app = new App(new ConsoleService(System.in, System.out), new AuthenticationService(API_BASE_URL),
				new TransferService(API_BASE_URL));
		app.run();
	}

	public App(ConsoleService console, AuthenticationService authenticationService, TransferService transferService) {
		this.console = console;
		this.authenticationService = authenticationService;
		this.transferService = transferService;
	}

	public void run() {
		System.out.println("*********************");
		System.out.println("* Welcome to TEnmo! *");
		System.out.println("*********************");

		registerAndLogin();
		mainMenu();
	}

	private void mainMenu() {
		while (true) {
			String choice = (String) console.getChoiceFromOptions(MAIN_MENU_OPTIONS);
			if (MAIN_MENU_OPTION_VIEW_BALANCE.equals(choice)) {
				viewCurrentBalance();
			} else if (MAIN_MENU_OPTION_VIEW_PAST_TRANSFERS.equals(choice)) {
				viewTransferHistory();
			} else if (MAIN_MENU_OPTION_VIEW_PENDING_REQUESTS.equals(choice)) {
				viewPendingRequests();
			} else if (MAIN_MENU_OPTION_SEND_BUCKS.equals(choice)) {
				sendBucks();
			} else if (MAIN_MENU_OPTION_REQUEST_BUCKS.equals(choice)) {
				requestBucks();
			} else if (MAIN_MENU_OPTION_LOGIN.equals(choice)) {
				login();
			} else {
				// the only other option on the main menu is to exit
				exitProgram();
			}
		}
	}

	private void viewCurrentBalance() {

		// TODO Auto-generated method stub
		// Account balance = transferService.viewCurrentBalance();
		// TransferService transferService = new TransferService(API_BASE_URL,
		// currentUser);
		transferService.setAUTH_TOKEN(currentUser.getToken());
		Double balance = transferService.viewCurrentBalance();
		System.out.println("Your current Account balance is: $" + balance);

		// System.out.println("Your current Account balance is: $" + balance);

	}

	private void viewTransferHistory() {
		// TODO Auto-generated method stub

		transferService.setAUTH_TOKEN(currentUser.getToken());
		Transfer[] allTransfers = transferService.viewTransferHistory();
		printAllTransfers(allTransfers);

		Scanner scanner = new Scanner(System.in);
		String input = scanner.nextLine();
		int transferId = Integer.parseInt(input);
		if (transferId != 0) {
			Transfer transferDetails = transferService.viewTransferDetails(transferId);
			printTransferDetails(transferDetails);
		}

	}

	private void viewPendingRequests() {
		// TODO Auto-generated method stub

	}

	private void sendBucks() {
		// TODO Auto-generated method stub
		transferService.setAUTH_TOKEN(currentUser.getToken());

		User[] users = transferService.listOfUsers();
		printAllUsers(users);
		
		
		System.out.print("Enter ID of user you are sending to (0 to cancel): ");
		Scanner scanner = new Scanner(System.in);
		String inputUserId = scanner.nextLine();
		int userId = Integer.parseInt(inputUserId);
		
		System.out.print("Enter amount: ");
		String inputAmount = scanner.nextLine();
		double amount = Double.parseDouble(inputAmount);
		
		TransferRequest transferRequest = new TransferRequest();
		transferRequest.setReceiverId(userId);
		transferRequest.setAmount(amount);
		
		transferService.sendBucks(transferRequest);
		
	

	}

	private void requestBucks() {
		// TODO Auto-generated method stub

	}

	private void exitProgram() {
		System.exit(0);
	}

	private void registerAndLogin() {
		while (!isAuthenticated()) {
			String choice = (String) console.getChoiceFromOptions(LOGIN_MENU_OPTIONS);
			if (LOGIN_MENU_OPTION_LOGIN.equals(choice)) {
				login();
			} else if (LOGIN_MENU_OPTION_REGISTER.equals(choice)) {
				register();
			} else {
				// the only other option on the login menu is to exit
				exitProgram();
			}
		}
	}

	private boolean isAuthenticated() {
		return currentUser != null;
	}

	private void register() {
		System.out.println("Please register a new user account");
		boolean isRegistered = false;
		while (!isRegistered) // will keep looping until user is registered
		{
			UserCredentials credentials = collectUserCredentials();
			try {
				authenticationService.register(credentials);
				isRegistered = true;
				System.out.println("Registration successful. You can now login.");
			} catch (AuthenticationServiceException e) {
				System.out.println("REGISTRATION ERROR: " + e.getMessage());
				System.out.println("Please attempt to register again.");
			}
		}
	}

	private void login() {
		System.out.println("Please log in");
		currentUser = null;
		while (currentUser == null) // will keep looping until user is logged in
		{
			UserCredentials credentials = collectUserCredentials();
			try {
				currentUser = authenticationService.login(credentials);
			} catch (AuthenticationServiceException e) {
				System.out.println("LOGIN ERROR: " + e.getMessage());
				System.out.println("Please attempt to login again.");
			}
		}
	}

	private UserCredentials collectUserCredentials() {
		String username = console.getUserInput("Username");
		String password = console.getUserInput("Password");
		return new UserCredentials(username, password);
	}

	private void printAllTransfers(Transfer[] transfers) {

		System.out.print(String.format("%-15s%-40s%10s\n\n", "Transfer ID", "To/From", "Amount"));

		for (Transfer transfer : transfers) {
			printTransfer(transfer);
		}
		System.out.print("Please enter transfer ID to view details (0 to cancel): ");

	}

	private void printTransfer(Transfer transfer) {

		if (transfer != null) {
			if (!transfer.getUserFrom().equalsIgnoreCase(currentUser.getUser().getUsername())) {
				System.out.print(String.format("%-15s%-40s$%10s\n", transfer.getTransferId(),
						"From: " + transfer.getUserFrom(), transfer.getAmount()));
			} else {
				System.out.print(String.format("%-15s%-40s$%10s\n", transfer.getTransferId(),
						"To: " + transfer.getUserTo(), transfer.getAmount()));
			}
		}

	}

	private void printTransferDetails(Transfer transfer) {
		if (transfer != null) {

			System.out.println("--------------------------------------------\r\n" + "Transfer Details\r\n"
					+ "--------------------------------------------\r\n" + " Id: " + transfer.getTransferId() + "\r\n"
					+ " From: " + transfer.getUserFrom() + "\r\n" + " To: " + transfer.getUserTo() + "\r\n" + " Type: "
					+ transfer.getTransferType() + "\r\n" + " Status: " + transfer.getTransferStatus() + "\r\n"
					+ " Amount: $" + transfer.getAmount());

		}

	}

	private void printAllUsers(User[] users) {

		System.out.print(String.format("%-15s%10s\n\n", "User Id", "Name"));

		for (User user : users) {
			if(!user.getUsername().equalsIgnoreCase(currentUser.getUser().getUsername()))
			printUser(user);
		}
		
		
	}

	private void printUser(User user) {

		System.out.print(String.format("%-15s%10s\n", user.getId(), user.getUsername()));
		;
	}

}
