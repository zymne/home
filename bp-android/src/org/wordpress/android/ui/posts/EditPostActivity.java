package org.wordpress.android.ui.posts;

import org.wordpress.android.R;
import org.wordpress.android.util.ImageHelper;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;

public class EditPostActivity extends SherlockActivity implements OnClickListener  {
	
	private static final int ACTIVITY_REQUEST_CODE_PICTURE_LIBRARY = 0;
	
	private ImageButton mAddPictureButton;
	
	private int mCurrentActivityRequest = -1;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        
        mAddPictureButton = (ImageButton)findViewById(R.id.addPictureButton);
        
        
        mAddPictureButton.setOnClickListener(this);
        
        
        setContentView(R.layout.edit);
        
        
	}

	@Override
	public void onClick(View v) {

		int id = v.getId();
		
		switch(id)
		{
			case R.id.addPictureButton:				
				launchPictureLibrary();								
				break;			
		}
		
		
		
		
	}
	
	private void addMedia(String imgPath, Uri imgUri)
	{
		Bitmap resizedBitmap = null;
        ImageHelper ih = new ImageHelper();
        Display display = getWindowManager().getDefaultDisplay();
        
        
	}
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		
		switch(requestCode)
		{
			case ACTIVITY_REQUEST_CODE_PICTURE_LIBRARY:
				if(data != null) {
					Uri imageUri = data.getData();
	                String imgPath = imageUri.toString();
	                addMedia(imgPath, imageUri);	                
				}
				break;
		}
		
				
	}
	
    private void launchPictureLibrary() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        mCurrentActivityRequest = ACTIVITY_REQUEST_CODE_PICTURE_LIBRARY;
        startActivityForResult(photoPickerIntent, ACTIVITY_REQUEST_CODE_PICTURE_LIBRARY);
    }
}
