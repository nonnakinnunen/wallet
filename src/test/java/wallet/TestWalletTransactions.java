package wallet;

import java.net.URI;
import java.net.URISyntaxException;
import com.amazonaws.http.HttpMethodName;
import java.io.ByteArrayInputStream;

public class TestWalletTransactions {
    static final String AWS_IAM_ACCESS_KEY = "your-access-key"; //fetch access key from AWS console
    static final String AWS_IAM_SECRET_ACCESS_KEY = "your-secret-access-key"; //fetch from AWS console
    static final String AWS_REGION = "eu-north-1"; //your region
    static final String AWS_API_GATEWAY_ENPOINT = "API_GATEWAY_URL"; //for example https://m9ujtzisdd.execute-api.eu-north-1.amazonaws.com/development/player

    static final String betJsonRequest = "{\n" +
            "  \"accountId\": 1237897,\n" +
            "  \"transactionId\": 645,\n" +
            "  \"betAmount\": 6\n" +
            "}";

    static final String winJsonRequest = "{\n" +
            "  \"accountId\": 1237897,\n" +
            "  \"transactionId\": 646,\n" +
            "  \"winAmount\": 10\n" +
            "}";

    public static void main(String... args) {
        try {
            JsonApiGatewayCaller caller = new JsonApiGatewayCaller(
                    AWS_IAM_ACCESS_KEY,
                    AWS_IAM_SECRET_ACCESS_KEY,
                    AWS_REGION,
                    new URI(AWS_API_GATEWAY_ENPOINT)
            );

            ApiGatewayResponse responseBalance = caller.execute(HttpMethodName.GET, "/1237897", null);

            System.out.println("BALANCE RESPONSE: "+responseBalance.getBody());

            ApiGatewayResponse response = caller.execute(HttpMethodName.POST, "/bet", new ByteArrayInputStream(betJsonRequest.getBytes()));

            System.out.println("BET RESPONSE: "+response.getBody());


            ApiGatewayResponse responseWin = caller.execute(HttpMethodName.POST, "/win", new ByteArrayInputStream(winJsonRequest.getBytes()));

            System.out.println("WIN RESPONSE: "+responseWin.getBody());


        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}