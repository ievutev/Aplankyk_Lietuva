package aplankyk.lietuva;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.ViewHolder> {

    private List<String> albums;
    private OnItemClickListener listener;

    public AlbumAdapter(List<String> albums) {
        this.albums = albums;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.album_item, parent, false);
        return new ViewHolder(view);
    }

    // This method handles the clicks
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") final int position) {
        String album = albums.get(position);
        holder.albumTitleTextView.setText(album);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onItemClick(position);
                }
            }
        });
    }

    // This method returns the count of albums
    @Override
    public int getItemCount() {
        return albums.size();
    }


    // This method names all the albums
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView albumTitleTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            albumTitleTextView = itemView.findViewById(R.id.albumTitleTextView);
        }
    }
}

