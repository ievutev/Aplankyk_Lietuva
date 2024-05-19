package aplankyk.lietuva;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {

    private List<Place> searchResults;
    private Context context;
    private OnDirectionClickListener directionClickListener;
    private OnAboutObjectClickListener aboutObjectClickListener;
    private OnAddToListClickListener addToListClickListener;

    private boolean hideButtonInLikedPlaces;

    public ListAdapter(Context context, List<Place> searchResults, boolean hideButtonInLikedPlaces) {
        this.context = context;
        this.searchResults = searchResults;
        this.hideButtonInLikedPlaces = hideButtonInLikedPlaces;
    }

    public interface OnDirectionClickListener {
        void onDirectionClick(String placeName);
    }

    public void setOnDirectionClickListener(OnDirectionClickListener listener) {
        this.directionClickListener = listener;
    }

    public interface OnAboutObjectClickListener {
        void onAboutObjectClick(String placeName);
    }

    public void setOnAboutObjectClickListener(OnAboutObjectClickListener listener) {
        this.aboutObjectClickListener = listener;
    }

    public interface OnAddToListClickListener {
        void onAddToListClick(Place place);
    }

    public void setOnAddToListClickListener(OnAddToListClickListener listener) {
        this.addToListClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.place_info, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Place searchResult = searchResults.get(position);
        holder.bind(searchResult.getTitle());

        if (hideButtonInLikedPlaces) {
            holder.addToListButton.setVisibility(View.GONE); // Hide the button
        } else {
            holder.addToListButton.setVisibility(View.VISIBLE); // Show the button
        }

        // Set click listener for the "Directions" button
        holder.directionsButton.setOnClickListener(v -> {
            if (directionClickListener != null) {
                directionClickListener.onDirectionClick(searchResult.getTitle());
            }
        });

        // Set click listener for the "About Object" button
        holder.aboutObjectButton.setOnClickListener(v -> {
            if (aboutObjectClickListener != null) {
                aboutObjectClickListener.onAboutObjectClick(searchResult.getTitle());
            }
        });

        // Set click listener for the "Add to List" button
        holder.addToListButton.setOnClickListener(v -> {
            if (addToListClickListener != null) {
                addToListClickListener.onAddToListClick(searchResult);
            }
        });
    }

    @Override
    public int getItemCount() {
        return searchResults.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView searchResultTextView;
        Button directionsButton;
        Button aboutObjectButton;
        Button addToListButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            searchResultTextView = itemView.findViewById(R.id.placeName);
            directionsButton = itemView.findViewById(R.id.directions);
            aboutObjectButton = itemView.findViewById(R.id.aboutObject);
            addToListButton = itemView.findViewById(R.id.addToList);
        }

        public void bind(String searchResult) {
            // Bind the search result to the TextView
            searchResultTextView.setText(searchResult);
        }
    }
}

