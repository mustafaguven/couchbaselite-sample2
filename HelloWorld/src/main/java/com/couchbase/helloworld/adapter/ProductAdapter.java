package com.couchbase.helloworld.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.couchbase.helloworld.R;
import com.couchbase.helloworld.couchbase.AbstractCouchbaseLiteObject;
import com.couchbase.helloworld.model.Product;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  private List<AbstractCouchbaseLiteObject> data;

  public ProductAdapter(List<AbstractCouchbaseLiteObject> data) {
    this.data = data;
  }

  @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    final View view =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
    return new ProductViewHolder(view);
  }

  @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    ProductViewHolder viewHolder = (ProductViewHolder) holder;
    viewHolder.tvBarkod.setText(getItem(position).getBarkod());
    viewHolder.tvStok.setText(getItem(position).getStokkod());
    viewHolder.tvUrunAciklama.setText(getItem(position).getUrunAciklama());
  }

  private Product getItem(int position) {
    return (Product) data.get(position);
  }

  @Override public int getItemCount() {
    return data == null ? 0 : data.size();
  }

  public void setData(List<AbstractCouchbaseLiteObject> data) {
    this.data = data;
    notifyDataSetChanged();
  }

  private class ProductViewHolder extends RecyclerView.ViewHolder {

    TextView tvBarkod;
    TextView tvStok;
    TextView tvUrunAciklama;

    public ProductViewHolder(View itemView) {
      super(itemView);
      tvBarkod = (TextView) itemView.findViewById(R.id.tvBarkod);
      tvStok = (TextView) itemView.findViewById(R.id.tvStokkod);
      tvUrunAciklama = (TextView) itemView.findViewById(R.id.tvUrunAciklama);
    }
  }
}
