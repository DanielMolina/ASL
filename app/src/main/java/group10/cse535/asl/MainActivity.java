package group10.cse535.asl;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Vector;

import Jama.Matrix;
import com.opencsv.CSVReader;

public class MainActivity extends AppCompatActivity {

    public static final int PICK_CSV = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, so request it
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    1);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                File directory;
                String storagePath;

                // if no SD Card, check if data folder is already on phone
                if (Environment.getExternalStorageState() == null) {
                    System.out.println("No SD Card Found");
                    storagePath = Environment.getDataDirectory()+"/about_father/";
                    directory = new File(storagePath);
                    if (directory.exists()) {
                        Uri uri = Uri.parse(storagePath);
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setDataAndType(uri, "*/*");
                        intent.putExtra("storage", storagePath);
                        setIntent(intent);
                        startActivityForResult(Intent.createChooser(intent, "Open"), PICK_CSV);
                    } else
                        dataAlert("about_father Data Folder Not Found");
                } else {
                    System.out.println("SD Card Found");
                    storagePath = Environment.getExternalStorageDirectory().getPath()+"/about_father/";
                    directory = new File(storagePath);
                    if (directory.exists()) {
                        Uri uri = Uri.parse(storagePath);
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setDataAndType(uri, "*/*");
                        intent.putExtra("storage", storagePath);
                        setIntent(intent);
                        startActivityForResult(Intent.createChooser(intent, "Open"), PICK_CSV);
                    } else
                        dataAlert("about_father Data Folder Not Found");
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /* method to create alert pop-up when data folder is not found*/
    protected void dataAlert(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(msg);
        builder.setCancelable(true);

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_CSV && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                System.out.println("Error Loading Data");
                dataAlert("Error Loading Data");
            } else {
                System.out.println("Data Selected");
                String csvDataPath = data.getData().getPath();
                String [] pathParts = csvDataPath.split("/");
                Bundle extras = this.getIntent().getExtras();
                if (extras != null) {
                    csvDataPath = extras.getString("storage") + pathParts[pathParts.length - 1];
                }
                System.out.println(csvDataPath);
                if(csvDataPath.endsWith(".csv") && new File(csvDataPath).exists()) {
                    test(csvDataPath);
                }
                else
                    dataAlert("Not .CSV File");
            }
        }
    }

    protected void test(String dataPath) {
        // load SVM parameters
        // first line is the bias term, following are the weights
        try {
            Matrix w;
            double b = 0.0;
            ArrayList<Double> weightList = new ArrayList<>();
            BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open("learnedparams.txt")));
            int wID = 0;
            for (String line; (line = br.readLine()) != null; ) {
                if(wID == 0) {
                    b = Double.valueOf(line);
                } else
                    weightList.add(wID-1, Double.valueOf(line));
                wID++;
            }

            // convert to double[] for easy conversion into Matrix
            double [] weights = weightList.stream().mapToDouble(Double::doubleValue).toArray();
            w = new Matrix(weights, weights.length);

            // extract features from csv
            Matrix features = getFeatures(dataPath);

            // get soft class label using w*x + b
            Matrix result = features.times(w);

            // add bias
            for(int i = 0; i < weightList.size(); i++) {
                result.set(i, 0, result.get(i,0) + b);
            }

            // average results for all time steps of a given datum
            double sum = 0;
            for(int i = 0; i < weightList.size(); i++) {
                sum += result.get(i, 0);
            }
            double ave = sum / weightList.size();

            // display result in new view
            Intent intent = new Intent(MainActivity.this, DisplayResultActivity.class);
            if (ave > 0) {
                // about has label == 1
                intent.putExtra("label", "positive");
                startActivity(intent);
            } else {
                // father has label == -1
                intent.putExtra("label", "negative");
                startActivity(intent);
            }
        } catch (Exception e) {
            System.out.println("Error reading csv");
            dataAlert("Error reading csv");
        }
    }

    protected Matrix getFeatures(String dataPath) {
        /* extract facial and arm features from data */
        int [] featuresToKeep = {3,4,6,7,9,10,12,13,15,16,18,19,21,22,24,25,27,28,30,31,33,34};
        Matrix features = new Matrix(0,0);

        try {
            if (new File(dataPath).exists()) {
                String [] lines;
                double [] ftrs = new double[0];
                Vector<double[]> x = new Vector<>();

                // parse csv
                CSVReader reader = new CSVReader(new FileReader(dataPath));
                String [] headers = reader.readNext();
                while ((lines = reader.readNext()) != null) {
                    // feature vector for current datum
                    ftrs = new double[lines.length];
                    // lines[] is an array of values from the line
                    for (int i = 0; i < lines.length; i++) {
                        ftrs[i] = Double.parseDouble(lines[i]);
                    }
                    x.add(ftrs);
                }

                // fill feature matrix
                features = new Matrix(x.size(), featuresToKeep.length);
                for (int i = 0; i < x.size(); i++) {
                    ftrs = x.get(i);
                    int adjustedIndex = 0;
                    for (int j = 0; j < ftrs.length; j++) {
                        // only add facial and arm features
                        for (int k = 0; k < featuresToKeep.length; k++) {
                            if (j == featuresToKeep[k]) {
                                features.set(i, adjustedIndex, ftrs[j]);
                                adjustedIndex++;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error reading csv");
            dataAlert("Error reading csv");
        }

        return features;
    }

    protected Matrix scale(Matrix X) {
        /* scale  data so each feature(column) has mean==0 and sd==1*/
        return new Matrix(0,0);
    }
}
