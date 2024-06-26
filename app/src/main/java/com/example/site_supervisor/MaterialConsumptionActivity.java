package com.example.site_supervisor;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MaterialConsumptionActivity extends Activity
{
    TextView tvDate;
    List<MaterialConsumptionPojo> material;
    SQLiteDatabase db = null;
    public String dbPath = "/data/data/com.example.site_supervisor/databases/";
    public static String dbName= "Site_Supervisor.db";
    String path = dbPath+dbName;
    ListView listMaterial;
    ImageView imgAdd, imgGallery, imgCamera, imgUploaded;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_material_consumption);

        listMaterial = findViewById(R.id.listMaterial);
        imgAdd = findViewById(R.id.imgAdd);
        tvDate = findViewById(R.id.tvDate);
        imgGallery = findViewById(R.id.imgGallery);
        imgCamera = findViewById(R.id.imgCamera);
        imgUploaded = findViewById(R.id.imgUploaded);

        material = new ArrayList<>();

        tvDate.setText(tvDate.getText().toString()+getIntent().getStringExtra("date"));

        try
        {
            db = SQLiteDatabase.openDatabase(path,null,SQLiteDatabase.OPEN_READWRITE);
        }
        catch (Exception e)
        {
            Toast.makeText(getApplicationContext(),"Error : "+e.getMessage(),Toast.LENGTH_LONG).show();
        }

        Cursor cur = db.rawQuery("select id,assembly_mark,name,qty from tbl_billofmaterialdetails where ProjectId = "+getIntent().getIntExtra("projectId",0)+" and date like '%"+getIntent().getStringExtra("date")+"%'",null);

        while (cur.moveToNext())
        {
            MaterialConsumptionPojo mcp = new MaterialConsumptionPojo();
            mcp.setId(cur.getInt(0));
            mcp.setAssemblyMark(cur.getString(1));
            mcp.setName(cur.getString(2));
            mcp.setQty(cur.getInt(3));
            material.add(mcp);
        }

        cur.close();
        MaterialConsumptionAdapter materialAdapter = new MaterialConsumptionAdapter(getApplicationContext(),R.layout.material_consumption_adapter,material);
        listMaterial.setAdapter(materialAdapter);

        db.close();

        imgAdd.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                showPopupMenu(v);
            }
        });

        imgGallery.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                startActivityForResult(gallery,111);
            }
        });

        imgCamera.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null)
                {
                    startActivityForResult(takePictureIntent, 101);
                }
            }
        });

        imgUploaded.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                showPopupImage(v);
            }
        });
    }

    private void showPopupImage(View view)
    {
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_image, null);
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true;

        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

        try
        {
            db = SQLiteDatabase.openDatabase(path,null,SQLiteDatabase.OPEN_READWRITE);
        }
        catch (Exception e)
        {
            Toast.makeText(getApplicationContext(),"Error : "+e.getMessage(),Toast.LENGTH_LONG).show();
        }

        List<byte[]> image = new ArrayList<>();

        Cursor cur = null;
        int expId=0;
        try
        {
            cur = db.rawQuery("select id,image from tbl_daily_image where ProjectID = "+getIntent().getIntExtra("projectId",0)+" and date = '"+getIntent().getStringExtra("date")+"'",null);
            while(cur.moveToNext())
            {
                expId = cur.getInt(0);
                image.add(cur.getBlob(1));
            }
        }
        catch (Exception e)
        {
            if(expId!=0)
            {
                cur = db.rawQuery("select id from tbl_daily_image where ProjectID = "+getIntent().getIntExtra("projectId",0)+" and date = '"+getIntent().getStringExtra("date")+"' and id >"+expId,null);
                cur.moveToFirst();
                expId = cur.getInt(0);
                db.execSQL("delete from tbl_daily_image where id = "+expId);
                Toast.makeText(getApplicationContext(),"Some image was too large display",Toast.LENGTH_SHORT).show();
            }
        }
        finally
        {

            cur.close();
            db.close();
        }

        ListView lvImage = popupView.findViewById(R.id.lvImage);

        SetImageAdapter sia = new SetImageAdapter(getApplicationContext(),R.layout.set_image_adapter,image);
        lvImage.setAdapter(sia);

        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
    }

    private void showPopupMenu(View view)
    {
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_add_material, null);

        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true;

        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

        EditText etAssemblyMark = popupView.findViewById(R.id.etAssemblyMark);
        EditText etQty = popupView.findViewById(R.id.etQty);
        Button btAdd = popupView.findViewById(R.id.btAdd);

        btAdd.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                try
                {
                    db = SQLiteDatabase.openDatabase(path,null,SQLiteDatabase.OPEN_READWRITE);
                }
                catch (Exception e)
                {
                    Toast.makeText(getApplicationContext(),"Error : "+e.getMessage(),Toast.LENGTH_LONG).show();
                }

                try
                {
                    Cursor cur = db.rawQuery("select Max(id) from tbl_billofmaterialdetails",null);
                    cur.moveToFirst();
                    int id = cur.getInt(0)+1;

                    cur = db.rawQuery("select name from tbl_billofmaterialdetails where lower(assembly_mark) = '"+etAssemblyMark.getText().toString().toLowerCase()+"'",null);
                    cur.moveToFirst();
                    MaterialConsumptionPojo mcp = new MaterialConsumptionPojo();
                    mcp.setId(id);
                    if(etAssemblyMark.getText().toString().equals(""))
                    {
                        throw new EmptyStringException();
                    }
                    mcp.setAssemblyMark(etAssemblyMark.getText().toString().toUpperCase());
                    mcp.setName(cur.getString(0));
                    mcp.setQty(Integer.parseInt(etQty.getText().toString()));

                    ContentValues values = new ContentValues();
                    values.put("id", id);
                    values.put("ProjectID", getIntent().getIntExtra("projectId",0));
                    values.put("assembly_mark",mcp.getAssemblyMark());
                    values.put("name",mcp.getName());
                    values.put("qty",mcp.getQty());
                    values.put("date",getIntent().getStringExtra("date"));

                    long newRowId = db.insert("tbl_billofmaterialdetails", null, values);

                    if (newRowId == -1)
                    {
                        Toast.makeText(getApplicationContext(), "Error inserting data", Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        Toast.makeText(getApplicationContext(), "Data inserted with row ID: " + newRowId, Toast.LENGTH_SHORT).show();
                    }

                    db.close();
                    material.add(mcp);

                    popupWindow.dismiss();
                }
                catch (NumberFormatException nfe)
                {
                    Toast.makeText(getApplicationContext(),"Qty should be Integer...!",Toast.LENGTH_SHORT).show();
                }
                catch (EmptyStringException ese)
                {
                    Toast.makeText(getApplicationContext(),ese.toString(),Toast.LENGTH_SHORT).show();
                }
                catch (Exception e)
                {
                    Toast.makeText(getApplicationContext(),"Assembly mark doesn't exist...!",Toast.LENGTH_SHORT).show();
                }
            }
        });
        popupWindow.showAtLocation(view, Gravity.NO_GRAVITY, 0, 0);
    }


    public void onActivityResult(int reqCode, int resCode, Intent data)
    {
        Bitmap imageBitmap = null;
        if(reqCode==101 && resCode==RESULT_OK)
        {
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");

        }
        else if(reqCode==111 && resCode==RESULT_OK)
        {
            Uri imageUri = data.getData();

            try
            {
                ContentResolver contentResolver = getContentResolver();
                InputStream inputStream = contentResolver.openInputStream(imageUri);
                 imageBitmap = BitmapFactory.decodeStream(inputStream);
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }
        }
        if(resCode==RESULT_OK)
        {
            try
            {
                db = SQLiteDatabase.openDatabase(path,null,SQLiteDatabase.OPEN_READWRITE);
            }
            catch (Exception e)
            {
                Toast.makeText(getApplicationContext(),"Error : "+e.getMessage(),Toast.LENGTH_LONG).show();
            }

            int id = 1;
            Cursor cur = db.rawQuery("select Max(id) from tbl_daily_image",null);
            if(cur.moveToFirst())
            {
                id = cur.getInt(0)+1;
            }
            cur.close();

            ContentValues values = new ContentValues();
            values.put("id", id);
            values.put("ProjectID", getIntent().getIntExtra("projectId",0));
            values.put("image",bitmapToByteArray(imageBitmap));
            values.put("date",getIntent().getStringExtra("date"));

            long newRowId = db.insert("tbl_daily_image", null, values);

            if (newRowId == -1)
            {
                Toast.makeText(getApplicationContext(), "Error inserting data", Toast.LENGTH_SHORT).show();
            }
            else
            {
                Toast.makeText(getApplicationContext(), "Data inserted with row ID: " + newRowId, Toast.LENGTH_SHORT).show();
            }

            db.close();
        }
    }

    private byte[] bitmapToByteArray(Bitmap bitmap)
    {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 50, byteArrayOutputStream);
        bitmap.compress(Bitmap.CompressFormat.PNG, 50, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }
}