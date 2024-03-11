package wallet;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import wallet.exception.AccountNotFoundException;
import wallet.exception.InsufficientFundsException;

import java.time.LocalDateTime;
import java.util.*;

public class HandlerForBet implements RequestHandler<HandlerForBet.BetRequest, HandlerForBet.BetResponse> {


  private static final String ACCOUNTS_TABLE_NAME = "Accounts";
  private static final String TRANSACTIONS_TABLE_NAME = "Transactions";
  private static final String ATTRIBUTE_ACCOUNT_ID = "AccountId";
  private static final String ATTRIBUTE_CURRENT_BALANCE = "CurrentBalance";
  private static final String ATTRIBUTE_VERSION = "Version";
  private static final String ATTRIBUTE_TRANSACTION_ID = "TransactionId";
  private static final String ATTRIBUTE_TRANSACTION_TYPE = "TransactionType";
  private static final String ATTRIBUTE_TIMESTAMP = "Timestamp";
  private static final String ATTRIBUTE_AMOUNT = "Amount";

  @Override
  /*
   * Takes in an BetRequest and saves it to DynamoDB table
   */
  public HandlerForBet.BetResponse handleRequest(HandlerForBet.BetRequest event, Context context) {

    LambdaLogger logger = context.getLogger();

    AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    DynamoDB dynamoDB = new DynamoDB(client);

    Table tableAccounts = dynamoDB.getTable(ACCOUNTS_TABLE_NAME);
    try{
      Item account = tableAccounts.getItem(ATTRIBUTE_ACCOUNT_ID, event.accountId());
      if(account != null){

        int currentBalance = account.getInt(ATTRIBUTE_CURRENT_BALANCE);
        int newBalance = currentBalance - event.betAmount();

        if(newBalance < 0){
          throw new InsufficientFundsException("Not enough money on account");
        }

        List<TransactWriteItem> transactionItems = new ArrayList<>();

        // Generate transaction item to be saved to database
        // with condition for idempotency check.
        // If there is already row in database with same transactionId ->
        // insert will fail with ConditionalCheckFailed exception

        Map<String, AttributeValue> transactionItem = new HashMap<>();
        transactionItem.put(ATTRIBUTE_TRANSACTION_ID, new AttributeValue().withN(event.transactionId().toString()));
        transactionItem.put(ATTRIBUTE_ACCOUNT_ID, new AttributeValue().withN(event.accountId().toString()));
        transactionItem.put(ATTRIBUTE_TRANSACTION_TYPE, new AttributeValue().withS("BET"));
        transactionItem.put(ATTRIBUTE_TIMESTAMP, new AttributeValue().withS(LocalDateTime.now().toString()));
        transactionItem.put(ATTRIBUTE_AMOUNT, new AttributeValue().withN(event.betAmount().toString()));

        Put transactionPut = new Put().withTableName(TRANSACTIONS_TABLE_NAME)
                .withItem(transactionItem)
                .withReturnValuesOnConditionCheckFailure(ReturnValuesOnConditionCheckFailure.ALL_OLD)
                .withConditionExpression("attribute_not_exists("+ATTRIBUTE_TRANSACTION_ID+")");

        TransactWriteItem transactionWriteItem = new TransactWriteItem().withPut(transactionPut);

        transactionItems.add(transactionWriteItem);

        int balanceRowVersion = account.getInt(ATTRIBUTE_VERSION);
        int nextVersion = balanceRowVersion + 1;


        // Update balance in accounts table with check for account row version
        // In case account row version is different from the one which was fetched before
        // (someone has updated balance in the meanwhile)
        // update will fail with ConditionalCheckFailed exception
        // also previous insert into transactions table will be rolled back

        HashMap<String, AttributeValue> accountIdItemKey = new HashMap<>();
        accountIdItemKey.put(ATTRIBUTE_ACCOUNT_ID, new AttributeValue().withN(event.accountId().toString()));

        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":newBalance", new AttributeValue().withN(Integer.toString(newBalance)));
        expressionAttributeValues.put(":timestamp",new AttributeValue(LocalDateTime.now().toString()));
        expressionAttributeValues.put(":nextVersion",new AttributeValue().withN(Integer.toString(nextVersion)));
        expressionAttributeValues.put(":currentVersion",new AttributeValue().withN(Integer.toString(balanceRowVersion)));

        Update updateAccount = new Update()
                .withTableName(ACCOUNTS_TABLE_NAME)
                .withUpdateExpression("SET CurrentBalance = :newBalance, BalanceUpdateTimestamp = :timestamp, Version = :nextVersion")
                .withKey(accountIdItemKey)
                .withExpressionAttributeValues(expressionAttributeValues)
                .withConditionExpression("Version = :currentVersion");

        TransactWriteItem accountWriteItem = new TransactWriteItem().withUpdate(updateAccount);

        transactionItems.add(accountWriteItem);
        TransactWriteItemsRequest placeWinTransaction = new TransactWriteItemsRequest()
                .withTransactItems(transactionItems);
      try{
        client.transactWriteItems(placeWinTransaction);
        return new BetResponse(newBalance);

      } catch(TransactionCanceledException e){

        // In case of idempotency check failure we will get TransactionCancelledException
        // We need to check the reason for failure and return old balance

        List<CancellationReason> reasons = e.getCancellationReasons();
        Optional<CancellationReason> idempotencyError = reasons.stream().filter(reason -> reason.getItem() != null && reason.getCode().equals("ConditionalCheckFailed")).findAny();
        if(idempotencyError.isPresent()){
          return new BetResponse(currentBalance);
        }
        throw e;
      }

      } else {
        throw new AccountNotFoundException("Account not found");
      }
    } catch(AccountNotFoundException e){
      logger.log("Account not found: " + e);
      throw e;
    } catch(Exception e){
      logger.log("Exception: " + e);
      throw e;
    }
  }

  public static record BetRequest(Integer accountId, Integer transactionId, Integer betAmount) {
  }
  public static record BetResponse(Integer balance) {
  }
  public static record ResponseError(String message) {
  }
}

