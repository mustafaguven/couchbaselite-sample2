package com.couchbase.helloworld;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.couchbase.helloworld.adapter.ProductAdapter;
import com.couchbase.helloworld.couchbase.AbstractCouchbaseLiteObject;
import com.couchbase.helloworld.model.Product;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.replicator.Replication;
import java.io.IOException;
import java.net.URL;
import java.util.List;

public class MainActivity extends Activity
    implements View.OnClickListener, AbstractCouchbaseLiteObject.OnCompletion {

  private static final String DB_NAME = "terminaldb40";
  ProgressDialog progressDialog;

  Button add, delete, update;
  EditText etWhereClause;
  RecyclerView rvProducts;
  Product product;
  private int completedChangeCount = 0;
  private static final String TAG = "mainactivity";

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    add = (Button) findViewById(R.id.add);
    add.setOnClickListener(this);
    delete = (Button) findViewById(R.id.delete);
    delete.setOnClickListener(this);
    update = (Button) findViewById(R.id.update);
    update.setOnClickListener(this);
    etWhereClause = (EditText) findViewById(R.id.etWhereClause);
    rvProducts = (RecyclerView) findViewById(R.id.rvProducts);

    progressDialog = new ProgressDialog(MainActivity.this);

    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
    rvProducts.setLayoutManager(linearLayoutManager);

    try {
      product = new Product.Builder().setDatabase(getManager().getDatabase(DB_NAME)).build();
      product.setOnCompletion(this);
      //product.getAllItems();

      pushToTerminal();
      pullFromTerminal();
    } catch (Exception exception) {
      Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
    }
  }

  @Nullable private Manager getManager() throws IOException {
    return new Manager(new AndroidContext(this), Manager.DEFAULT_OPTIONS);
  }

  public void pullFromTerminal() {
    try {
      URL url = new URL("http://10.100.1.236:4984/terminalonline/");
      Replication pull = getManager().getDatabase(DB_NAME).createPullReplication(url);
      pull.setContinuous(true);
      //Authenticator auth = new BasicAuthenticator(username, password);
      //push.setAuthenticator(auth);
      //pull.setAuthenticator(auth);

      pull.addChangeListener(new Replication.ChangeListener() {
        @Override public void changed(Replication.ChangeEvent event) {
          Replication replication = event.getSource();
          Log.d(TAG, "Replication : " + replication + " changed.");
          if (!replication.isRunning()) {
            String msg = String.format("Replicator %s not running", replication);
            Log.d(TAG, msg);
          } else {
            int processed = replication.getCompletedChangesCount();
            int total = replication.getChangesCount();
            String msg = String.format("Replicator processed %d / %d", processed, total);
            Log.d(TAG, msg);
          }

          completedChangeCount++;
          if (completedChangeCount % 1000 == 0) {
            Log.e("COMPLETEDCHANGECOUNT", String.valueOf(completedChangeCount));
          }

          if (event.getError() != null) {
            Toast.makeText(MainActivity.this, "Sync error" + event.getError(), Toast.LENGTH_SHORT)
                .show();
          }
        }
      });
      pull.start();
    } catch (CouchbaseLiteException | IOException e) {
      e.printStackTrace();
    }
  }

  public void pushToTerminal() {
    try {
      URL url = new URL("http://10.100.1.236:4984/terminalonline/");
      Replication push = getManager().getDatabase(DB_NAME).createPushReplication(url);
      push.setContinuous(true);
      //Authenticator auth = new BasicAuthenticator(username, password);
      //push.setAuthenticator(auth);
      //pull.setAuthenticator(auth);
      push.addChangeListener(new Replication.ChangeListener() {
        @Override public void changed(Replication.ChangeEvent event) {
          int a = 0;
        }
      });
      push.start();
    } catch (CouchbaseLiteException | IOException e) {
      e.printStackTrace();
    }
  }

  @Override public void onClick(View view) {
    try {
      progressDialog.show();
      switch (view.getId()) {
        case R.id.add:
          //product.fetchAllProducts();
          /*product.create(new Product.Builder().setBarkod(key)
              .setStokkod(value)
              .setUrunAciklama("yeni urun aciklama")
              .build());*/
          product.getRowCount();
          break;
        case R.id.delete:
          product.deleteAllProducts();
          break;
        case R.id.update:
          final String key = etWhereClause.getText().toString().split("==")[0];
          final String value = etWhereClause.getText().toString().split("==")[1];
          product.updateProduct(key, value);
          break;
        default:
          break;
      }
    } catch (CouchbaseLiteException e) {
      e.printStackTrace();
    }
  }

  @Override public void onDataReceived(final AbstractCouchbaseLiteObject.EventType eventType,
      final List<AbstractCouchbaseLiteObject> data) {

    try {
      if (eventType != AbstractCouchbaseLiteObject.EventType.QUERIED) {
        Log.d(TAG, eventType.name());
        product.getAllItems();
      } else {

        Log.e(TAG, String.valueOf(data.size()));
        final ProductAdapter adapter = new ProductAdapter(data);
        runOnUiThread(new Runnable() {
          @Override public void run() {
            rvProducts.setAdapter(adapter);
          }
        });
      }
    } catch (CouchbaseLiteException e) {
      e.printStackTrace();
    } finally {

      if (progressDialog != null) progressDialog.dismiss();
    }
  }

  @Override
  public void onTotalRowCount(AbstractCouchbaseLiteObject.EventType eventType, int count) {
    Log.e(TAG, String.valueOf(count));
    if (progressDialog != null) progressDialog.dismiss();
  }
}

