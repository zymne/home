package org.wordpress.android.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.simonvt.menudrawer.MenuDrawer;
import net.simonvt.menudrawer.Position;

import org.wordpress.android.MainActivity;
import org.wordpress.android.R;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.accounts.NewAccountActivity;
import org.wordpress.android.ui.posts.PostsActivity;
import org.wordpress.android.util.EscapeUtils;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;


public abstract class WPActionBarActivity extends SherlockFragmentActivity {
	
	private static final String TAG = "WPActionBarActivity";
	
	protected static final String LAST_ACTIVITY_PREFERENCE = "wp_pref_last_activity";
	
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
    
    /**
     * Used to restore active activity on app creation
     */
    protected static final int POSTS_ACTIVITY = 1;
    
    
    
    protected boolean mShouldFinish;    
    protected boolean mFirstLaunch = false;
    protected boolean shouldAnimateRefreshButton;
    
    private boolean mIsXLargeDevice;    
        
    protected List<MenuDrawerItem> mMenuItems = new ArrayList<MenuDrawerItem>();
    
    private static int[] blogIDs;
    
    protected MenuDrawer mMenuDrawer;    
    private ListView mListView;
    private MenuAdapter mAdapter;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
    	
    	super.onCreate(savedInstanceState);
    	
        if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == 4)
            mIsXLargeDevice = true;
    	
    	mMenuItems.add(new PostsMenuItem());
    	
    }
    
    @Override
    protected void onResume()
    {
    	super.onResume();
    	refreshUI();    	
    }
    
    private void refreshUI(){
        // the current blog may have changed while we were away
        setupCurrentBlog();
        /*
        if (mMenuDrawer != null) {
            updateMenuDrawer();
        }

        Blog currentBlog = MainActivity.getCurrentBlog();

        if (currentBlog != null && mListView != null && mListView.getHeaderViewsCount() > 0) {
            for (int i = 0; i < blogIDs.length; i++) {
                if (blogIDs[i] == currentBlog.getId()) {
                    mBlogSpinner.setSelection(i);
                }
            }
        }*/
    }
    

    
    /**
     * Setup the global state tracking which blog is currently active.
     * <p>
     * If the global state is not already set, try and determine the last active
     * blog from the last time the application was used. If we're not able to
     * determine the last active blog, just select the first one.
     * <p>
     * If no blogs are configured, display the "new account" activity to allow
     * the user to setup a blog.
     */
    public void setupCurrentBlog() {
        Blog currentBlog = MainActivity.getCurrentBlog();

        // No blogs are configured or user has signed out, so display new account activity
        if (currentBlog == null || getBlogNames().length == 0) {
            Log.d(TAG, "No accounts configured.  Sending user to set up an account");
            mShouldFinish = false;
            Intent i = new Intent(this, NewAccountActivity.class);
            startActivityForResult(i, ADD_ACCOUNT_REQUEST);
            return;
        }
    }
    
    /**
     * Get the names of all the blogs configured within the application. If a
     * blog does not have a specific name, the blog URL is returned.
     * 
     * @return array of blog names
     */
    private static String[] getBlogNames() {
        List<Map<String, Object>> accounts = MainActivity.wpDB.getAccounts();

        int blogCount = accounts.size();
        blogIDs = new int[blogCount];
        String[] blogNames = new String[blogCount];

        for (int i = 0; i < blogCount; i++) {
            Map<String, Object> account = accounts.get(i);
            String name;
            if (account.get("blogName") != null) {
                name = EscapeUtils.unescapeHtml(account.get("blogName").toString());
            } else {
                name = account.get("url").toString();
            }
            blogNames[i] = name;
            blogIDs[i] = Integer.valueOf(account.get("id").toString());
        }

        return blogNames;
    }
    
    protected void startActivityWithDelay(final Intent i) {

        if (mIsXLargeDevice && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Tablets in landscape don't need a delay because the menu drawer doesn't close
            startActivity(i);
        } else {
            
        	// When switching to LAST_ACTIVITY_PREFERENCE onCreate we don't need to delay
            
        	if (mFirstLaunch) {
                startActivity(i);
                return;
            }
            
            // Let the menu animation finish before starting a new activity
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActivity(i);
                }
            }, 400);
        }
    }
    
    private class PostsMenuItem extends MenuDrawerItem
    {    	
    	public PostsMenuItem() {
			super(POSTS_ACTIVITY, R.string.posts, R.drawable.dashboard_icon_posts);
		}
    	
        @Override
        public Boolean isSelected(){
            WPActionBarActivity activity = WPActionBarActivity.this;
            return (activity instanceof PostsActivity) /*&& !(activity instanceof PagesActivity)*/;
        }
    	
		@Override
		public void onSelectItem() {
			
			if (!(WPActionBarActivity.this instanceof PostsActivity)
                    /*|| (WPActionBarActivity.this instanceof PagesActivity)*/)
                mShouldFinish = true;
			
            Intent intent = new Intent(WPActionBarActivity.this, PostsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivityWithDelay(intent);
		}
    }
    
    /**
     * Create a menu drawer and attach it to the activity.
     * 
     * @param contentViewID {@link View} of the main content for the activity.
     */
    protected void createMenuDrawer(int contentViewID) {
        
    	try
    	{
    		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    	}
    	catch(Exception e)
    	{
    		Log.d("CreategMenuDrawer", e.getMessage());
    	}

        mMenuDrawer = attachMenuDrawer();
        mMenuDrawer.setContentView(contentViewID);
        
        initMenuDrawer();
    }

    /**
     * Create a menu drawer and attach it to the activity.
     * 
     * @param contentView {@link View} of the main content for the activity.
     */
    protected void createMenuDrawer(View contentView) {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        mMenuDrawer = attachMenuDrawer();
        mMenuDrawer.setContentView(contentView);

        initMenuDrawer();
    }
    
    /**
     * Attach a menu drawer to the Activity
     * Set to be a static drawer if on a landscape x-large device
     */
    private MenuDrawer attachMenuDrawer() {
        MenuDrawer menuDrawer = null;
        if (mIsXLargeDevice) {
            // on a x-large screen device
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                menuDrawer = MenuDrawer.attach(this, MenuDrawer.Type.STATIC, Position.LEFT);
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            } else {
                menuDrawer = MenuDrawer.attach(this, MenuDrawer.Type.OVERLAY);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                menuDrawer.setDrawerIndicatorEnabled(true);
            }
        } else {
            menuDrawer = MenuDrawer.attach(this, MenuDrawer.Type.OVERLAY);
            menuDrawer.setDrawerIndicatorEnabled(true);
        }
        int shadowSizeInPixels = getResources().getDimensionPixelSize(R.dimen.menu_shadow_width);
        menuDrawer.setDropShadowSize(shadowSizeInPixels);
        menuDrawer.setDropShadowColor(getResources().getColor(R.color.md__shadowColor));
        //menuDrawer.setSlideDrawable(R.drawable.ic_drawer);
        return menuDrawer;
    }
    
    /**
     * Create menu drawer ListView and listeners
     */
    private void initMenuDrawer() {
        mListView = new ListView(this);
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mListView.setDivider(null);
        mListView.setDividerHeight(0);
        mListView.setCacheColorHint(android.R.color.transparent);
        mAdapter = new MenuAdapter(this);
        
        /*String[] blogNames = getBlogNames();
        if (blogNames.length > 1) {
            addBlogSpinner(blogNames);
        }*/

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // account for header views
                int menuPosition = position - mListView.getHeaderViewsCount();
                // bail if the adjusted position is out of bounds for the adapter
                if (menuPosition < 0 || menuPosition >= mAdapter.getCount())
                    return;
                MenuDrawerItem item = mAdapter.getItem(menuPosition);
                // if the item has an id, remember it for launch
                if (item.hasItemId()){
                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(WPActionBarActivity.this);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putInt(LAST_ACTIVITY_PREFERENCE, item.getItemId());
                    editor.commit();
                }
                // only perform selection if the item isn't already selected
                if (!item.isSelected())
                    item.selectItem();
                // save the last activity preference
                // close the menu drawer
                mMenuDrawer.closeMenu();
                // if we have an intent, start the new activity
            }
        });
        
        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                    int totalItemCount) {
                mMenuDrawer.invalidate();
            }
        });
        
        mMenuDrawer.setMenuView(mListView);
        mListView.setAdapter(mAdapter);
        updateMenuDrawer();
    }
    
    /**
     * Update all of the items in the menu drawer based on the current active
     * blog.
     */
    protected void updateMenuDrawer() {
        mAdapter.clear();
        // iterate over the available menu items and only show the ones that should be visible
        Iterator<MenuDrawerItem> availableItems = mMenuItems.iterator();
        while(availableItems.hasNext()){
            MenuDrawerItem item = availableItems.next();
            if (item.isVisible()) {
                mAdapter.add(item);
            }
        }
        mAdapter.notifyDataSetChanged();

    }
    
    public static class MenuAdapter extends ArrayAdapter<MenuDrawerItem> {

        MenuAdapter(Context context) {
            super(context, R.layout.menu_drawer_row, R.id.menu_row_title, new ArrayList<MenuDrawerItem>());
        }

        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            MenuDrawerItem item = getItem(position);

            TextView titleTextView = (TextView) view.findViewById(R.id.menu_row_title);
            titleTextView.setText(item.getTitleRes());

            ImageView iconImageView = (ImageView) view.findViewById(R.id.menu_row_icon);
            iconImageView.setImageResource(item.getIconRes());
            // Hide the badge always
            view.findViewById(R.id.menu_row_badge).setVisibility(View.GONE);

            if (item.isSelected()) {
                // http://stackoverflow.com/questions/5890379/setbackgroundresource-discards-my-xml-layout-attributes
                int bottom = view.getPaddingBottom();
                int top = view.getPaddingTop();
                int right = view.getPaddingRight();
                int left = view.getPaddingLeft();
                //view.setBackgroundResource(R.drawable.menu_drawer_selected);
                view.setPadding(left, top, right, bottom);
            } else {
                //view.setBackgroundResource(R.drawable.md_list_selector);
            }
            // allow the menudrawer item to configure the view
            item.configureView(view);

            return view;
        }
    }
    

}
