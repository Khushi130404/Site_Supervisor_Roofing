package com.example.site_supervisor;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class SiteInventoryActivity extends Activity
{
    Spinner spProject;
    TextView tvCustomer, tvPONo;
    SQLiteDatabase db = null;
    public String dbPath = "/data/data/com.example.site_supervisor/databases/";
    public static String dbName= "Site_Supervisor.db";
    String path;
    SpinnerAdapter spinnerAdapter;
    Button btDC, btStock;
    DatePicker date;
    TextView tvDate;
    private DatePickerDialog datePickerDialog;
    private Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_site_inventory);

        spProject = findViewById(R.id.spProject);
        tvCustomer = findViewById(R.id.tvCustomer);
        tvPONo = findViewById(R.id.tvPONo);
        btDC = findViewById(R.id.btDC);
        btStock = findViewById(R.id.btStock);
        tvDate = findViewById(R.id.tvDate);

        path = dbPath+dbName;

        calendar = Calendar.getInstance();
        setDateToToday();

        try
        {
            db = SQLiteDatabase.openDatabase(path,null,SQLiteDatabase.OPEN_READONLY);
        }
        catch (Exception e)
        {
            Toast.makeText(getApplicationContext(), "Error : "+e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        Cursor cur = db.rawQuery("select ProjectName from tbl_ProjectSite",null);

        List<String> projectName = new ArrayList<>();

        while (cur.moveToNext())
        {
            projectName.add(cur.getString(0));
        }
        db.close();
        spinnerAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1,projectName);
        spProject.setAdapter(spinnerAdapter);

        tvDate.setOnClickListener(v -> showDatePickerDialog());

        spProject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                try
                {
                    db = SQLiteDatabase.openDatabase(path,null,SQLiteDatabase.OPEN_READONLY);
                }
                catch (Exception e)
                {
                    Toast.makeText(getApplicationContext(), "Error : "+e.getMessage(), Toast.LENGTH_SHORT).show();
                }


                Cursor cur = db.rawQuery("select CustomerName,CustomerPO from tbl_ProjectSite where ProjectName = '"+projectName.get(position)+"'",null);
                cur.moveToFirst();
                tvCustomer.setText(cur.getString(0));
                tvPONo.setText(cur.getString(1));

                db.close();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
                // Do nothing
            }
        });

        btDC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try
                {
                    db = SQLiteDatabase.openDatabase(path,null,SQLiteDatabase.OPEN_READONLY);
                }
                catch (Exception e)
                {
                    Toast.makeText(getApplicationContext(), "Error : "+e.getMessage(), Toast.LENGTH_SHORT).show();
                }

                Cursor cur = db.rawQuery("select ProjectID from tbl_ProjectSite where ProjectName = '"+spProject.getSelectedItem().toString()+"'",null);
                cur.moveToFirst();

                db.close();

                Intent i = new Intent(getApplicationContext(), DeliveryChallanActivity.class);
                i.putExtra("projectId",cur.getInt(0));
                i.putExtra("date",tvDate.getText().toString());
                startActivity(i);
            }
        });

        btStock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try
                {
                    db = SQLiteDatabase.openDatabase(path,null,SQLiteDatabase.OPEN_READONLY);
                }
                catch (Exception e)
                {
                    Toast.makeText(getApplicationContext(), "Error : "+e.getMessage(), Toast.LENGTH_SHORT).show();
                }

                Cursor cur = db.rawQuery("select ProjectID from tbl_ProjectSite where ProjectName = '"+spProject.getSelectedItem().toString()+"'",null);
                cur.moveToFirst();

                db.close();

                Intent i = new Intent(getApplicationContext(), StockMaterialActivity.class);
                i.putExtra("projectId",cur.getInt(0));
                i.putExtra("date",tvDate.getText().toString());
                startActivity(i);
            }
        });

    }

    private void setDateToToday()
    {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        tvDate.setText(sdf.format(calendar.getTime()));
    }

    private void updateDateInView()
    {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        tvDate.setText(sdf.format(calendar.getTime()));
    }

    private void showDatePickerDialog()
    {
        datePickerDialog = new DatePickerDialog(SiteInventoryActivity.this,
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private final DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener()
    {
        @Override
        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth)
        {
            date = datePickerDialog.getDatePicker();
            tvDate.setText(""+addZero(date.getDayOfMonth())+"-"+addZero((date.getMonth()+1))+"-"+date.getYear());
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        }
    };

    private String addZero(int date)
    {
        return ((date<10) ? "0" : "")+date;
    }
}