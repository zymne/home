package org.wordpress.android;

import java.net.SocketException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Post;
import org.wordpress.android.ui.accounts.NewAccountActivity;
import org.wordpress.android.ui.posts.PostsActivity;
import org.wordpress.android.util.BitmapLruCache;
import org.wordpress.android.util.WPRestClient;
 
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.Request.Method;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.wordpress.rest.Oauth;
import com.wordpress.rest.RestClient;
import com.wordpress.rest.RestRequest;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;
import android.view.Menu;

public class MainActivity extends Activity {

	public static final String WPCOM_USERNAME_PREFERENCE = "wp_pref_wpcom_username";
	public static final String WPCOM_PASSWORD_PREFERENCE = "wp_pref_wpcom_password";
	
	private static final String TAG = "MainActivity";
	
	/**
     * Request code used when no accounts exist, and user is prompted to add an
     * account.
     */
    static final int ADD_ACCOUNT_REQUEST = 100;
    /**
     * Request code for reloading menu after returning from  the PreferencesActivity.
     */
    static final int SETTINGS_REQUEST = 200;
    /**
     * Request code for re-authentication
     */
    static final int AUTHENTICATE_REQUEST = 300;
	
    
    public static OnPostUploadedListener onPostUploadedListener = null;
    
	private static final String ACCESS_TOKEN_PREFERENCE = "wp_pref_wpcom_access_token";
	public static String versionName;
	public static WordPressDB wpDB;
	
	public static boolean postsShouldRefresh;
	public static boolean shouldRestoreSelectedActivity;
	protected boolean mShouldFinish;
	public static WPRestClient restClient;
	public static RequestQueue requestQueue;
	public static ImageLoader imageLoader;
	
