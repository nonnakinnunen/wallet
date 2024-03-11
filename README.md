# Pelitili

Projektissa toteutetaan yksinkertainen pelimoottorin ja pelitilin (wallet) välinen integraatio, jonka avulla voi ostaa pelejä ja 
maksaa voittoja. Pelitilillä tarkoitetaan tässä yhteydessä palvelinta, joka tarjoaa HTTP API:n pelimoottoreille ja 
hallinnoi asiakkaiden pelivaroja. Pelimoottoria ei ole tässä toteutettu.

### HTTP API rajapinta

Pelitili tarjoa seuraavan rajapinnan pelimoottorin käytettäväksi.

| Endpoint            | Method | Request body (JSON)                                                                     | Response body (JSON)           | Exceptions                                | 
|---------------------|--------|-----------------------------------------------------------------------------------------|--------------------------------|-------------------------------------------|
| /player/{accountId} | GET    |                                                                                         | { <br/>"balance": "125" <br/>} | Account not found                         |
| /player/bet         | POST   | { <br/>"accountID": 123, <br/> "transactionId": 345678, <br/> "betAmount": "45" <br/> } | { <br/>"balance": "125" <br/>} | Account not found,<br/>Insufficient funds |
| /player/win         | POST   | { <br/>"accountID": 123, <br/> "transactionId": 345676, <br/> "winAmount": "10" <br/> } | { <br/>"balance": "125" <br/>} | Account not found                         |

- AccountId = Pelaajan yksilöivä tunniste
- TransactionId = Osto-/Voittotapahtuman yksilöivä tunniste
- BetAmount = Veloitettava summa sentteissä
- WinAmount = Voittosumma sentteissä
- Balance = Saldo sentteissä

Toteutuksessa on oletettu, että TransactionId toimii kyselyn yksilöivänä tunnisteena.

### Tietokanta pelitilille

Projektissa tietokantana käytetään AWS DynamoDB:ta. Luodaan 2 taulua (Accounts ja Transactions). Accounts-taulussa säilytetään pelaajan tunnistetiedot ja saldo. 
Transactions-taulussa pidetään osto- ja voittotapahtumien tiedot.

#### Accounts taulu

| Sarake                 | Kuvaus                                       |
|------------------------|----------------------------------------------|
| AccountId              | Pelaajan yksilöivä tunniste                  |
| Name                   | Pelaajan nimi                                |
| CurrentBalance         | Pelitilin viimeisin saldo sentteissä         |
| BalanceUpdateTimestamp | Pelitilin saldon viimeisin päivitysaikaleima |
| InitialBalance         | Pelitilin alkusaldo sentteissä               |
| Version                | Pelitilin versio                             |

#### Transactions taulu

| Sarake          | Kuvaus                        |
|-----------------|-------------------------------|
| AccountId       | Pelaajan yksilöivä tunniste   |
| TransactionId   | Tapahtuman yksilöivä tunniste |
| TransactionType | Tapahtuman tyyppi             |
| Timestamp       | Tapahtuman aikaleima          |
| Amount          | Tapahtuman summa sentteissä   |


### Toteutuksen arkkitehtuuri

HTTP API on toteutettu AWS:n API Gateway:n avulla. API Gateway rajapinta kutsuu 3 eri lambda toteutusta. 
Saldon hakemiselle, veloitukselle ja voiton maksulle on toteutettu omat lambdat. Tietokantana toimii DynamoDB.
HTTP API:n kutsuissa vaaditaan tunnistautumista.

### Esivaatimukset
- java 21
- maven 3
- AWS CLI

### Asennus

Kloonaa repository omaan hakemistoon

    $ git clone https://github.com/awsdocs/aws-lambda-developer-guide.git
    $ cd wallet

Luo S3 bucket, jossa säilytetään riippuvuudet
    
    wallet$ ./1-create-bucket.sh

Käynnistä cloudformation scripti, joka luo wallet stackin AWS:lle
    
    wallet$ ./2-deploy.sh

Scripti luo DynamoDB tietokannan, lambdat, API Gateway rajapinnan ja uuden käyttäjän. 
Käyttäjän luontia varten kysytään käyttäjätunnusta ja salasanaa.

### Testaus

Testausta varten on tehty oma lamdba funktio, joka lisää DynamoDB tietokantaan tilitiedot
    
    wallet$ ./3-generate-test-account.sh

HTTP API voidaan testata luokan test.java.wallet.TestWalletTransactions avulla.
Ennen testiluokan ajamista pitää käydä AWS Consolilla luomassa uudelle käyttäjälle 
accesskey sekä hakemassa API Gateway:n URL. 
Tiedot päivitetään TestWalletTransactions luokan muuttujiin.

### Projektin poisto AWS:sta

Projektin poistamiseksi käynnistä 4-cleanup.sh skripti

    wallet$ ./4-cleanup.sh
