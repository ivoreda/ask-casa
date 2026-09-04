package com.casava.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "casava.ai")
public class AiProperties {

  private int topK = 5;
  private double similarityThreshold = 0.55;
  private int maxContextChars = 6000;
  private int maxHistoryMessages = 20;
  private String frontendOrigin = "http://localhost:5173";
  private String vectorStorePath = "./data/vector-store.json";

  public int getTopK() {
    return topK;
  }

  public void setTopK(int topK) {
    this.topK = topK;
  }

  public double getSimilarityThreshold() {
    return similarityThreshold;
  }

  public void setSimilarityThreshold(double similarityThreshold) {
    this.similarityThreshold = similarityThreshold;
  }

  public int getMaxContextChars() {
    return maxContextChars;
  }

  public void setMaxContextChars(int maxContextChars) {
    this.maxContextChars = maxContextChars;
  }

  public int getMaxHistoryMessages() {
    return maxHistoryMessages;
  }

  public void setMaxHistoryMessages(int maxHistoryMessages) {
    this.maxHistoryMessages = maxHistoryMessages;
  }

  public String getFrontendOrigin() {
    return frontendOrigin;
  }

  public void setFrontendOrigin(String frontendOrigin) {
    this.frontendOrigin = frontendOrigin;
  }

  public String getVectorStorePath() {
    return vectorStorePath;
  }

  public void setVectorStorePath(String vectorStorePath) {
    this.vectorStorePath = vectorStorePath;
  }
}
