package edu.udg.exit.heartrate.Views;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;
import edu.udg.exit.heartrate.Global;
import edu.udg.exit.heartrate.Model.ResponseBody;
import edu.udg.exit.heartrate.Model.Tokens;
import edu.udg.exit.heartrate.Model.User;
import edu.udg.exit.heartrate.R;
import edu.udg.exit.heartrate.Services.ApiService;
import edu.udg.exit.heartrate.TodoApp;
import edu.udg.exit.heartrate.Utils.UserPreferences;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;

/**
 * Activity started when user attempts to login or register.
 */
public class LaunchActivity extends Activity {

    ///////////////
    // Constants //
    ///////////////

    private static final int MAX_ATTEMPTS = 10;
    private static final int DELAY = 10; // Delay between login attempts

    ////////////////
    // Attributes //
    ////////////////

    private Handler handler; // Handler to try login again
    private int count; // Counter to try login

    ///////////////////////
    // Lifecycle Methods //
    ///////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);

        // Handler & counter
        handler = new Handler();
        count = 0;

        // Launch animation
        loadLaunchAnimation();

        // Check if user is logged in
        checkLogin();
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    /**
     * Load the launch animation into the screen.
     */
    private void loadLaunchAnimation() {
        ImageView launchAnimation = (ImageView) findViewById(R.id.launch_animation);
        Glide.with(this).load(R.drawable.loading_heart_rate).into(new GlideDrawableImageViewTarget(launchAnimation));
    }

    /**
     * Check if the user is logged in and tries to retrieve its information.
     * If no user session found (tokens) redirect to login activity.
     */
    private void checkLogin() {
        String accessToken = UserPreferences.getInstance().load(getApplicationContext(),UserPreferences.ACCESS_TOKEN);
        if(accessToken != null) getUser();
        else startLoginActivity();
    }

    /**
     * Gets the user from access token and redirect to main activity.
     * On access token expired request new access token.
     * On access token invalid redirect to login activity.
     */
    private void getUser() {
        ApiService apiService = ((TodoApp) getApplication()).getApiService();

        if(apiService == null){
            if(count == MAX_ATTEMPTS) startMainActivity();
            else{
                count = count + 1;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        getUser();
                    }
                },DELAY);
            }
            return;
        }

        apiService.getUserService().getUser().enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if(response.isSuccessful()){
                    User user = (User) response.body();
                    ((TodoApp)getApplication()).setUser(user);
                    startMainActivity();
                }else{
                    try {
                        ResponseBody errorBody = Global.gson.fromJson(response.errorBody().string(), ResponseBody.class);
                        switch (errorBody.getCode()){
                            case ResponseBody.EXPIRED_TOKEN:
                                refreshTokens();
                                break;
                            default:
                                Toast.makeText(LaunchActivity.this,errorBody.getMessage(),Toast.LENGTH_LONG).show();
                                UserPreferences.getInstance().removeAll(getApplicationContext());
                                startLoginActivity();
                                break;
                        }
                    } catch (IOException e) {
                        Toast.makeText(LaunchActivity.this,"Unknown login error.",Toast.LENGTH_LONG).show();
                        startMainActivity();
                    } catch (Exception e) {
                        Toast.makeText(LaunchActivity.this,"Fatal login error.",Toast.LENGTH_LONG).show();
                        startMainActivity();
                    }
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Failed to connect.", Toast.LENGTH_LONG).show();
                startMainActivity();
            }
        });
    }

    /**
     * Gets new access & refresh tokens using the refresh token.
     */
    private void refreshTokens() {
        String refreshToken = UserPreferences.getInstance().load(getApplicationContext(),UserPreferences.REFRESH_TOKEN);
        RequestBody body = RequestBody.create(MediaType.parse("text/plain"), refreshToken);
        ApiService apiService = ((TodoApp) getApplication()).getApiService();
        apiService.getAuthService().refresh(body).enqueue(new Callback<Tokens>() {
            @Override
            public void onResponse(Call<Tokens> call, Response<Tokens> response) {
                if(response.isSuccessful()){
                    Tokens tokens = (Tokens) response.body();
                    UserPreferences.getInstance().save(getApplicationContext(),UserPreferences.ACCESS_TOKEN,tokens.getAccessToken());
                    UserPreferences.getInstance().save(getApplicationContext(),UserPreferences.REFRESH_TOKEN,tokens.getRefreshToken());
                    getUser();
                }else{
                    try {
                        ResponseBody errorBody = Global.gson.fromJson(response.errorBody().string(), ResponseBody.class);
                        Toast.makeText(LaunchActivity.this,errorBody.getMessage(),Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        Toast.makeText(LaunchActivity.this,"Unknown error.",Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(LaunchActivity.this,"Fatal error.",Toast.LENGTH_LONG).show();
                    }
                    UserPreferences.getInstance().removeAll(getApplicationContext());
                    startLoginActivity();
                }
            }

            @Override
            public void onFailure(Call<Tokens> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Failed to connect.", Toast.LENGTH_LONG).show();
                startMainActivity();
            }
        });
    }

    /**
     * Starts main activity and finish this activity.
     */
    private void startMainActivity() {
        Intent main = new Intent(LaunchActivity.this,MainActivity.class);
        startActivity(main);
        this.finish();
    }

    /**
     * Starts login activity and finish this activity.
     */
    private void startLoginActivity() {
        Intent login = new Intent(LaunchActivity.this,LoginActivity.class);
        startActivity(login);
        this.finish();
    }


}
