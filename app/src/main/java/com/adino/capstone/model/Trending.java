package com.adino.capstone.model;

/**
 * Created by afadinsro on 3/23/18.
 */

public class Trending {
    private String title;
    private String details;
    private String imageURL;
    private String topic;
    private boolean status;

    /**
     * Default constructor
     * Required by Firebase
     */
    public Trending() {
    }

    public Trending(String title, String details, String imageURL, String topic, boolean status) {
        setTitle(title);
        setDetails(details);
        setImageURL(imageURL);
        setStatus(status);
        setTopic(topic);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}
