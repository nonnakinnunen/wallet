package wallet;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import wallet.exception.AccountNotFoundException;

public class HandlerForBalance implements RequestHandler<HandlerForBalance.BalanceRequest, HandlerForBalance.BalanceResponse> {

  private static final String ACCOUNTS_TABLE_NAME = "Accounts";
  private static final String ATTRIBUTE_ACCOUNT_ID = "AccountId";
  private static final String ATTRIBUTE_CURRENT_BALANCE = "CurrentBalance";
  @Override
  /*
   * Takes in an BalanceRequest and saves it to DynamoDB table
   */
  public BalanceResponse handleRequest(BalanceRequest event, Context context) {

    LambdaLogger logger = context.getLogger();

    AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    DynamoDB dynamoDB = new DynamoDB(client);

    Table table = dynamoDB.getTable(ACCOUNTS_TABLE_NAME);


    try{
      Item item = table.getItem(ATTRIBUTE_ACCOUNT_ID, event.accountId());
      if(item != null){
        return new BalanceResponse(item.getInt(ATTRIBUTE_CURRENT_BALANCE));
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

  public static record BalanceRequest(Integer accountId) {
  }

  public static record BalanceResponse(Integer balance) {
  }

}