	public static Blog currentBlog;
	public static Post currentPost;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.new_account);
		
		versionName = getVersionName();
        
		wpDB = new WordPressDB(this);
        
        // Volley networking setup

        requestQueue = Volley.newRequestQueue(this);
		
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        // Use a small slice of available memory for the image cache
        int cacheSize = maxMemory / 32;
        
        imageLoader = new ImageLoader(requestQueue, new BitmapLruCache(cacheSize));
        
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);  
        if (settings.getInt("wp_pref_last_activity", -1) >= 0)
            shouldRestoreSelectedActivity = true;
        
        restClient = new WPRestClient(requestQueue, new OauthAuthenticator(), settings.getString(ACCESS_TOKEN_PREFERENCE, null));
        
        //RestRequest request = mRestClient.makeRequest(Method.GET, RestClient.getAbsoluteURL(path, params), null, listener, errorListener);
                
        Map<String, String> params = new HashMap<String, String>();
        
        params.put("ll", "40.7,-74");
        params.put("client_id", "3GXVUMWMWGYM4KQU1FOHO1FNE2RFOGOCA3Q3W3ZKZEVVYJRY");
        params.put("client_secret", "OTZHTPHAIZWHLMFFPBYKFU52VC3ULKMAC2KPGENMMIMRGKQU");
        
        restClient.get("https://api.foursquare.com/v2/venues/search", params, null, null, null);
        
        //registerForCloudMessaging(this);
	}

	
	
	@Override
	protected void onResume()
	{
		super.onResume();		
		Intent i = new Intent(MainActivity.this, PostsActivity.class);
		startActivity(i);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data )
	{
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	
	
	private String getVersionName(){
        PackageManager pm = getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(getPackageName(), 0);
            return pi.versionName;
        } catch (NameNotFoundException e) {
            return "";
        }
    }
	
    /**
     * Get the currently active blog.
     * <p>
     * If the current blog is not already set, try and determine the last active blog from the last
     * time the application was used. If we're not able to determine the last active blog, just
     * select the first one.
     */
    
	public static Blog getCurrentBlog() {
        if (currentBlog == null) {
            // attempt to restore the last active blog
            setCurrentBlogToLastActive();

            // fallback to just using the first blog
            List<Map<String, Object>> accounts = MainActivity.wpDB.getAccounts();
            if (currentBlog == null && accounts.size() > 0) {
                int id = Integer.valueOf(accounts.get(0).get("id").toString());
                setCurrentBlog(id);
                wpDB.updateLastBlogId(id);
            }
        }

        return currentBlog;
    }	
	
	 /**
     * Set the last active blog as the current blog.
     * 
     * @return the current blog
     */
    public static Blog setCurrentBlogToLastActive() {
        List<Map<String, Object>> accounts = MainActivity.wpDB.getAccounts();

        int lastBlogId = MainActivity.wpDB.getLastBlogId();
        if (lastBlogId != -1) {
            for (Map<String, Object> account : accounts) {
                int id = Integer.valueOf(account.get("id").toString());
                if (id == lastBlogId) {
                    setCurrentBlog(id);
                }
            }
        }

        return currentBlog;
    }
	
	/**
     * Set the blog with the specified id as the current blog.
     * 
     * @param id id of the blog to set as current
     * @return the current blog
     */
    public static Blog setCurrentBlog(int id) {
        try {
            currentBlog = new Blog(id);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return currentBlog;
    }
	
	/*
	public static void registerForCloudMessaging(Context ctx) {
        
        if (MainActivity.hasValidWPComCredentials(ctx)) {
            String token = null;
            try {
                // Register for Google Cloud Messaging
                GCMRegistrar.checkDevice(ctx);
                GCMRegistrar.checkManifest(ctx);
                token = GCMRegistrar.getRegistrationId(ctx);
                String gcmId = Config.GCM_ID;
                if (gcmId != null && token.equals("")) {
                    GCMRegistrar.register(ctx, gcmId);
                } else {
                    // Send the token to WP.com in case it was invalidated
                    new WPComXMLRPCApi().registerWPComToken(ctx, token);
                    Log.v("WORDPRESS", "Already registered for GCM");
                }
            } catch (Exception e) {
                Log.v("WORDPRESS", "Could not register for GCM: " + e.getMessage());
            }
        }
    }*/
	
    public static void postUploaded() {
        if (onPostUploadedListener != null) {
            try {
                onPostUploadedListener.OnPostUploaded();
            } catch (Exception e) {
                postsShouldRefresh = true;
            }
        } else {
            postsShouldRefresh = true;
        }
    }
    
    public static boolean hasValidWPComCredentials(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String username = settings.getString(WPCOM_USERNAME_PREFERENCE, null);
        String password = settings.getString(WPCOM_PASSWORD_PREFERENCE, null);
        
        if (username != null && password != null)
            return true;
        else 
            return false;
    }
	
	class OauthAuthenticator implements WPRestClient.Authenticator {
        
		private final RequestQueue mQueue = Volley.newRequestQueue(MainActivity.this);
        @Override
        
        public void authenticate(WPRestClient.Request request){
            // set the access token if we have one
            if (!hasValidWPComCredentials(MainActivity.this)) {
                request.abort(new VolleyError("Missing WordPress.com Account"));
            } else {
                requestAccessToken(request);
            }
        }
        
        public void requestAccessToken(final WPRestClient.Request request){
            final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
            String username = settings.getString(WPCOM_USERNAME_PREFERENCE, null);
            String password = WordPressDB.decryptPassword(settings.getString(WPCOM_PASSWORD_PREFERENCE, null));
            Oauth oauth = new Oauth(Config.OAUTH_APP_ID, Config.OAUTH_APP_SECRET, Config.OAUTH_REDIRECT_URI);
            // make oauth volley request
            Request oauthRequest = oauth.makeRequest(username, password,
                new Oauth.Listener(){
                    @Override
                    public void onResponse(Oauth.Token token){
                        settings.edit().putString(ACCESS_TOKEN_PREFERENCE, token.toString())
                            .commit();
                        request.setAccessToken(token);
                        request.send();
                    }
                },
                new Oauth.ErrorListener(){
                    @Override
                    public void onErrorResponse(VolleyError error){
                        request.abort(error);
                    }
                }
            );
            mQueue.add(oauthRequest);
            // add oauth request to the request queue
            
        }
    }
	
    public interface OnPostUploadedListener {
        public abstract void OnPostUploaded();
    }

}
