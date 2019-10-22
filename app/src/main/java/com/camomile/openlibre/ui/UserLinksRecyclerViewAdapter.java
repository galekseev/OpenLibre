package com.camomile.openlibre.ui;

import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.camomile.openlibre.R;
import com.camomile.openlibre.model.db.UserProfile;

public class UserLinksRecyclerViewAdapter extends RecyclerView.Adapter<UserLinksRecyclerViewAdapter.InvitesViewHolder> {

    private static final String LOG_ID = "OpenLibre::" + UserLinksRecyclerViewAdapter.class.getSimpleName();

    private final UserFragment fragment;
    private final UserProfile userProfile;

    UserLinksRecyclerViewAdapter(UserFragment fragment, UserProfile data){
        this.fragment = fragment;
        this.userProfile = data;
    }

    @NonNull
    @Override
    public InvitesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_user_invites_row, parent, false);
        return new InvitesViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull InvitesViewHolder viewHolder, int i) {
        int linkedCount = userProfile.getLinked().size();
        if (i < linkedCount) {
            viewHolder.tvUserMail.setText(userProfile.getLinked().get(i));
            viewHolder.btnAllow.setVisibility(View.GONE);
            viewHolder.btnDeny.setVisibility(View.GONE);
            viewHolder.btnUnlink.setVisibility(View.VISIBLE);
        }
        else{
            viewHolder.tvUserMail.setText(userProfile.getRequests().get(i - linkedCount));
            viewHolder.btnAllow.setVisibility(View.VISIBLE);
            viewHolder.btnDeny.setVisibility(View.VISIBLE);
            viewHolder.btnUnlink.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        if (userProfile == null)
            return 0;
        else
            return userProfile.getLinked().size() + userProfile.getRequests().size();
    }

    public class InvitesViewHolder
            extends RecyclerView.ViewHolder
    {

        private TextView tvUserMail;
        private AppCompatButton btnAllow;
        private AppCompatButton btnDeny;
        private AppCompatButton btnUnlink;

        public InvitesViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserMail = itemView.findViewById(R.id.tv_user_mail);
            btnAllow = itemView.findViewById(R.id.button_allow_link);
            btnDeny = itemView.findViewById(R.id.button_deny_link);
            btnUnlink = itemView.findViewById(R.id.button_unlink_link);
            btnAllow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    fragment.approveLink(tvUserMail.getText().toString());
                }
            });
            btnDeny.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    fragment.denyLink(tvUserMail.getText().toString());
                }
            });
            btnUnlink.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    fragment.detachFromMaster(tvUserMail.getText().toString());
                }
            });
        }
    }
}
