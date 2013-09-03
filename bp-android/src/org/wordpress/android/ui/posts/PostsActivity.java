package org.wordpress.android.ui.posts;

import org.wordpress.android.MainActivity;
import org.wordpress.android.R;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.posts.PostsListFragment.OnRefreshListener;
import org.xmlrpc.android.ApiHelper;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;


public class PostsActivity extends WPActionBarActivity implements OnRefreshListener {
	
	private PostsListFragment postList;
	private MenuItem refreshMenuItem;
	public boolean isRefreshing = false;
	
	private int ACTIVITY_EDIT_POST = 0;

	public void onCreate(Bundle savedInstanceState) {
        
		super.onCreate(savedInstanceState);
        		
		createMenuDrawer(R.layout.posts);		
				
		FragmentManager fm = getSupportFragmentManager();
        fm.addOnBackStackChangedListener(mOnBackStackChangedListener);
        postList = (PostsListFragment) fm.findFragmentById(R.id.postList);
        postList.setListShown(true);
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.posts , menu);
        refreshMenuItem = menu.findItem(R.id.menu_refresh);

        /*
        if (isPage) {
            menu.findItem(R.id.menu_new_post).setTitle(R.string.new_page);
        }*/

        if (shouldAnimateRefreshButton) {
            shouldAnimateRefreshButton = false;
            //startAnimatingRefreshButton(refreshMenuItem);
        }
        return true;
    }
	
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_refresh) {
            checkForLocalChanges(true);
            new ApiHelper.RefreshBlogContentTask(this, MainActivity.currentBlog).execute(false);
            return true;
        } else if (itemId == R.id.menu_new_post) {
            Intent i = new Intent(this, EditPostActivity.class);
            i.putExtra("id", MainActivity.currentBlog.getId());
            i.putExtra("isNew", true);
            
            /*
            if (isPage)
                i.putExtra("isPage", true);*/
            startActivityForResult(i, ACTIVITY_EDIT_POST);
            return true;
        } else if (itemId == android.R.id.home) {
            FragmentManager fm = getSupportFragmentManager();
            if (fm.getBackStackEntryCount() > 0) {
                popPostDetail();
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }
	
	@Override
	protected void onResume()
	{
		super.onResume();
		
		if(postList.getListView().getCount() == 0)
			postList.loadPosts(false);		
	}
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if(data !=null)
		{
			
		}		
		
		super.onActivityResult(requestCode, resultCode, data);
	}
	
    private FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener = new FragmentManager.OnBackStackChangedListener() {
        public void onBackStackChanged() {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0)
                mMenuDrawer.setDrawerIndicatorEnabled(true);
        }
    };

	@Override
	public void onRefresh(boolean start) {
        if (start) {
            attemptToSelectPost();
            shouldAnimateRefreshButton = true;
            //startAnimatingRefreshButton(refreshMenuItem);
            isRefreshing = true;
        } else {
            //stopAnimatingRefreshButton(refreshMenuItem);
            isRefreshing = false;
        }
		
	}
    
	protected void checkForLocalChanges(boolean shouldPrompt) {
        boolean hasLocalChanges = MainActivity.wpDB.findLocalChanges();
        if (hasLocalChanges) {
            if (!shouldPrompt)
                return;
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                    PostsActivity.this);
            dialogBuilder.setTitle(getResources().getText(
                    R.string.local_changes));
            dialogBuilder.setMessage(getResources().getText(R.string.remote_changes));
            dialogBuilder.setPositiveButton(getResources().getText(R.string.yes),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                            popPostDetail();
                            attemptToSelectPost();
                            postList.refreshPosts(false);
                        }
                    });
            dialogBuilder.setNegativeButton(getResources().getText(R.string.no),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                            //just close the window
                        }
                    });
            dialogBuilder.setCancelable(true);
            if (!isFinishing()) {
                dialogBuilder.create().show();
            }
        } else {
            popPostDetail();
            attemptToSelectPost();
            shouldAnimateRefreshButton = true;
            postList.refreshPosts(false);
        }
    }
	
    protected void popPostDetail() {
        /*
    	FragmentManager fm = getSupportFragmentManager();
        ViewPostFragment f = (ViewPostFragment) fm
                .findFragmentById(R.id.postDetail);
        if (f == null) {
            try {
                fm.popBackStack();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }*/
    }
	
    private void attemptToSelectPost() {
    	/*
        FragmentManager fm = getSupportFragmentManager();
        ViewPostFragment f = (ViewPostFragment) fm
                .findFragmentById(R.id.postDetail);

        if (f != null && f.isInLayout()) {
            postList.shouldSelectAfterLoad = true;
        }*/

    }
	
	
}
