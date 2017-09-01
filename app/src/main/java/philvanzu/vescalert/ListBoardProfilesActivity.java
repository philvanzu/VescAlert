package philvanzu.vescalert;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class ListBoardProfilesActivity extends AppCompatActivity {
    public class ProfilesListAdapter extends ArrayAdapter<BoardProfile>
    {
        LayoutInflater mLayoutInflater;
        ArrayList<BoardProfile> mData;
        ProfilesListAdapter(@NonNull Context context, @LayoutRes int resource, @IdRes int textViewResourceId, @NonNull ArrayList<BoardProfile> list)
        {

            super(context, resource, textViewResourceId, list);
            mData = list;
            mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mLayoutInflater.inflate(R.layout.profileslist, null);
            }

            ImageView itemIcon = (ImageView)convertView.findViewById(R.id.profileicon);
            itemIcon.setOnClickListener(new android.view.View.OnClickListener(){
                @Override
                public void onClick(View view)
                {
                    ListBoardProfilesActivity.this.onItemClick(position);
                }
            });

            TextView itemTextView = (TextView) convertView.findViewById(R.id.Itemname);
            itemTextView.setText(mData.get(position).name);
            itemTextView.setOnClickListener(new android.view.View.OnClickListener(){
                @Override
                public void onClick(View view)
                {
                    ListBoardProfilesActivity.this.onItemClick(position);
                }
            });

            ImageButton buttonDelete = (ImageButton) convertView.findViewById(R.id.deleteProfileButton);
            buttonDelete.setOnClickListener(new android.view.View.OnClickListener(){
                @Override
                public void onClick( View view) {
                    ListBoardProfilesActivity.this.onItemDeleteClick(position);
                }
            });
            return convertView;
        }
    }

    public static final int REQ_CREATEITEM = 0;
    public static final int REQ_EDITITEM = 1;

    public ArrayList<BoardProfile> profiles = new ArrayList<BoardProfile>();
    public HashMap<String, Integer> profilesMap = new HashMap<String, Integer>();

    ListView profilesListView;
    ListBoardProfilesActivity.ProfilesListAdapter profilesListViewAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_board_profiles);
        profilesListView = (ListView) findViewById(R.id.profilesListView);

    }

    void onProfilesModified()
    {
        profilesMap.clear();
        for(int i = 0; i < profiles.size(); i++) profilesMap.put(profiles.get(i).guid, i);

        profilesListViewAdapter = new ListBoardProfilesActivity.ProfilesListAdapter(this, R.layout.profileslist, R.id.Itemname, profiles);
        profilesListView.setAdapter(profilesListViewAdapter);

    }


    public void LoadProfilesFromStorage()
    {
        try {
            FileInputStream fis = this.openFileInput("profiles.data");
            ObjectInputStream is = new ObjectInputStream(fis);
            profiles = (ArrayList<BoardProfile>) is.readObject();
            onProfilesModified();
            is.close();
            fis.close();
        }
        catch (IOException e){e.printStackTrace();}
        catch (ClassNotFoundException e) { e.printStackTrace(); }
    }

    private void SaveProfilesToStorage() {
        try {
            FileOutputStream fos = this.openFileOutput("profiles.data", Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject((Serializable) profiles);
            os.close();
            fos.close();

        }
        catch (IOException e){e.printStackTrace();}
        setResult(Activity.RESULT_OK, new Intent());
    }


    public void onItemClick(int position)
    {
        BoardProfile edit = profiles.get(position);
        Intent myIntent = new Intent(this, CreateBoardProfileActivity.class);
        myIntent.putExtra("BoardProfile", (Parcelable)edit);
        startActivityForResult(myIntent, REQ_EDITITEM);
    }

    public void onItemDeleteClick(int position)
    {
        profiles.remove(position);
        onProfilesModified();
        SaveProfilesToStorage();
    }

    public void BackButtonPressed(View v)
    {

        finish();
    }

    public void CreateButtonPressed(View v)
    {
        Intent myIntent = new Intent(this, CreateBoardProfileActivity.class);
        startActivityForResult(myIntent, REQ_CREATEITEM);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case (REQ_CREATEITEM) : {
                if (resultCode == Activity.RESULT_OK) {
                    BoardProfile freshProfile = data.getParcelableExtra("BoardProfile");
                    if(freshProfile != null)
                    {
                        profiles.add(freshProfile);
                        onProfilesModified();
                        SaveProfilesToStorage();
                    }
                }
                break;
            }
            case(REQ_EDITITEM):{
                if (resultCode == Activity.RESULT_OK) {
                    BoardProfile freshProfile = data.getParcelableExtra("BoardProfile");
                    if(freshProfile != null) {
                        Integer pos = profilesMap.get(freshProfile.guid);
                        if (pos != null) profiles.set(pos, freshProfile );
                        else profiles.add(freshProfile);

                        onProfilesModified();
                        SaveProfilesToStorage();
                    }
                }
                break;
            }
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        LoadProfilesFromStorage();
    }


}
