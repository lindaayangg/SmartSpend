package com.htn.budgetbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;

import com.htn.budgetbuddy.models.Suggestion;
import com.htn.budgetbuddy.models.Transaction;
import com.htn.budgetbuddy.utils.Constants;
import com.htn.budgetbuddy.utils.TinyDB;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SuggestionsActivityTransportation extends AppCompatActivity implements View.OnClickListener {

    private ImageButton backButton, shoppingButton, foodButton, entertainmentButton;
    private ListView suggestionList;
    private SuggestionAdapter suggestionAdapter;

    private TinyDB tinyDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggestions_transporation);

        tinyDB = new TinyDB(this);

        backButton = findViewById(R.id.suggestions_imageButton_leftArrow);
        shoppingButton = findViewById(R.id.suggestionsTransportation_button_shopping);
        foodButton =  findViewById(R.id.suggestionsTransportation_button_food);
        entertainmentButton = findViewById(R.id.suggestionsTransportation_button_entertainment);
        suggestionList = findViewById(R.id.suggestionsTransportation_list_suggestions);

        List<Suggestion> list = new ArrayList<>();

        suggestionAdapter = new SuggestionAdapter(this, list);

        suggestionList.setAdapter(suggestionAdapter);

        backButton.setOnClickListener(this);
        shoppingButton.setOnClickListener(this);
        foodButton.setOnClickListener(this);
        entertainmentButton.setOnClickListener(this);

        try {
            getSuggestions();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.suggestions_imageButton_leftArrow:
                openMainActivity();
                break;
            case R.id.suggestionsTransportation_button_shopping:
                openShoppingActivity();
                break;
            case R.id.suggestionsTransportation_button_food:
                openFoodActivity();
                break;
            case R.id.suggestionsTransportation_button_entertainment:
                openEntertainmentActivity();
                break;
        }
    }

    private void getSuggestions() throws IOException {

        ArrayList <Transaction> transactions = tinyDB.getListTransaction("transactions");
        int transactionsSize = transactions.size();

        final ArrayList <Suggestion> shopping = new ArrayList<>();
        final ArrayList <Suggestion> food = new ArrayList<>();
        final ArrayList <Suggestion> transportation = new ArrayList<>();
        final ArrayList <Suggestion> entertainment = new ArrayList<>();

        final OkHttpClient client = new OkHttpClient();

        for (int i = 0; i < transactionsSize; i++) {
            final String location = transactions.get(i).getLocationStreet();
            final String termOne = transactions.get(i).getMerchantName();
            final String category = transactions.get(i).getCategoryTags().get(0);

            if (location == null || location.isEmpty() || termOne == null || termOne.isEmpty()
                    || category == null || category.isEmpty()) {
                continue;
            }

            Request yelpOne = new Request.Builder()
                    .url(Constants.YELP_URL + "/businesses/search?" + "location=" + location + "&term=" + termOne)
                    .addHeader("Authorization", "Bearer " + Constants.YELP_KEY)
                    .build();

            client.newCall(yelpOne).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    call.cancel();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {

                    final String result = response.body().string();
                    SystemClock.sleep(500);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                JSONObject yelpOneJSON = new JSONObject(result);
                                if (yelpOneJSON.has("businesses")) {

                                    JSONArray businessArr = yelpOneJSON.getJSONArray("businesses");
                                    String termTwo = businessArr.getJSONObject(0).getJSONArray("categories").getJSONObject(0).getString("alias");
                                    final int price = yelpOneJSON.getJSONArray("businesses").getJSONObject(0).getString("price").length();
                                    final String oldURL = yelpOneJSON.getJSONArray("businesses").getJSONObject(0).getString("image_url");

                                    final Request yelpTwo = new Request.Builder()
                                            .url(Constants.YELP_URL + "/businesses/search?" + "location=" + location + "&term=" + termTwo)
                                            .addHeader("Authorization", "Bearer " + Constants.YELP_KEY)
                                            .build();

                                    client.newCall(yelpTwo).enqueue(new Callback() {
                                        @Override
                                        public void onFailure(Call call, IOException e) {
                                            call.cancel();
                                        }

                                        @Override
                                        public void onResponse(Call call, Response response) throws IOException {

                                            final String result = response.body().string();

                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    try {
                                                        JSONObject yelpTwoJSON = new JSONObject(result);
                                                        //find the correct suggestion (if any exist)
                                                        if (yelpTwoJSON.has("businesses")) {
                                                            JSONArray yelpTwoArray = yelpTwoJSON.getJSONArray("businesses");

                                                            int arrLen = yelpTwoArray.length();
                                                            for (int i = 0; i < arrLen; i++) {
                                                                if (!yelpTwoArray.getJSONObject(i).has("price")) {
                                                                    continue;
                                                                }

                                                                if (yelpTwoArray.getJSONObject(i).getString("price").length() <= price) {
                                                                    Suggestion recommend = new Suggestion();
                                                                    recommend.setCurrentName(termOne);
                                                                    recommend.setCurrentURL(oldURL);
                                                                    recommend.setSuggestedName(yelpTwoArray.getJSONObject(i).getString("name"));
                                                                    recommend.setSuggestedURL(yelpTwoArray.getJSONObject(i).getString("image_url"));

                                                                    /*System.out.println(recommend.toString());

                                                                    if (category == "Food and Dining") {
                                                                        food.add(recommend);
                                                                    } else if (category == "Home") {
                                                                        shopping.add(recommend);
                                                                    } else if (category == "Auto and Transport") {
                                                                        transportation.add(recommend);
                                                                    } else if (category == "Entertainment") {
                                                                        entertainment.add(recommend);
                                                                    }*/
                                                                    if (category.equalsIgnoreCase("Auto and Transport")) {
                                                                        suggestionAdapter.addItem(recommend);
                                                                    }
                                                                    return;
                                                                }
                                                            }

                                                            //add suggestion to the correct ArrayList
                                                        }
                                                    } catch (JSONException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            });
                                        }
                                    });
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            });
        }
    }

    private void openMainActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }

    private void openShoppingActivity() {
        Intent intent = new Intent(this, SuggestionsActivityShopping.class);
        startActivity(intent);
    }

    private void openFoodActivity() {
        Intent intent = new Intent(this, SuggestionsActivityFood.class);
        startActivity(intent);
    }

    private void openEntertainmentActivity() {
        Intent intent = new Intent(this, SuggestionsActivityEntertainment.class);
        startActivity(intent);
    }
}