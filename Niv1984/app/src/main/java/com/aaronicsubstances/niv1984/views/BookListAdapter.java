package com.aaronicsubstances.niv1984.views;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.aaronicsubstances.niv1984.R;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * Created by Aaron on 6/21/2017.
 */

public class BookListAdapter extends RecyclerView.Adapter<BookListAdapter.ViewHolder> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BookListAdapter.class);

    private final String[] mBooks;
    // Store the context for easy access
    private final Context mContext;

    private RecyclerViewItemClickListener mItemClickListener;

    public BookListAdapter(Context context, String[] books) {
        this.mBooks = books;
        this.mContext = context;
    }

    public RecyclerViewItemClickListener getItemClickListener() {
        return mItemClickListener;
    }

    public void setItemClickListener(RecyclerViewItemClickListener itemClickListener) {
        this.mItemClickListener = itemClickListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View subjectView = inflater.inflate(R.layout.item_book, parent, false);

        // Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(subjectView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        // Get the data model based on position
        String bookEntry = mBooks[position];
        int sepIdx = bookEntry.indexOf("|");
        final String bookCode = bookEntry.substring(0, sepIdx);
        final String description = bookEntry.substring(sepIdx+1);

        // Set item views based on your views and data model
        holder.nameTextView.setText(description);

        char ch;
        int i = 0;
        do {
            ch = description.toLowerCase().charAt(i++);
        } while (i < description.length() && (ch < 'a' || ch > 'z'));
        /*try {
            Field f = R.drawable.class.getField("ic_subject_" +
                ch);
            int resId = (int) f.get(null);
            holder.iconView.setImageResource(resId);
        }
        catch (Exception ex) {
            LOGGER.error("Could not find icon for {} book.", description, ex);
        }*/
        int resId;
        if (ch == 'a') {
            resId = R.drawable.ic_subject_a; // used
        }
        else if (ch == 'b') {
            resId = R.drawable.ic_subject_b;
        }
        else if (ch == 'c') {
            resId = R.drawable.ic_subject_c; // used
        }
        else if (ch == 'd') {
            resId = R.drawable.ic_subject_d; // used
        }
        else if (ch == 'e') {
            resId = R.drawable.ic_subject_e; // used
        }
        /*else if (ch == 'f') {
            resId = R.drawable.ic_subject_f;
        }*/
        else if (ch == 'g') {
            resId = R.drawable.ic_subject_g; // used
        }
        else if (ch == 'h') {
            resId = R.drawable.ic_subject_h; // used
        }
        else if (ch == 'i') {
            resId = R.drawable.ic_subject_i; // used
        }
        else if (ch == 'j') {
            resId = R.drawable.ic_subject_j; // used
        }
        else if (ch == 'k') {
            resId = R.drawable.ic_subject_k; // used
        }
        else if (ch == 'l') {
            resId = R.drawable.ic_subject_l; // used
        }
        else if (ch == 'm') {
            resId = R.drawable.ic_subject_m; // used
        }
        else if (ch == 'n') {
            resId = R.drawable.ic_subject_n; // used
        }
        else if (ch == 'o') {
            resId = R.drawable.ic_subject_o; // used
        }
        else if (ch == 'p') {
            resId = R.drawable.ic_subject_p; // used
        }
        /*else if (ch == 'q') {
            resId = R.drawable.ic_subject_q;
        }*/
        else if (ch == 'r') {
            resId = R.drawable.ic_subject_r; // used
        }
        else if (ch == 's') {
            resId = R.drawable.ic_subject_s; // used
        }
        else if (ch == 't') {
            resId = R.drawable.ic_subject_t; // used
        }
        /*else if (ch == 'u') {
            resId = R.drawable.ic_subject_u;
        }
        else if (ch == 'v') {
            resId = R.drawable.ic_subject_v;
        }*/
        else if (ch == 'w') {
            resId = R.drawable.ic_subject_w;
        }
        /*else if (ch == 'x') {
            resId = R.drawable.ic_subject_x;
        }
        else if (ch == 'y') {
            resId = R.drawable.ic_subject_y;
        }*/
        else if (ch == 'z') {
            resId = R.drawable.ic_subject_z; // used
        }
        else {
            resId = 0;
        }
        if (resId > 0) {
            holder.iconView.setImageResource(resId);
        }
        else {
            LOGGER.error("Could not find icon for {} book.", description);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mItemClickListener != null) {
                    mItemClickListener.onItemClicked(holder.getAdapterPosition(),
                            bookCode);
                }
            }
        });
    }

    // Returns the total count of items in the list
    @Override
    public int getItemCount() {
        return mBooks.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView nameTextView;
        final ImageView iconView;

        public ViewHolder(View itemView) {
            super(itemView);

            nameTextView = (TextView) itemView.findViewById(R.id.text);
            iconView = (ImageView) itemView.findViewById(R.id.icon);
        }
    }
}
