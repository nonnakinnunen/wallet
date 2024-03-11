package wallet.testing;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Random;

public class HandlerForTestingWallet implements RequestHandler<HandlerForTestingWallet.AccountRecord, Integer> {

  private static final String ACCOUNTS_TABLE_NAME = "Accounts";
  private static final String ATTRIBUTE_ACCOUNT_ID = "AccountId";
  private static final String ATTRIBUTE_NAME = "Name";
  private static final String ATTRIBUTE_INITIAL_BALANCE = "InitialBalance";
  private static final String ATTRIBUTE_CURRENT_BALANCE = "CurrentBalance";
  private static final String ATTRIBUTE_BALANCE_TIMESTAMP = "BalanceUpdateTimestamp";
  private static final String ATTRIBUTE_VERSION = "Version";

  @Override
  /*
   * Takes in an AccountRecord and saves it to DynamoDB table
   */
  public Integer handleRequest(HandlerForTestingWallet.AccountRecord event, Context context) {

    LambdaLogger logger = context.getLogger();

    AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    DynamoDB dynamoDB = new DynamoDB(client);

    Table table = dynamoDB.getTable(ACCOUNTS_TABLE_NAME);
    Item item = new Item()
            .withPrimaryKey(ATTRIBUTE_ACCOUNT_ID, event.accountId())
            .withString(ATTRIBUTE_NAME, event.name())
            .withDouble(ATTRIBUTE_INITIAL_BALANCE, event.balance())
            .withDouble(ATTRIBUTE_CURRENT_BALANCE, event.balance())
            .withInt(ATTRIBUTE_VERSION, 0)
            .withString(ATTRIBUTE_BALANCE_TIMESTAMP, LocalDateTime.now().toString());

    try{
      // Write item to the table
      table.putItem(item);
    }catch(Exception e){
      logger.log("Exception: " + e);
    }
    return null;
  }

  public static record AccountRecord(Integer accountId, String name, Double balance) {
  }

}

