package com.example.petra.healthylifeapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import java.util.concurrent.Callable;

public class Login extends AppCompatActivity {

    private LoginButton mFacebookSignInButton;
    private CallbackManager mFacebookCallbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        //facebook

        mFacebookCallbackManager = CallbackManager.Factory.create();
        mFacebookSignInButton = (LoginButton) findViewById(R.id.login_button);
        mFacebookSignInButton.registerCallback(mFacebookCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(final LoginResult loginResult) {
                        //TODO: Use the Profile class to get information about the current user.
                        handleSignInResult(new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                LoginManager.getInstance().logOut();
                                return null;
                            }
                        });
                    }

                    @Override
                    public void onCancel() {
                        handleSignInResult(null);
                    }

                    @Override
                    public void onError(FacebookException error) {
                        Log.d(MainActivity.class.getCanonicalName(), error.getMessage());
                        handleSignInResult(null);
                    }
                }

        );


    }

    private void handleSignInResult(Object o) {
//Handle sign result here
    }

    @Override

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        mFacebookCallbackManager.onActivityResult(requestCode, resultCode, data);

    }
}
