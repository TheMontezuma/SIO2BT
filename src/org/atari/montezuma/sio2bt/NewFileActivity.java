package org.atari.montezuma.sio2bt;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class NewFileActivity extends Activity implements OnClickListener
{
	public static final String FILE_NAME_EXTRA = "FILE_NAME_EXTRA";
	
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_file);
        EditText et = (EditText)findViewById(R.id.editFileName);
        et.setText(getIntent().getStringExtra(FILE_NAME_EXTRA));
        Button bt = (Button)findViewById(R.id.buttonFileNameOK);
        bt.setOnClickListener(this);
    }

	@Override
	public void onClick(View v)
	{
        EditText et = (EditText)findViewById(R.id.editFileName);
        String filename = et.getText().toString();
        Intent intent = new Intent();
		intent.putExtra(FILE_NAME_EXTRA,filename);
		setResult(Activity.RESULT_OK, intent);
		finish();
	}
}
