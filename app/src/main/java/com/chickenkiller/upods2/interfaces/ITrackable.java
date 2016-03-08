package com.chickenkiller.upods2.interfaces;

import com.chickenkiller.upods2.models.Track;

import java.util.ArrayList;

/**
 * Created by alonzilberman on 8/31/15.
 * Implement it if item has nested tracks/episodes
 */
public interface ITrackable {

    void setTracks(ArrayList<? extends Track> tracks);

    ArrayList<? extends Track>  getTracks();

    String getTracksFeed();

    Track getSelectedTrack();

    /**
     * Selects track which will be automatycly played when ITrackable will be passed to player
     */
    void selectTrack(Track track);
}
