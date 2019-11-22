
import javafx.application.Application;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Pair;
import netscape.javascript.JSObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Main extends Application {
    // for communication to the Javascript engine.
    private JSObject javascriptConnector;
    private Invoice invoice;
    private User user;
    private int preferredWidth;
    private int preferredHeight;

    // for communication from the Javascript engine. //
    private JavaConnector javaConnector = new JavaConnector();

    private static ArrayList<User> usersList = new ArrayList<User>();

    public static void main(String[] args) throws IOException {
        List<String> info = getUserDataFile();
        Iterator iterator = info.iterator();
        for (int count = 0; count < info.size(); count++){
            String[] userInfo = (info.get(count)).split(" ");
            usersList.add(new User(userInfo[0], userInfo[1]));
        }
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Mom and Pop's Clothing Store");
        StackPane pane = new StackPane ();
        pane.setPadding(new Insets(15, 5, 5, 5));

        final WebView browser = new WebView();
        // final WebEngine webEngine = browser.getEngine();
        WebEngine webEngine = browser.getEngine();

        URL url = null;
        try {
            //url = new URL("https://dpsoccerdude101.github.io/dpsoccerdude101.github.io/Lab07%20(Attempt%20%232)/Lab07.html");
            url = new URL("https://dpsoccerdude101.github.io/dpsoccerdude101.github.io/Lab07%20(Attempt%20%232)/LoginView.html");
            //System.out.print(url.toExternalForm());
            webEngine.load(url.toExternalForm());
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            webEngine.loadContent("<html><head></head><body><h1>There's a bloody error.</h1></body></html>");
        }
        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (Worker.State.SUCCEEDED == newValue) {
                // set an interface object named 'javaConnector' in the web engine's page
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaConnector", javaConnector);

                // get the Javascript connector object.
                javascriptConnector = (JSObject) webEngine.executeScript("getJsConnector()");
            }
        });

        preferredWidth = 310;
        preferredHeight = 280;
        pane.getChildren().add(browser);

        Scene scene = new Scene(pane, preferredWidth, preferredHeight);
        //600, 650
        primaryStage.setScene(scene);

        primaryStage.show();

        webEngine.getLoadWorker().stateProperty().addListener((ov, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                Document doc = webEngine.getDocument();
                Element title = (Element) doc.getElementById("title");
                EventListener listener = new EventListener() {
                    @Override
                    public void handleEvent(org.w3c.dom.events.Event ev) {
                        if ((title.getTextContent().equals("Login"))) {
                            primaryStage.setWidth(310);
                            primaryStage.setHeight(280);
                        }
                        if ((title.getTextContent().equals("Lab07"))) {
                            primaryStage.setWidth(600);
                            primaryStage.setHeight(650);
                        }
                    }
                };

                Element button = (Element) doc.getElementById("back");
                //textcontet = login
                System.out.println(title.getTextContent());
                ((EventTarget) button).addEventListener("onclick", listener, false);
            }
        });
        // set up the listener

    }
    private static ArrayList<String> getUserDataFile () throws IOException {
        URL users = new URL("https://raw.githubusercontent.com/dpsoccerdude101/dpsoccerdude101.github.io/master/Lab07%20(Attempt%20%232)/Users.txt");
        BufferedReader input = new BufferedReader(new InputStreamReader(users.openStream()));
        ArrayList<String> list = new ArrayList<>();
        String inputLine;
        while ((inputLine = input.readLine()) != null) {
            list.add(inputLine);
        }
        input.close();
        return list;
    }
    private static boolean contains (User thatUser) {
        for (int count = 0; count < usersList.size(); count++) {
            User tempUser = usersList.get(count);
            if (tempUser.getUsername().equals(thatUser.getUsername())) {
                if (tempUser.getPassword().equals(thatUser.getPassword())) {
                    return true;
                }
            }
        }
        return false;
    }

    public class JavaConnector {

        private String value;
        /**
         * called when the JS side wants a String to be converted.
         *
         * @param value
         *         the String to convert
         */
        public void toJavaData(String value) {
            this.value = value;
            String[] tokens = value.split("&");
            Double[] tokensDouble = new Double[tokens.length];
            for (int count = 0; count < tokens.length; count++) {
                String[] miniTokens = tokens[count].split("=");
                tokensDouble[count] = Double.parseDouble(miniTokens[1]);
            }
            invoice = new Invoice(tokensDouble[0], tokensDouble[1], tokensDouble[2], tokensDouble[3], tokensDouble[4]);

            if (this.value != null) {
                javascriptConnector.call("showResult", ("$" + String.format("%.2f", invoice.getTotalBill())));
            }
        }
        public void toJavaLogin(String value) {
            this.value = value;
            String[] tokens = value.split("&");
            Pair<String, String> pair = new Pair<>((tokens[0].split("="))[1], (tokens[1].split("="))[1]);
            user = new User(pair.getKey(), pair.getValue());

            if (contains(user)) {
                preferredWidth = 600;
                preferredHeight = 650;
                javascriptConnector.call("goToQueryPage");
            }
            else
                javascriptConnector.call("loginFailed");
        }
        public void toJavaDimensions(Integer width, Integer height) {
            System.out.println("Width change");
            preferredWidth = width;
            preferredHeight = height;
        }
    }
}

