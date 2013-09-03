package org.wordpress.android.ui.posts;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.wordpress.android.MainActivity;
import org.wordpress.android.R;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.EscapeUtils;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class PostsListFragment extends ListFragment {
	
    private String[] mPostIDs, mTitles, mDateCreated, mDateCreatedFormatted,
    mDraftIDs, mDraftTitles, mDraftDateCreated, mStatuses, mDraftStatuses;
	
    private OnRefreshListener mOnRefreshListener;
    
    public String errorMsg = "";
	
    public boolean isPage = false, shouldSelectAfterLoad = false;
	
	public int numRecords = 20;
	
	PostListAdapter mPostListAdapter;
	
	public getRecentPostsTask getPostsTask;
	
	@Override
	public void onCreate(Bundle data)
	{
		super.onCreate(data);
		
		Bundle extras = getActivity().getIntent().getExtras();		
		if(extras != null)
			isPage = extras.getBoolean("viewPages");								
	}
	
	
	public boolean loadPosts(boolean loadMore)	
	{
		List<Map<String, Object>> loadedPosts;
		
		try {
			if(isPage)
			{
				loadedPosts = MainActivity.wpDB.loadUploadedPosts(MainActivity.currentBlog.getId(), true);
			}
			else
			{
				loadedPosts = MainActivity.wpDB.loadUploadedPosts(MainActivity.currentBlog.getId(), false);
			}
		}
		catch(Exception e)
		{
			return false;
		}
		
        if (loadedPosts != null) {
            numRecords = loadedPosts.size();
            mTitles = new String[loadedPosts.size()];
            mPostIDs = new String[loadedPosts.size()];
            mDateCreated = new String[loadedPosts.size()];
            mDateCreatedFormatted = new String[loadedPosts.size()];
            mStatuses = new String[loadedPosts.size()];
        } else {
            mTitles = new String[0];
            mPostIDs = new String[0];
            mDateCreated = new String[0];
            mDateCreatedFormatted = new String[0];
            mStatuses = new String[0];
            if (mPostListAdapter != null) {
                mPostListAdapter.notifyDataSetChanged();
            }
        }
        
        boolean drafts = false;
        
        if (loadedPosts != null) {
            Date d = new Date();
            for (int i = 0; i < loadedPosts.size(); i++) {
                Map<String, Object> contentHash = loadedPosts.get(i);
                mTitles[i] = EscapeUtils.unescapeHtml(contentHash.get("title")
                        .toString());

                mPostIDs[i] = contentHash.get("id").toString();
                mDateCreated[i] = contentHash.get("date_created_gmt").toString();

                if (contentHash.get("post_status") != null) {
                    String api_status = contentHash.get("post_status")
                            .toString();
                    if (api_status.equals("publish")) {
                        mStatuses[i] = getResources()
                                .getText(R.string.published).toString();
                    } else if (api_status.equals("draft")) {
                        mStatuses[i] = getResources().getText(R.string.draft)
                                .toString();
                    } else if (api_status.equals("pending")) {
                        mStatuses[i] = getResources().getText(
                                R.string.pending_review).toString();
                    } else if (api_status.equals("private")) {
                        mStatuses[i] = getResources().getText(
                                R.string.post_private).toString();
                    }

                    if ((Long) contentHash.get("date_created_gmt") > d
                            .getTime() && api_status.equals("publish")) {
                        mStatuses[i] = getResources()
                                .getText(R.string.scheduled).toString();
                    }
                }

                long localTime = (Long) contentHash.get("date_created_gmt");
                mDateCreatedFormatted[i] = getFormattedDate(localTime);
                                
            }
        }
        
        if (loadedPosts != null || drafts == true) {
            ListView listView = getListView();
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            listView.setBackgroundColor(getResources().getColor(R.color.list_row_bg));
            listView.setDivider(getResources().getDrawable(R.drawable.list_divider));
            listView.setDividerHeight(1);                        
            
            if (loadMore) {
                mPostListAdapter.notifyDataSetChanged();
            } else {
                mPostListAdapter = new PostListAdapter(getActivity().getBaseContext());
                listView.setAdapter(mPostListAdapter);
            }
            
            if (loadedPosts == null) {
                refreshPosts(false);
            }
            
            return true;
        }
        else {
        	
        	if (loadedPosts == null) {
                refreshPosts(false);
            }
        	
        	return false;
        }
		
	}
	
	public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            // check that the containing activity implements our callback
            //mOnPostSelectedListener = (OnPostSelectedListener) activity;
            mOnRefreshListener = (OnRefreshListener) activity;
            //mOnPostActionListener = (OnPostActionListener) activity;
        } catch (ClassCastException e) {
            activity.finish();
            throw new ClassCastException(activity.toString()
                    + " must implement Callback");
        }
    }
	
	
    public void refreshPosts(final boolean loadMore) {

        if (!loadMore) {
            mOnRefreshListener.onRefresh(true);
            numRecords = 20;
        }
        List<Object> apiArgs = new Vector<Object>();
        apiArgs.add(MainActivity.currentBlog);
        apiArgs.add(isPage);
        apiArgs.add(numRecords);
        apiArgs.add(loadMore);
        getPostsTask = new getRecentPostsTask();
        getPostsTask.execute(apiArgs);
    }
	
    private String getFormattedDate(long localTime) {
        int flags = 0;
        flags |= android.text.format.DateUtils.FORMAT_SHOW_DATE;
        flags |= android.text.format.DateUtils.FORMAT_ABBREV_MONTH;
        flags |= android.text.format.DateUtils.FORMAT_SHOW_YEAR;
        flags |= android.text.format.DateUtils.FORMAT_SHOW_TIME;
        String formattedDate = DateUtils
                .formatDateTime(getActivity().getApplicationContext(),
                        localTime, flags);
        return formattedDate;
    }
	
	class ViewWrapper {
        View base;
        TextView title = null;
        TextView date = null;
        TextView status = null;

        ViewWrapper(View base) {
            this.base = base;
        }

        TextView getTitle() {
            if (title == null) {
                title = (TextView) base.findViewById(R.id.title);
            }
            return (title);
        }

        TextView getDate() {
            if (date == null) {
                date = (TextView) base.findViewById(R.id.date);
            }
            return (date);
        }

        TextView getStatus() {
            if (status == null) {
                status = (TextView) base.findViewById(R.id.status);
            }
            return (status);
        }
    }
	
	private class PostListAdapter extends BaseAdapter
	{
		
		public PostListAdapter(Context context)
		{
			
		}
		
		@Override
		public int getCount() {

			return mPostIDs.length;
		}

		@Override
		public Object getItem(int position) {

			return position;
		}

		@Override
		public long getItemId(int position) {

			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
            View pv = convertView;
            ViewWrapper wrapper = null;
            if (pv == null) {
                LayoutInflater inflater = getActivity().getLayoutInflater();
                pv = inflater.inflate(R.layout.row_post_page, parent, false);
                wrapper = new ViewWrapper(pv);
                pv.setTag(wrapper);
                wrapper = new ViewWrapper(pv);
                pv.setTag(wrapper);
            } else {
                wrapper = (ViewWrapper) pv.getTag();
            }

            String date = mDateCreatedFormatted[position];
            String status_text = mStatuses[position];

            pv.setTag(R.id.row_post_id, mPostIDs[position]);
            pv.setId(Integer.valueOf(mPostIDs[position]));
            String titleText = mTitles[position];
            if (titleText == "")
                titleText = "(" + getResources().getText(R.string.untitled) + ")";
            wrapper.getTitle().setText(titleText);
            wrapper.getDate().setText(date);
            wrapper.getStatus().setText(status_text);

            return pv;
		}
				
	}
	
	public class getRecentPostsTask extends AsyncTask<List<?>, Void, Boolean>
	{
		boolean isPage, loadMore;
		
		protected void onPostExecute(Boolean result) {
            if (isCancelled() || !result) {
                mOnRefreshListener.onRefresh(false);
                if (getActivity() == null)
                    return;
                if (errorMsg != "" && !getActivity().isFinishing()) {
                    FragmentTransaction ft = getFragmentManager()
                            .beginTransaction();
                    Log.d(String.format(getResources().getString(R.string.error_refresh)), errorMsg);
                    
                    /*
                    WPAlertDialogFragment alert = WPAlertDialogFragment
                            .newInstance(String.format(getResources().getString(R.string.error_refresh), (isPage) ? getResources().getText(R.string.pages) : getResources().getText(R.string.posts)), errorMsg);
                    try {
                        alert.show(ft, "alert");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }*/
                    errorMsg = "";
                }
                return;
            }

            /*if (loadMore)
                switcher.showPrevious();*/
            mOnRefreshListener.onRefresh(false);
            if (isAdded())
                loadPosts(loadMore);
        }
		
		
		@Override
		protected Boolean doInBackground(List<?>... args) {
			
			boolean success = false;
			
            List<?> arguments = args[0];
            MainActivity.currentBlog = (Blog) arguments.get(0);
            isPage = (Boolean) arguments.get(1);
            int recordCount = (Integer) arguments.get(2);
            loadMore = (Boolean) arguments.get(3);
			
			
			XMLRPCClient client = new XMLRPCClient(MainActivity.currentBlog.getUrl(),
					MainActivity.currentBlog.getHttpuser(), MainActivity.currentBlog.getHttppassword());
			
			Object[] params = {MainActivity.currentBlog.getUrl(),
					MainActivity.currentBlog.getHttpuser(), 
					MainActivity.currentBlog.getHttppassword()};
			
			Object[] result = null;
			
			try {
				
				result = (Object[])client.call("metaWeblog.getRecentPosts", params);
				
				if(result != null)
				{
					if(result.length > 0)
					{
						success = true;
						
						Map<?, ?> contentHash = new HashMap<Object, Object>();
						List<Map<?, ?>> dbVector = new Vector<Map<?,?>>();
						
						if (!loadMore) {
                            MainActivity.wpDB.deleteUploadedPosts(
                                    MainActivity.currentBlog.getId(), isPage);
                        }
						
						for(int ctr=0; ctr < result.length; ++ctr)
						{
							contentHash = (Map<Object, Object>)result[ctr];
							dbVector.add(ctr, contentHash);
						}
						
						MainActivity.wpDB.savePosts(dbVector,
								MainActivity.currentBlog.getId(), isPage);
					}
				}
			}
			catch(XMLRPCException e)
			{
				errorMsg = e.getMessage();
                if (errorMsg == null)
                    errorMsg = getResources().getString(R.string.error_generic);
			}
			
			return success;
		}
					
	}
	
    public interface OnRefreshListener {
        public void onRefresh(boolean start);
    }
}
