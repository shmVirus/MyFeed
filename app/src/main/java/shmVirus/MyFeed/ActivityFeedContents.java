package shmVirus.MyFeed;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class ActivityFeedContents extends AppCompatActivity {
    RecyclerView feedContentsList;
    String feedURL, feedTitle;
    ArrayList<ModelItem> feedItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_contents);

        feedContentsList = findViewById(R.id.feedContentList);

        Bundle Extra = getIntent().getExtras();
        feedURL = Extra.getString("feed");
        feedTitle = Extra.getString("title");
        setTitle(feedTitle + ": Items");

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean connected = (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED || connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED);

        if (connected) {
            new GetFeedContents().execute();    // fetching feed contents
        } else {
            Toast.makeText(ActivityFeedContents.this, "No Internet! Internet is Required!\nTurn on Internet and Then Try!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_feed_contents, menu); // menus for FeedContents page
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemID = item.getItemId();  // getting id of clicked menu

        // taking appropriate actions for different menus
        if (itemID == R.id.mBack) {
            finish();   // closing this FeedContents activity, and this allows to go back to previous activity
        } else if (itemID == R.id.mDelete) {
            // TODO: implement deletion of a particular feed while viewing it's items
            Toast.makeText(ActivityFeedContents.this, "Coming Soon!", Toast.LENGTH_SHORT).show();
        } else if (itemID == R.id.mClose) {
            finishAffinity();   // closing all activities, in short closing application
        }
        return true;
    }

    public InputStream getInputStream(URL url) {
        try {
            return url.openConnection().getInputStream();   // getting outputs from a URL
        } catch (IOException e) {
            return null;
        }
    }

    public static class ModelItem {     // custom RecyclerView model for each feed item
        String imageURL, title, description, author, date, link;

        public ModelItem(String imageURL, String title, String description, String author, String date, String link) {
            this.imageURL = imageURL;
            this.title = title;
            this.description = description;
            this.author = author;
            this.date = date;
            this.link = link;
        }
    }

    // AsyncTask performs heavy/networked tasks in the background and keeps UI thread free
    public class GetFeedContents extends AsyncTask<Integer, Void, Exception> {
        // alert message that can be invoked to appear on the screen to display the progress of an action that is loading
        ProgressDialog progressDialog = new ProgressDialog(ActivityFeedContents.this);
        Exception exception = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.setMessage("Loading Feed, Please Wait...");
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setCancelable(false);
            progressDialog.show();  // set an loading process dialog while fetching items
        }

        @Override
        protected Exception doInBackground(Integer... params) {
            try {
                URL url = new URL(feedURL);

                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                // specifies parser produced by factory will provide support for XML namespaces
                factory.setNamespaceAware(false);

                XmlPullParser pullParser = factory.newPullParser();
                pullParser.setInput(getInputStream(url), "UTF-8");  // contents of XML file

                boolean insideItem = false;     // to check if inside an item tag or not
                int tagType = pullParser.getEventType();    // pulling an XML tag

                // setting default values for each attributes of an item
                String imageURL = "N/A", title = "N/A", description = "N/A", author = "@: N/A", date = "N/A", link = "N/A";
                while (tagType != pullParser.END_DOCUMENT) {    // checking if reached end of the file
                    if (tagType == pullParser.START_TAG) {      // checking if the tag is staring tag or not
                        if (pullParser.getName().equalsIgnoreCase("item")) {    // checking if the starting tag is item tag or not
                            insideItem = true;  // inside an item tag
                        } else if (pullParser.getName().equalsIgnoreCase("title")) {
                            if (insideItem) {
                                title = pullParser.nextText();  // title/headline of the item
                            }
                        } else if (pullParser.getName().equalsIgnoreCase("description")) {
                            if (insideItem) {
                                description = pullParser.nextText();  // description of the item
                                if (description.contains("src")) {
                                    // featured image from item's description/content
                                    imageURL = description.substring(description.indexOf("src=") + 5, description.indexOf("/>") - 2);
                                }
                            }
                        } else if (pullParser.getName().equalsIgnoreCase("author")) {
                            if (insideItem) {
                                author = pullParser.nextText();  // author name of the item
                            }
                        } else if (pullParser.getName().equalsIgnoreCase("pubDate")) {
                            if (insideItem) {
                                date = pullParser.nextText();  // publication time of the item

                                String format = "dd-M-yyyy, hh:mm:ss a";    // custom date and time format
                                @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat(format);
                                TimeZone timeZone = TimeZone.getTimeZone(TimeZone.getDefault().getID());    // user's timezone
                                Calendar cal = new GregorianCalendar();
                                cal.setTime(new Date(date));
                                dateFormat.setTimeZone(timeZone);
                                cal.setTimeZone(timeZone);          // setting user's timezone to publication time

                                date = dateFormat.format(cal.getTime());    // converted time
                            }
                        } else if (pullParser.getName().equalsIgnoreCase("link")) {
                            if (insideItem) {
                                link = pullParser.nextText();  // link/url/uri of the item
                            }
                        }
                    } else if (tagType == pullParser.END_TAG && pullParser.getName().equalsIgnoreCase("item")) {
                        // adding attributes when reached end of item tag
                        feedItems.add(new ModelItem(imageURL, title, description, author, date, link));

                        // marking outside item tag and setting default values of attributes
                        insideItem = false;
                        imageURL = "N/A";
                        title = "N/A";
                        description = "N/A";
                        author = "@: N/A";
                        date = "N/A";
                        link = "N/A";
                    }
                    tagType = pullParser.next();    // traversing tags of the XML file
                }
            } catch (XmlPullParserException | IOException e) {
                exception = e;
            }
            return exception;
        }

        @Override
        protected void onPostExecute(Exception s) {
            super.onPostExecute(s);
            // setting feed items to the UI
            RecyclerAdapterFeedItems adapter = new RecyclerAdapterFeedItems(ActivityFeedContents.this, feedItems);
            feedContentsList.setLayoutManager(new LinearLayoutManager(ActivityFeedContents.this));
            feedContentsList.setAdapter(adapter);
            progressDialog.dismiss();   // dismissing processing dialog when fetched items
        }
    }

    public class RecyclerAdapterFeedItems extends RecyclerView.Adapter<RecyclerAdapterFeedItems.ViewAdapter> {
        Context context;
        ArrayList<ModelItem> items;

        RecyclerAdapterFeedItems(Context context, ArrayList<ModelItem> items) {
            this.context = context;
            this.items = items;
        }

        @NonNull
        @Override   // creating adapter for Recycler Card
        public ViewAdapter onCreateViewHolder(@NonNull ViewGroup viewGroup, int position) {
            View view = LayoutInflater.from(ActivityFeedContents.this).inflate(R.layout.recyclerview_card_feed_item, viewGroup, false);
            return new ViewAdapter(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewAdapter viewAdapter, @SuppressLint("RecyclerView") int position) {
            // setting attributes to the UI, in short placing item view to UI
            if (!feedItems.get(position).imageURL.contains("N/A")) {
                Picasso.get().load(feedItems.get(position).imageURL).into(viewAdapter.imageView);
            } else {
                viewAdapter.imageView.setImageResource(R.drawable.ic_no_image);
            }
            viewAdapter.tTitle.setText(feedItems.get(position).title);
            viewAdapter.tDescription.setText(Html.fromHtml(feedItems.get(position).description).toString());
            viewAdapter.tAuthor.setText(feedItems.get(position).author);
            viewAdapter.tDate.setText(feedItems.get(position).date);

            viewAdapter.itemView.setOnClickListener(new View.OnClickListener() {    // listening on click to items
                @Override
                public void onClick(View view) {    // launching WebView for clicked item, with item's URL
                    Intent intent = new Intent(ActivityFeedContents.this, ActivityWebView.class);
                    intent.putExtra("uri", feedItems.get(position).link);
                    intent.putExtra("title", feedItems.get(position).title);
                    startActivity(intent);
                }
            });
        }

        @Override   // number of feed items, needed for allocation of resources for RecyclerView
        public int getItemCount() {
            return feedItems.size();
        }

        public class ViewAdapter extends RecyclerView.ViewHolder {
            ImageView imageView;
            TextView tTitle, tDescription, tAuthor, tDate;

            public ViewAdapter(@NonNull View itemView) {
                super(itemView);
                // defining views for attributes of feed item
                imageView = itemView.findViewById(R.id.image);
                tTitle = itemView.findViewById(R.id.title);
                tDescription = itemView.findViewById(R.id.description);
                tAuthor = itemView.findViewById(R.id.author);
                tDate = itemView.findViewById(R.id.date);
            }
        }
    }
}