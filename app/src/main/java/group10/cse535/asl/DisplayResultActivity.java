package group10.cse535.asl;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.content.Intent;


public class DisplayResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_result);
        String result_label = getIntent().getStringExtra("label");
        String time_taken = getIntent().getStringExtra("time_taken");

        TextView tv = (TextView)findViewById(R.id.predictionView);
        tv.setText(result_label);

        TextView timeView = (TextView)findViewById(R.id.timeTextView);
        timeView.setText(time_taken);

        Button okButton = (Button)findViewById(R.id.ok_button);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DisplayResultActivity.this, MainActivity.class);
                startActivity(intent);
            }

        });
    }
}
